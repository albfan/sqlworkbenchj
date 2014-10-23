/*
 * WbManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.EventQueue;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import workbench.interfaces.FontChangedListener;
import workbench.interfaces.ToolWindow;
import workbench.interfaces.ToolWindowManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.gui.DisconnectInfo;
import workbench.gui.MainWindow;
import workbench.gui.WbFocusManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.bookmarks.BookmarkManager;
import workbench.gui.components.ColumnOrderMgr;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.gui.lnf.LnFHelper;
import workbench.gui.profiles.ProfileKey;
import workbench.gui.tools.DataPumper;
import workbench.gui.tools.ObjectSourceSearchPanel;

import workbench.sql.BatchRunner;
import workbench.sql.OutputPrinter;
import workbench.sql.VariablePool;
import workbench.sql.macros.MacroManager;

import workbench.util.DeadlockMonitor;
import workbench.util.FileUtil;
import workbench.util.MacOSHelper;
import workbench.util.MemoryWatcher;
import workbench.util.StringUtil;
import workbench.util.UpdateCheck;
import workbench.util.VersionNumber;
import workbench.util.WbFile;
import workbench.util.WbThread;


/**
 * The main application "controller" for the SQL Workbench/J
 *
 * @author Thomas Kellerer
 */
public final class WbManager
	implements FontChangedListener, Runnable, Thread.UncaughtExceptionHandler, ToolWindowManager
{
	private static WbManager wb;
	private final List<MainWindow> mainWindows = Collections.synchronizedList(new ArrayList<MainWindow>(5));
	private final List<ToolWindow> toolWindows = Collections.synchronizedList(new ArrayList<ToolWindow>(5));

	private RunMode runMode;
	private boolean writeSettings = true;
	private boolean overWriteGlobalSettingsFile = true;
	private boolean outOfMemoryOcurred;
	private WbThread shutdownHook = new WbThread(this, "ShutdownHook");
	private DeadlockMonitor deadlockMonitor;

	private final AppArguments cmdLine = new AppArguments();
	private boolean isWindowsClassic;
	private JDialog closeMessage;

	private WbManager()
	{
		this(false);
	}

	private WbManager(boolean forConsole)
	{
		if (!forConsole)
		{
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	public static WbManager getInstance()
	{
		return wb;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable error)
	{
		error.printStackTrace();
		LogMgr.logError("WbManager.uncaughtException()", "Thread '" + thread.getName() + "' caused an exception!", error);
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
			if (dialog.isResizable())
			{
				Settings.getInstance().restoreWindowSize(dialog);
			}
			WbSwingUtilities.center(dialog, parent);
			dialog.setVisible(true);
			if (dialog.isResizable())
			{
				Settings.getInstance().storeWindowSize(dialog);
			}
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

	public boolean isDevBuild()
	{
		VersionNumber buildNumber = ResourceMgr.getBuildNumber();
		return buildNumber.getMajorVersion() == 999 || buildNumber.getMinorVersion() != -1;
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

	private JFrame getCurrentToolWindow()
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

	@Override
	public void registerToolWindow(ToolWindow aWindow)
	{
		synchronized (toolWindows)
		{
			toolWindows.add(aWindow);
		}
	}

	@Override
	public void unregisterToolWindow(ToolWindow toolWindow)
	{
		if (toolWindow == null) return;
		synchronized (toolWindows)
		{
			toolWindows.remove(toolWindow);

			if (this.toolWindows.isEmpty() && this.mainWindows.isEmpty())
			{
				this.exitWorkbench(toolWindow.getWindow(), false);
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
			toolWindows.clear();
		}
	}

	@Override
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
	 * Returns the location of the application's jar file.
	 *
	 * @return the file object denoting the running jar file.
	 * @see #getJarPath()
	 */
	public File getJarFile()
	{
		URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		File f;
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
	 * Returns the directory in which the application is installed.
	 *
	 * @return the full path to the jarfile
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
		Settings.getInstance().addFontChangedListener(this);
		if (Settings.getInstance().getBoolProperty("workbench.gui.install.focusmgr", true))
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					FocusManager.setCurrentManager(WbFocusManager.getInstance());
				}
			});
		}
	}

	/**
	 * Saves the preferences of all open MainWindows.
	 *
	 * @return true if the preferences were saved successfully
	 *         false if at least on MainWindow "refused" to close
	 */
	private boolean saveWindowSettings()
	{
		// no settings should be saved, pretend everything was done.
		if (!this.writeSettings) return true;

		boolean settingsSaved = false;

		if (!this.checkProfiles(getCurrentWindow())) return false;

		boolean result;
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

		// No window with focus found, saveAs the size and position of the last opened window
		if (!settingsSaved && mainWindows.size() > 0)
		{
			mainWindows.get(mainWindows.size() - 1).saveSettings();
		}

		return true;
	}

	public RunMode getRunMode()
	{
		assert runMode != null;
		return runMode;
	}

	public boolean isGUIMode()
	{
		assert runMode != null;
		return runMode == RunMode.GUI;
	}

	public boolean isConsoleMode()
	{
		assert runMode != null;
		return runMode == RunMode.Console;
	}

	public boolean isBatchMode()
	{
		assert runMode != null;
		return runMode == RunMode.Batch;
	}

	public boolean canExit()
	{
		if (this.saveWindowSettings())
		{
			if (Settings.getInstance().wasExternallyModified())
			{
				String msg = ResourceMgr.getFormattedString("MsgSettingsChanged", Settings.getInstance().getConfigFile().getFullPath());
				int choice = WbSwingUtilities.getYesNoCancel(getCurrentWindow(), msg);
				LogMgr.logDebug("WbManager.canExit()", "Config file overwrite choice: " + WbSwingUtilities.choiceToString(choice));
				this.overWriteGlobalSettingsFile = (choice == JOptionPane.OK_OPTION);
				return choice != JOptionPane.CANCEL_OPTION;
			}
			return true;
		}
		else
		{
			LogMgr.logDebug("WbManager.canExit()", "saveWindowSettings() returned false!");
			return false;
		}
	}

	public void exitWorkbench(boolean forceAbort)
	{
		JFrame w = this.getCurrentWindow();
		this.exitWorkbench(w, forceAbort);
	}

	public void exitWorkbench(final JFrame window, final boolean forceAbort)
	{
		// canExit() will also prompt if any modified files should be changed
		if (!canExit())
		{
			return;
		}

		if (window == null)
		{
			ConnectionMgr.getInstance().disconnectAll();
			this.doShutdown(0);
			return;
		}

		// When disconnecting it can happen that the disconnect itself
		// takes some time. Because of this, a small window is displayed
		// that the disconnect takes place, and the actual disconnect is
		// carried out in a different thread to not block the AWT thread.
		// If it takes too long the user can still abort the JVM ...
		WbSwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				createCloseMessageWindow(window);
				if (closeMessage != null) closeMessage.setVisible(true);
			}
		});

		MacroManager.getInstance().save();
		Thread t = new WbThread("WbManager disconnect")
		{
			@Override
			public void run()
			{
				disconnectWindows(forceAbort);
				ConnectionMgr.getInstance().disconnectAll();
				disconnected();
			}
		};
		t.start();
	}

	private void createCloseMessageWindow(JFrame parent)
	{
		if (parent == null) return;
		ActionListener abort = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				doShutdown(0);
			}
		};

		this.closeMessage = new DisconnectInfo(parent, abort, "MsgAbortImmediately");
		WbSwingUtilities.center(this.closeMessage, parent);
	}

	private void disconnectWindows(boolean forceAbort)
	{
		for (MainWindow w : mainWindows)
		{
			if (w == null) continue;
			if (forceAbort)
			{
				w.forceDisconnect();
			}
			else
			{
				w.abortAll();
				w.disconnect(false, true, false);
			}
		}
	}

	/**
	 *	this gets called from exitWorkbench() when disconnecting everything
	 */
	private void disconnected()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (closeMessage != null)
				{
					closeMessage.setVisible(false);
					closeMessage.dispose();
					closeMessage = null;
				}
			}
		});
		doShutdown(0);
	}

	private void closeAllWindows()
	{
		LogMgr.logDebug("WbManager.closeAllWindows()", "Closing all open windows");
		for (MainWindow w : mainWindows)
		{
			if (w != null)
			{
				try { w.setVisible(false); } catch (Throwable th) {}
				try { w.dispose(); } catch (Throwable th) {}
			}
		}
		mainWindows.clear();
		closeToolWindows();
	}

	public void saveConfigSettings()
	{
		if (this.writeSettings && !this.isBatchMode())
		{
			if (overWriteGlobalSettingsFile)
			{
				Settings.getInstance().saveSettings(outOfMemoryOcurred);
			}
			else
			{
				LogMgr.logInfo("WbManager.saveSettings()", "Not overwritting global settings!");
			}

			FilterDefinitionManager.getInstance().saveMRUList();
			try
			{
				ColumnOrderMgr.getInstance().saveSettings();
			}
			catch (Exception e)
			{
				LogMgr.logError("WbManager.saveSettings()", "Could not write column order storage", e);
			}
		}
		else
		{
			LogMgr.logDebug("WbManager.saveConfigSettings()", "Settings not saved. writeSettings=" + writeSettings + ", runMode=" + runMode);
		}
	}

	public void removeShutdownHook()
	{
		if (this.shutdownHook != null)
		{
			try
			{
				Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			}
			catch (Throwable ex)
			{
				// ignore, we can't do anything about it anyway
			}
			this.shutdownHook = null;
		}
		if (this.deadlockMonitor != null)
		{
			this.deadlockMonitor.cancel();
		}
	}

	public void doShutdown(int errorCode)
	{
		removeShutdownHook();
		closeAllWindows();
		saveConfigSettings();
		LogMgr.logInfo("WbManager.doShutdown()", "Stopping " + ResourceMgr.TXT_PRODUCT_NAME + ", Build " + ResourceMgr.getString("TxtBuildNumber"));
		LogMgr.shutdown();
		// The property workbench.system.doexit can be used to embedd the sqlworkbench.jar
		// in other applications and still be able to call doShutdown()
		if (shouldDoSystemExit()) System.exit(errorCode);
	}

	public static boolean shouldDoSystemExit()
	{
		return "true".equals(System.getProperty("workbench.system.doexit", "true"));
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
	 * @param win the window to close
	 *
	 * @see workbench.gui.MainWindow#windowClosing(java.awt.event.WindowEvent)
	 * @see workbench.gui.MainWindow#connectCancelled()
	 */
	public void closeMainWindow(final MainWindow win)
	{
		if (this.mainWindows.size() == 1)
		{
			// If only one window is present, shut down the application
			this.exitWorkbench(win, win.isBusy());
		}
		else if (win != null)
		{
			if (win.isBusy())
			{
				if (!checkAbort(win)) return;
			}

			if (!win.saveWorkspace()) return;

			this.mainWindows.remove(win);
			BookmarkManager.getInstance().clearBookmarksForWindow(win.getWindowId());

			WbThread t = new WbThread(win.getWindowId() + " Disconnect")
			{
				@Override
				public void run()
				{
					// First parameter tells the window to disconnect in the
					// current thread as we are already in a background thread
					// second parameter tells the window not to close the workspace
					// third parameter tells the window not to saveAs the workspace
					// this does not need to happen on the EDT
					win.disconnect(false, false, false);
					win.setVisible(false);
					win.dispose();
					ConnectionMgr.getInstance().dumpConnections();
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
			@Override
			public void run()
			{
				openNewWindow(false);
			}
		});
	}

	private void openNewWindow(boolean checkCmdLine)
	{
		final MainWindow main = new MainWindow();
		mainWindows.add(main);
		main.display();
		boolean connected = false;

		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(AppArguments.ARG_PROFILE);
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			ConnectionProfile prof;
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
				main.connectTo(prof, true, true);

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
				@Override
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

			if (cmdLine.isArgPresent(AppArguments.ARG_LOG_ALL_STMT))
			{
				boolean logAllStmts = cmdLine.getBoolean(AppArguments.ARG_LOG_ALL_STMT, false);
				System.setProperty(Settings.PROPERTY_LOG_ALL_SQL, Boolean.toString(logAllStmts));
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

			value = cmdLine.getValue(AppArguments.ARG_LOGLEVEL);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.log.level", value);
			}

			if (cmdLine.isArgPresent(AppArguments.ARG_NOSETTNGS))
			{
				this.writeSettings = false;
			}

			List<String> list = cmdLine.getList(AppArguments.ARG_PROP);
			for (String propDef : list)
			{
				String[] elements = propDef.split("=");
				if (elements.length == 2)
				{
					System.setProperty(elements[0], elements[1]);
				}
			}

			// Make sure the Settings object is (re)initialized properly now that
			// some system properties have been read from the commandline
			// this is especially necessary during JUnit tests to make
			// sure a newly passed commandline overrules the previously initialized
			// Settings instance
			Settings.getInstance().initialize();

			String scriptname = cmdLine.getValue(AppArguments.ARG_SCRIPT);
			String cmd = cmdLine.getValue(AppArguments.ARG_COMMAND);

			if (StringUtil.isEmptyString(cmd) && cmdLine.isArgPresent(AppArguments.ARG_COMMAND))
			{
				cmd = FileUtil.getSystemIn();
				cmdLine.setCommandString(cmd);
			}

			boolean readDriverTemplates = true;
			boolean showHelp = cmdLine.isArgPresent("help");
			boolean hasScript = StringUtil.isNonBlank(scriptname) || StringUtil.isNonBlank(cmd) ;

			this.runMode = RunMode.GUI;

			if (BatchRunner.hasConnectionArgument(cmdLine) || showHelp || hasScript)
			{
				// Do not read the driver templates in batchmode
				readDriverTemplates = false;
			}

			if (hasScript || showHelp)
			{
				this.runMode = RunMode.Batch;
			}

			List<String> vars = cmdLine.getList(AppArguments.ARG_VARDEF);
			for (String var : vars)
			{
				try
				{
					VariablePool.getInstance().readDefinition(StringUtil.trimQuotes(var));
				}
				catch (IOException e)
				{
					LogMgr.logError("WbManager.initCmdLine()", "Error reading variable definition from file " + var, e);
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

			value = cmdLine.getValue(AppArguments.ARG_MACRO_STORAGE);
			Settings.getInstance().setMacroStorage(value);

			LogMgr.logInfo("WbManager.init()", "Starting " + ResourceMgr.TXT_PRODUCT_NAME + ", " + ResourceMgr.getBuildInfo());
			LogMgr.logInfo("WbManager.init()", "Java version=" + System.getProperty("java.version")  + ", java.home=" + System.getProperty("java.home") + ", vendor=" + System.getProperty("java.vendor") + ", name=" + System.getProperty("java.vm.name"));
			LogMgr.logInfo("WbManager.init()", "Operating System=" + System.getProperty("os.name")  + ", version=" + System.getProperty("os.version") + ", platform=" + System.getProperty("os.arch"));

			long maxMem = MemoryWatcher.MAX_MEMORY / (1024*1024);
			LogMgr.logInfo("WbManager.init()", "Available memory: " + maxMem + "MB");

			if (cmdLine.isArgPresent(AppArguments.ARG_NOSETTNGS))
			{
				LogMgr.logInfo("WbManager.init()", "The '" + AppArguments.ARG_NOSETTNGS + "' option was specified on the commandline. Global settings will not be saved!");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbManager.initCdmLine()", "Error initializing command line arguments!", e);
		}
	}

	public void startApplication()
	{
		// batchMode flag is set by readParameters()
		if (isBatchMode())
		{
			runBatch();
		}
		else
		{
			boolean doWarmup = Settings.getInstance().getBoolProperty("workbench.gui.warmup", false);
			if (!doWarmup)
			{
				// if the connection dialog is not show, pre-load the profiles
				doWarmup = !Settings.getInstance().getShowConnectDialogOnStartup();
			}

			if (doWarmup)
			{
				warmUp();
			}

			// This will install the application listener if running under MacOS
			MacOSHelper m = new MacOSHelper();
			m.installApplicationHandler();

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					runGui();
				}
			});
		}
	}

	private void warmUp()
	{
		WbThread t1 = new WbThread("BackgroundProfilesLoader")
		{
			@Override
			public void run()
			{
				ConnectionMgr.getInstance().getProfiles();
			}
		};
		t1.start();

		WbThread t2 = new WbThread("BackgroundMacrosLoader")
		{
			@Override
			public void run()
			{
				MacroManager.getInstance(); // get instance will trigger loading the default macros
			}
		};
		t2.start();
	}

	public void runGui()
	{
		this.initUI();

		boolean pumper = cmdLine.isArgPresent(AppArguments.ARG_SHOW_PUMPER);
		boolean explorer = cmdLine.isArgPresent(AppArguments.ARG_SHOW_DBEXP);
		boolean searcher = cmdLine.isArgPresent(AppArguments.ARG_SHOW_SEARCHER);

		if (pumper)
		{
			new DataPumper().showWindow();
		}
		else if (explorer)
		{
			DbExplorerWindow.showWindow();
		}
		else if (searcher)
		{
			new ObjectSourceSearchPanel().showWindow();
		}
		else
		{
			openNewWindow(true);
		}

		if (Settings.getInstance().getBoolProperty("workbench.gui.debug.deadlockmonitor.enabled", true))
		{
			LogMgr.logInfo("WbManager.runGui()", "Starting DeadlockMonitor");
			deadlockMonitor = new DeadlockMonitor();
			deadlockMonitor.start();
		}

		UpdateCheck upd = new UpdateCheck();
		upd.startUpdateCheck();
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

			OutputPrinter printer = new OutputPrinter()
			{

				@Override
				public void printMessage(String message)
				{
					System.out.println(message);
				}
			};

			runner.setTraceOutput(printer);

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
		wb = new WbManager(false);
		wb.cmdLine.removeArgument(AppArguments.ARG_ABORT);
		wb.cmdLine.removeArgument(AppArguments.ARG_SUCCESS_SCRIPT);
		wb.cmdLine.removeArgument(AppArguments.ARG_ERROR_SCRIPT);
		wb.cmdLine.removeArgument(AppArguments.ARG_SHOW_PUMPER);
		wb.cmdLine.removeArgument(AppArguments.ARG_SHOW_DBEXP);
		wb.cmdLine.removeArgument(AppArguments.ARG_SHOW_SEARCHER);
		wb.cmdLine.removeArgument(AppArguments.ARG_SCRIPT_ENCODING);
		wb.cmdLine.removeArgument(AppArguments.ARG_CONN_SEPARATE);
		wb.cmdLine.removeArgument(AppArguments.ARG_SCRIPT);
		wb.cmdLine.removeArgument(AppArguments.ARG_WORKSPACE);
		wb.cmdLine.removeArgument(AppArguments.ARG_CONSOLIDATE_LOG);
		wb.cmdLine.removeArgument(AppArguments.ARG_PROFILE_STORAGE);
		wb.readParameters(args);
		wb.runMode = RunMode.Console;
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
		System.setProperty(Settings.TEST_MODE_PROPERTY, "true");
		wb.readParameters(new String[] { args} );
	}

	public static boolean isTest()
	{
		return "true".equals(System.getProperty(Settings.TEST_MODE_PROPERTY, "false"));
	}

	public static void prepareForTest(String[] args)
	{

		wb = new WbManager();
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);

		// The test mode is used by DbDriver to skip the test if a driver library
		// is accessible because in test mode the drivers are not loaded
		// through our own class loader as they are already present
		// on the classpath.
		// It is also used by Settings.initLogging() to allow a second
		// initialization of the LogMgr
		System.setProperty(Settings.TEST_MODE_PROPERTY, "true");

		System.setProperty("workbench.log.console", "false");
		System.setProperty("workbench.log.log4j", "false");
		System.setProperty("workbench.gui.language", "en");
		wb.readParameters(args);
	}

	public static void main(String[] args)
	{
		wb = new WbManager();

		wb.cmdLine.parse(args);
		boolean showHelp = wb.cmdLine.isArgPresent("help");
		boolean showVersion = wb.cmdLine.isArgPresent("version");
		if (showHelp)
		{
			System.out.println(wb.cmdLine.getHelp());
			Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
			System.exit(0);
		}
		else if (showVersion)
		{
			System.out.println(ResourceMgr.TXT_PRODUCT_NAME + " " + ResourceMgr.getBuildInfo());
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
	 *  This is the callback method for the shutdownhook.
	 */
	@Override
	public void run()
	{
		LogMgr.logWarning("WbManager.shutdownHook()", "SQL Workbench/J process has been interrupted.");
		saveConfigSettings();

		boolean exitImmediately = Settings.getInstance().getBoolProperty("workbench.exitonbreak", true);
		if (exitImmediately)
		{
			LogMgr.logWarning("WbManager.shutdownHook()", "Aborting process...");
			LogMgr.shutdown();
			Runtime.getRuntime().halt(15); // exit() doesn't work properly from inside a shutdownhook!
		}
		else
		{
			ConnectionMgr.getInstance().disconnectAll();
			LogMgr.shutdown();
		}
	}

}
