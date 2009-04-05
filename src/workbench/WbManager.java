/*
 * WbManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbFocusManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ColumnOrderMgr;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.VariablePool;
import workbench.sql.macros.MacroManager;
import workbench.util.MacOSHelper;
import workbench.util.StringUtil;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.gui.lnf.LnFHelper;
import workbench.gui.profiles.ProfileKey;
import workbench.gui.tools.DataPumper;
import workbench.util.UpdateCheck;
import workbench.util.WbFile;
import workbench.util.WbThread;


/**
 * The main application "controller" for the SQL Workbench/J
 *
 * @author  support@sql-workbench.net
 */
public final class WbManager
	implements FontChangedListener, Runnable, Thread.UncaughtExceptionHandler
{
	private static WbManager wb;
	private final List<MainWindow> mainWindows = Collections.synchronizedList(new ArrayList<MainWindow>(5));
	private final List<ToolWindow> toolWindows = Collections.synchronizedList(new ArrayList<ToolWindow>(5));
	private boolean batchMode = false;
	private boolean writeSettings = true;
	private boolean overWriteGlobalSettingsFile = true;
	private boolean outOfMemoryOcurred = false;
	private WbThread shutdownHook = new WbThread(this, "ShutdownHook");
	private AppArguments cmdLine = new AppArguments();
	private boolean isWindowsClassic;
	public final boolean isJava15;

	private WbManager()
	{
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		Thread.setDefaultUncaughtExceptionHandler(this);

		String version = System.getProperty("java.version", System.getProperty("java.runtime.version"));
		isJava15 = version.startsWith("1.5");
	}

	public static WbManager getInstance()
	{
		return wb;
	}

	public void uncaughtException(Thread thread, Throwable error)
	{
		LogMgr.logError("WbManager.uncaughtException()", "Thread + " + thread.getName() + " caused an exception!", error);
	}

	public AppArguments getCommandLine()
	{
		return cmdLine;
	}

	public boolean getSettingsShouldBeSaved()
	{
		return this.writeSettings;
	}

	public void showDialog(String clazz)
	{
		JFrame parent = WbManager.getInstance().getCurrentWindow();
		if (parent == null)
		{
			LogMgr.logWarning("WbManager.showDialog()", "Could not find the current MainWindow!", null);
		}

		JDialog dialog = null;
		try
		{
			// Use reflection to load various dialogs in order to
			// avoid unnecessary class loading during startup
			Class cls = Class.forName(clazz);
			Class[] types = new Class[] { java.awt.Frame.class  };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { parent };
			dialog = (JDialog)cons.newInstance(args);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			if (dialog.isModal()) WbSwingUtilities.center(dialog, parent);
			dialog.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbManager.showDialog()", "Error when creating dialog " + clazz, ex);
		}
		finally
		{
			if (dialog != null && dialog.isModal())
			{
				dialog.dispose();
				dialog = null;
			}
		}
	}

	public void setOutOfMemoryOcurred()
	{
		this.outOfMemoryOcurred = true;
	}

	public boolean outOfMemoryOcurred()
	{
		return this.outOfMemoryOcurred;
	}

	public void showOutOfMemoryError()
	{
		outOfMemoryOcurred = true;
		showLowMemoryError();
	}

	public void showLowMemoryError()
	{
		WbSwingUtilities.showErrorMessageKey(getCurrentWindow(), "MsgLowMemoryError");
	}

	public JFrame getCurrentWindow()
	{
		if (this.mainWindows == null) return getCurrentToolWindow();

		if (this.mainWindows.size() == 1)
		{
			return this.mainWindows.get(0);
		}

		for (MainWindow w : mainWindows)
		{
			if (w != null && w.hasFocus()) return w;
		}

		return null;
	}

	protected JFrame getCurrentToolWindow()
	{
		if (this.toolWindows == null) return null;
		if (this.toolWindows.size() == 1)
		{
			ToolWindow w = toolWindows.get(0);
			if (w != null) return w.getWindow();
		}

		for (ToolWindow t : toolWindows)
		{
			if (t != null)
			{
				JFrame f = t.getWindow();
				if (f.hasFocus()) return f;
			}
		}

		return null;

	}
	public void registerToolWindow(ToolWindow aWindow)
	{
		synchronized (toolWindows)
		{
			this.toolWindows.add(aWindow);
		}
	}

	public void unregisterToolWindow(ToolWindow aWindow)
	{
		if (aWindow == null) return;
		synchronized (toolWindows)
		{
			this.toolWindows.remove(aWindow);

			if (this.toolWindows.size() == 0 && this.mainWindows.size() == 0)
			{
				this.exitWorkbench(aWindow.getWindow());
			}
		}
	}

	private void closeToolWindows()
	{
		synchronized (toolWindows)
		{
			for (ToolWindow w : toolWindows)
			{
				w.closeWindow();
			}
			this.toolWindows.clear();
		}
	}

	public void fontChanged(String aFontKey, Font newFont)
	{
		if (aFontKey.equals(Settings.PROPERTY_DATA_FONT))
		{
			UIManager.put("Table.font", newFont);
			UIManager.put("TableHeader.font", newFont);
		}
	}

	public boolean isWindowsClassic()
	{
		return isWindowsClassic;
	}

	/**
	 * Returns the location of the application's jar file
	 * @see #getJarPath()
	 */
	public File getJarFile()
	{
		URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		File f = null;
		try
		{
			// Sending the path through the URLDecoder is important
			// because otherwise a path with %20 will be created
			// if the directory contains spaces!
			String p = URLDecoder.decode(url.getFile(), "UTF-8");
			f = new File(p);
		}
		catch (Exception e)
		{
			// Fallback, should not happen
			String p = url.getFile().replace("%20", " ");
			f = new File(p);
		}
		return f;
	}

	/**
	 * Returns the directory in which the application is installed
	 * @see #getJarFile()
	 */
	public String getJarPath()
	{
		WbFile parent = new WbFile(getJarFile().getParentFile());
		return parent.getFullPath();
	}

	private void initUI()
	{
		LnFHelper helper = new LnFHelper();
		helper.initUI();
		this.isWindowsClassic = helper.isWindowsClassic();
		FocusManager.setCurrentManager(WbFocusManager.getInstance());
		Settings.getInstance().addFontChangedListener(this);
	}

	protected JDialog closeMessage;

	private boolean saveWindowSettings()
	{
		if (!this.writeSettings) return true;
		boolean settingsSaved = false;

		if (!this.checkProfiles(getCurrentWindow())) return false;

		boolean result = true;
		for (MainWindow win : mainWindows)
		{
			if (win == null) continue;
			if (!settingsSaved && win.hasFocus())
			{
				win.saveSettings();
				settingsSaved = true;
			}
			if (win.isBusy())
			{
				if (!this.checkAbort(win)) return false;
			}
			result = win.saveWorkspace(true);
			if (!result) return false;
		}

		if (!settingsSaved)
		{
			for (MainWindow win : mainWindows)
			{
				if (win != null)
				{
					win.saveSettings();
				}
			}
		}

		return true;
	}

	public boolean isBatchMode()
	{
		return this.batchMode;
	}

	public boolean canExit()
	{
		if (this.saveWindowSettings())
		{
			if (Settings.getInstance().wasExternallyModified())
			{
				String msg = ResourceMgr.getFormattedString("MsgSettingsChanged", Settings.getInstance().getConfigFile().getFullPath());
				int result = WbSwingUtilities.getYesNoCancel(getCurrentWindow(), msg);
				this.overWriteGlobalSettingsFile = (result == JOptionPane.OK_OPTION);
				return result != JOptionPane.CANCEL_OPTION;
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	public void exitWorkbench()
	{
		JFrame w = this.getCurrentWindow();
		this.exitWorkbench(w);
	}

	public boolean exitWorkbench(JFrame window)
	{
		// saveSettings() will also prompt if any modified
		// files should be changed
		if (!canExit())
		{
			LogMgr.logInfo("WbManaer.exitWorkbench()", "Exiting application was cancelled during saveWindowSettings()");
			return false;
		}

		if (window == null)
		{
			ConnectionMgr.getInstance().disconnectAll();
			this.doShutdown(0);
			return true;
		}

		// When disconnecting it can happen that the disconnect itself
		// takes some time. Because of this, a small window is displayed
		// that the disconnect takes place, and the actual disconnect is
		// carried out in a different thread to not block the AWT thread.

		// If it takes too long the user can still abort the JVM ...
		this.createCloseMessageWindow(window);
		if (this.closeMessage != null) this.closeMessage.setVisible(true);

		MacroManager.getInstance().save();
		Thread t = new WbThread("WbManager disconnect")
		{
			public void run()
			{
				disconnectWindows();
				ConnectionMgr.getInstance().disconnectAll();
				disconnected();
			}
		};
		t.setDaemon(false);
		t.start();
		return true;
	}

	private void createCloseMessageWindow(JFrame parent)
	{
		if (parent == null) return;
		this.closeMessage = new JDialog(parent, false);
		this.closeMessage.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.getBevelBorderRaised());
		p.setLayout(new BorderLayout());
		JLabel l = new JLabel(ResourceMgr.getString("MsgClosingConnections"));
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		l.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(l, BorderLayout.CENTER);

		JButton b = new JButton(ResourceMgr.getString("MsgAbortImmediately"));
		b.setToolTipText(ResourceMgr.getDescription("MsgAbortImmediately"));
		b.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					doShutdown(0);
				}
			});

		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
		p2.add(b);
		p.add(p2, BorderLayout.SOUTH);
		this.closeMessage.getContentPane().setLayout(new BorderLayout());
		this.closeMessage.getContentPane().add(p, BorderLayout.CENTER);
		this.closeMessage.setUndecorated(true);
		this.closeMessage.setSize(210,80);
		WbSwingUtilities.center(this.closeMessage, parent);
	}

	protected void disconnectWindows()
	{
		for (MainWindow w : mainWindows)
		{
			if (w == null) continue;
			w.abortAll();
			w.disconnect(false, true, false);
		}
	}

	/**
	 *	this gets called from exitWorkbench() when disconnecting everything
	 */
	protected void disconnected()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				if (closeMessage != null)
				{
					closeMessage.setVisible(false);
					closeMessage.dispose();
					closeMessage = null;
				}
				closeAllWindows();
			}
		});
		doShutdown(0);
	}

	protected void closeAllWindows()
	{
		for (MainWindow w : mainWindows)
		{
			if (w != null)
			{
				try { w.setVisible(false); } catch (Throwable th) {}
				try { w.dispose(); } catch (Throwable th) {}
			}
		}
		closeToolWindows();
	}

	protected void saveSettings()
	{
		if (this.writeSettings && !this.isBatchMode())
		{
			Settings s = Settings.getInstance();
			FilterDefinitionManager.getInstance().saveMRUList();
			try
			{
				ColumnOrderMgr.getInstance().saveSettings();
			}
			catch (Exception e)
			{
				LogMgr.logError("WbManager.saveSettings()", "Could not write column order storage", e);
			}
			if (s != null && overWriteGlobalSettingsFile) s.saveSettings(outOfMemoryOcurred);
		}
	}

	public void doShutdown(int errorCode)
	{
		Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		this.closeAllWindows();
		saveSettings();
		LogMgr.logInfo("WbManager.doShutdown()", "Stopping " + ResourceMgr.TXT_PRODUCT_NAME + ", Build " + ResourceMgr.getString("TxtBuildNumber"));
		LogMgr.shutdown();
		// The property workbench.system.doexit can be used to embedd the workbench.jar
		// in other applications and still be able to call doShutdown()
		boolean doExit = "true".equals(System.getProperty("workbench.system.doexit", "true"));
		if (doExit) System.exit(errorCode);
	}

	private boolean checkAbort(MainWindow win)
	{
		return WbSwingUtilities.getYesNo(win, ResourceMgr.getString("MsgAbortRunningSql"));
	}

	private boolean checkProfiles(JFrame win)
	{
		if (ConnectionMgr.getInstance().profilesAreModified())
		{
			int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedProfiles"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				ConnectionMgr.getInstance().saveProfiles();
				return true;
			}
			else
			{
				return answer == JOptionPane.NO_OPTION;
			}
		}
		return true;
	}

	/**
	 * Called whenever a MainWindow is closed.
	 *
	 * @see workbench.gui.MainWindow#windowClosing(java.awt.event.WindowEvent)
	 * @see workbench.gui.MainWindow#connectCancelled()
	 */
	public void windowClosing(final MainWindow win)
	{
		if (this.mainWindows.size() == 1)
		{
			// If only one window is present, shut down the application
			this.exitWorkbench(win);
		}
		else if (win != null)
		{
			if (!win.saveWorkspace()) return;
			this.mainWindows.remove(win);
			WbThread t = new WbThread("WindowDisconnect")
			{
				public void run()
				{
					// First parameter tells the window to disconnect in the
					// current thread as we are already in a background thread
					// second parameter tells the window not to close the workspace
					// third parameter tells the window not to save the workspace
					// this does not need to happen on the EDT
					win.disconnect(false, false, false);
					win.setVisible(false);
					win.dispose();
				}
			};
			t.start();
		}
	}

	/**
	 * Open a new main window, but do not check any command line parameters.
	 *
	 * This method will be called from the GUI
	 * when the user requests a new window
	 *
	 * @see workbench.gui.actions.FileNewWindowAction
	 */
	public void openNewWindow()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				openNewWindow(false);
			}
		});

	}

	protected void openNewWindow(boolean checkCmdLine)
	{
		final MainWindow main = new MainWindow();
		this.mainWindows.add(main);
		main.display();
		boolean connected = false;

		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(AppArguments.ARG_PROFILE);
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			ConnectionProfile prof  = null;
			if (!StringUtil.isEmptyString(profilename))
			{
				ProfileKey def = new ProfileKey(profilename, group);
				prof = ConnectionMgr.getInstance().getProfile(def);
			}
			else
			{
				prof = BatchRunner.createCmdLineProfile(this.cmdLine);
			}

			if (prof != null)
			{
				LogMgr.logDebug("WbManager.openNewWindow()", "Connecting to " + prof.getName());
				// try to connect to the profile passed on the
				// command line. If this fails the connection
				// dialog will be show to the user
				main.connectTo(prof, true);

				// the main window will take care of displaying the connection dialog
				// if the connection to the requested profile fails.
				connected = true;
			}
		}

		boolean autoSelect = Settings.getInstance().getShowConnectDialogOnStartup();
		final boolean exitOnCancel = Settings.getInstance().getExitOnFirstConnectCancel();

		// no connection? then display the connection dialog
		if (!connected && autoSelect)
		{
			// Should be done later, so that the main window
			// has enough time to initialize
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					main.selectConnection(exitOnCancel);
				}
			});
		}
	}

	private void readParameters(String[] args)
	{
		try
		{
			cmdLine.parse(args);

			String lang = cmdLine.getValue(AppArguments.ARG_LANG);
			if (!StringUtil.isEmptyString(lang))
			{
				System.setProperty("workbench.gui.language", lang);
			}

			String value = cmdLine.getValue(AppArguments.ARG_CONFIGDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.configdir", value);
			}

			value = cmdLine.getValue(AppArguments.ARG_LIBDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.libdir", value);
			}

			value = cmdLine.getValue(AppArguments.ARG_LOGFILE);
			if (!StringUtil.isEmptyString(value))
			{
				WbFile file = new WbFile(value);
				System.setProperty("workbench.log.filename", file.getFullPath());
			}

			// Make sure the Settings object is (re)initialized properly now that
			// some system properties have been read from the commandline
			// this is especially necessary during JUnit tests to make
			// sure a newly passed commandline overrules the previously initialized
			// Settings instance
			Settings.getInstance().initialize();

			String scriptname = cmdLine.getValue(AppArguments.ARG_SCRIPT);
			String cmd = cmdLine.getValue(AppArguments.ARG_COMMAND);

			boolean readDriverTemplates = true;
			boolean showHelp = cmdLine.isArgPresent("help");

			if (StringUtil.isBlank(scriptname) && StringUtil.isBlank(cmd) && !showHelp)
			{
				this.batchMode = false;
				String url = cmdLine.getValue(AppArguments.ARG_CONN_URL);
				String jar = cmdLine.getValue(AppArguments.ARG_CONN_JAR);
				if (!StringUtil.isEmptyString(url) && !StringUtil.isEmptyString(jar))
				{
					// Do not read the driver templates if a connection is specified directly
					readDriverTemplates = false;
				}
			}
			else
			{
				this.batchMode = true;
				readDriverTemplates = false;
			}

			value = cmdLine.getValue(AppArguments.ARG_VARDEF);
			if (!StringUtil.isEmptyString(value))
			{
				try
				{
					VariablePool.getInstance().readDefinition(StringUtil.trimQuotes(value));
				}
				catch (IOException e)
				{
					LogMgr.logError("WbManager.initCmdLine()", "Error reading variable definition from file " + value, e);
				}
			}

			if (cmdLine.isArgPresent(AppArguments.ARG_NOTEMPLATES))
			{
				readDriverTemplates = false;
			}
			ConnectionMgr.getInstance().setReadTemplates(readDriverTemplates);

			// Setting the profile storage should be done after initializing
			// the configuration stuff correctly!
			value = cmdLine.getValue(AppArguments.ARG_PROFILE_STORAGE);
			Settings.getInstance().setProfileStorage(value);

			if (cmdLine.isArgPresent(AppArguments.ARG_NOSETTNGS))
			{
				this.writeSettings = false;
			}

			if (cmdLine.hasUnknownArguments())
			{
				String unknown = cmdLine.getUnknownArguments();
				LogMgr.logError("WbManager.readParameters()", "The following parameters are invalid: " + unknown, null);
				if (batchMode)
				{
					System.err.println("Invalid parameter(s): " + unknown);
				}
			}

			LogMgr.logInfo("WbManager.init()", "Starting " + ResourceMgr.TXT_PRODUCT_NAME + ", " + ResourceMgr.getBuildInfo());
			LogMgr.logInfo("WbManager.init()", "Java version=" + System.getProperty("java.version")  + ", java.home=" + System.getProperty("java.home") + ", vendor=" + System.getProperty("java.vendor") );
			LogMgr.logInfo("WbManager.init()", "Operating System=" + System.getProperty("os.name")  + ", version=" + System.getProperty("os.version") + ", platform=" + System.getProperty("os.arch"));
		}
		catch (Exception e)
		{
			LogMgr.logError("WbManager.initCdmLine()", "Error initializing command line arguments!", e);
		}
	}

	public void startApplication()
	{
		// batchMode flag is set by readParameters()
		if (this.batchMode)
		{
			runBatch();
		}
		else
		{
			warmUp();

			// This will install the application listener if running under MacOS
			MacOSHelper m = new MacOSHelper();
			m.installApplicationHandler();

			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					runGui();
				}
			});
		}
	}

	protected void warmUp()
	{
		WbThread t = new WbThread("WarmUp")
		{
			public void run()
			{
				ResourceMgr.getResources();
				MacroManager.getInstance().getMacros();
				ConnectionMgr.getInstance().readProfiles();
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void runGui()
	{
//		WbSplash splash = null;
//		if (GuiSettings.getShowSplash())
//		{
//			splash = new WbSplash();
//			splash.setVisible(true);
//		}

//		try
//		{
			this.initUI();

			boolean pumper = cmdLine.isArgPresent(AppArguments.ARG_SHOW_PUMPER);
			boolean explorer = cmdLine.isArgPresent(AppArguments.ARG_SHOW_DBEXP);

			if (pumper)
			{
				new DataPumper().showWindow();
			}
			else if (explorer)
			{
				DbExplorerWindow.showWindow();
			}
			else
			{
				openNewWindow(true);
			}

			UpdateCheck upd = new UpdateCheck();
			upd.startUpdateCheck();
//		}
//		finally
//		{
//			if (splash != null)
//			{
//				splash.setVisible(false);
//				splash.dispose();
//			}
//		}
	}

	// Package visible for testing purposes
	int exitCode = 0;

	private void runBatch()
	{
		exitCode = 0;

		// Make sure batch mode is always using English
		// System.setProperty("workbench.gui.language", "en");

		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine);

		if (runner != null)
		{
			try
			{
				runner.connect();
			}
			catch (Exception e)
			{
				exitCode = 1;
				// no need to log connect errors, already done by BatchRunner and ConnectionMgr
				// runner.isSuccess() will also be false for the next step
			}

			try
			{
				// Do not check for runner.isConnected() as the in batch mode
				// the application might be started without a profile
				// (e.g. for a single WbCopy command)
				if (runner.isSuccess())
				{
					runner.execute();
					// Not all exceptions will be re-thrown by the batch runner
					// in order to be able to run the error script, so it is important
					// to check isSuccess() in order to return the correct status
					if (!runner.isSuccess()) exitCode = 2;
				}
			}
			catch (OutOfMemoryError e)
			{
				LogMgr.logError("WbManager.runBatch()", "Not enough memory to finish the operation. Aborting execution!", null);
				System.err.println("Not enough memory to finish the operation. Aborting execution!");
				exitCode = 10;
			}
			catch (Exception e)
			{
				exitCode = 2;
			}
			finally
			{
				ConnectionMgr mgr = ConnectionMgr.getInstance();
				if (mgr != null) mgr.disconnectAll();
			}
		}
		else
		{
			exitCode = 3;
		}
		this.doShutdown(exitCode);
	}

	public static void initConsoleMode(String[] args)
	{
		wb = new WbManager();
		wb.cmdLine.removeArgument("abortOnError");
		wb.cmdLine.removeArgument("cleanupError");
		wb.cmdLine.removeArgument("cleanupSuccess");
		wb.cmdLine.removeArgument("dbExplorer");
		wb.cmdLine.removeArgument("dataPumper");
		wb.cmdLine.removeArgument("encoding");
		wb.cmdLine.removeArgument("separateConnection");
		wb.cmdLine.removeArgument("script");
		wb.cmdLine.removeArgument("workspace");
		wb.readParameters(args);
		ConnectionMgr.getInstance().setReadTemplates(false);
		wb.writeSettings = false;
	}

	/**
	 * Prepare the Workbench "environment" to be used inside another
	 * application (e.g. for Unit testing)
	 */
	public static void prepareForEmbedded()
	{
		wb = new WbManager();
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
		String args = "-notemplates -nosettings";
		System.setProperty("workbench.system.doexit", "false");
		System.setProperty("workbench.gui.testmode", "true");
		wb.readParameters(new String[] { args} );
	}

	public boolean isTestMode()
	{
		return Boolean.getBoolean("workbench.gui.testmode");
	}

	public static void prepareForTest(String[] args)
	{

		wb = new WbManager();
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);

		// This will be used in isTestMode(). Basically the test
		// mode is used by DbDriver to skip the test if a driver library
		// is accessible because in test mode the drivers are not loaded
		// through our own class loader because they are already present
		// on the classpath.
		System.setProperty("workbench.gui.testmode", "true");
		System.setProperty("workbench.log.console", "false");
		wb.readParameters(args);
	}

	public static void main(String[] args)
	{
		wb = new WbManager();

		wb.cmdLine.parse(args);
		boolean showHelp = wb.cmdLine.isArgPresent("help");
		if (showHelp)
		{
			System.out.println(wb.cmdLine.getHelp());
			Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
			System.exit(0);
		}
		else
		{
			wb.readParameters(args);
			wb.startApplication();
		}
	}

	/**
	 *  this is for the shutdownhook
	 */
	public void run()
	{
		LogMgr.logDebug("WbManager.run()", "Shutdownhook called!");
		ConnectionMgr.getInstance().disconnectAll();
		saveSettings();
	}

}
