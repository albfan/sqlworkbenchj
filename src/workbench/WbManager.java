/*
 * WbManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ShowHelpAction;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.lnf.LnFDefinition;
import workbench.gui.lnf.LnFLoader;
import workbench.gui.tools.DataPumper;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.MacroManager;
import workbench.sql.VariablePool;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.gui.dialogs.WbSplash;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.gui.lnf.LnFManager;
import workbench.gui.profiles.ProfileKey;
import workbench.util.FileDialogUtil;
import workbench.util.WbCipher;
import workbench.util.WbNullCipher;
import workbench.util.WbThread;


/**
 * The main application "controller" for the jWorkbench
 * @author  support@sql-workbench.net
 */
public class WbManager
	implements FontChangedListener, Runnable
{
	private static WbManager wb;
	private List<MainWindow> mainWindows = Collections.synchronizedList(new LinkedList());
	private List<ToolWindow> toolWindows = new LinkedList();
	private WbCipher desCipher = null;
	private boolean batchMode = false;
	private boolean writeSettings = true;
	
	private static PrintStream traceOut;
	private static final String traceFile = "workbench.trc";
	
	static
	{
		boolean doTrace = "true".equalsIgnoreCase(System.getProperty("workbench.startuptrace", "false"));
		if (doTrace)
		{
			try
			{
				traceOut = new PrintStream(new FileOutputStream(traceFile));
				traceOut.println(ResourceMgr.TXT_PRODUCT_NAME + " startup trace enabled");
				traceOut.close();
			}
			catch (Exception e)
			{
				traceOut = null;
			}
		}
	}
	
	private Thread shutdownHook = new Thread(this);

	private WbManager()
	{
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}

	public static void trace(String msg)
	{
		if (traceOut != null) 
		{
			try
			{
				traceOut = new PrintStream(new FileOutputStream(traceFile,true));
				traceOut.println(msg);
				traceOut.close();
			}
			catch (Exception e)
			{
				traceOut = null;
			}
		}
	}
	
	
	/**
	 *	return an instance of the WbDesCipher.
	 *
	 *	this method uses class.forName() to create a new
	 *	instance of WbDesCipher() so that WbManager itself
	 *	does not reference the javax.crypto classes and
	 *	it can at least be loaded in JDK < 1.4 to give
	 *	out an error message.
	 */
	public WbCipher getDesCipher()
	{
		if (desCipher == null)
		{
			try
			{
				Class des = Class.forName("workbench.util.WbDesCipher");
				this.desCipher = (WbCipher)des.newInstance();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbManager.getDesCipher)", "Could not create WbDesCipher", e);
				this.desCipher = new WbNullCipher();
			}
		}
		return desCipher;
	}

	public static WbManager getInstance()
	{
		return wb;
	}

	public boolean writeSettings() { return this.writeSettings; }
	
	public void showHelpForProfiles(JDialog owner)
	{
		JDialog dialog = null;
		try
		{
			// Use reflection to load the HtmlViewer in order to
			// avoid unnecessary class loading during startup
			Class cls = Class.forName("workbench.gui.help.HtmlViewer");
			Class[] types = new Class[] { JDialog.class  };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { owner };
			dialog = (JDialog)cons.newInstance(args);
			
			Class[] noArgs = new Class[] {};
			Method m = cls.getMethod("showProfileHelp", noArgs);
			if (m != null)
			{
				m.invoke(dialog, new Object[]{});
			}
			dialog.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbManager.showDialog()", "Error when creating help viewer", ex);
		}
	}
	
	public void showDialog(String clazz)
	{
		JFrame parent = WbManager.getInstance().getCurrentWindow();
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
			WbSwingUtilities.center(dialog, parent);
			dialog.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbManager.showDialog()", "Error when creating dialog " + clazz, ex);
		}
	}

	public void showOutOfMemoryError()
	{
		System.gc();
		WbSwingUtilities.showErrorMessageKey(getCurrentWindow(), "MsgOutOfMemoryError");
	}
	
	public MainWindow getCurrentWindow()
	{
		if (this.mainWindows == null) return null;
		if (this.mainWindows.size() == 1)
		{
			return (MainWindow)this.mainWindows.get(0);
		}
		for (MainWindow w : mainWindows)
		{
			if (w.hasFocus()) return w;
		}
		return null;
	}

	public void registerToolWindow(ToolWindow aWindow)
	{
		this.toolWindows.add(aWindow);
	}

	public void unregisterToolWindow(ToolWindow aWindow)
	{
		if (aWindow == null) return;
		int index = this.toolWindows.indexOf(aWindow);
		if (index > -1)
		{
			this.toolWindows.remove(index);
		}
		if (this.toolWindows.size() == 0 && this.mainWindows.size() == 0)
		{
			if (aWindow instanceof JFrame)
			{
				this.exitWorkbench((JFrame)aWindow);
			}
			else
			{
				this.exitWorkbench();
			}
		}
	}

	private void closeToolWindows()
	{
		int count = this.toolWindows.size();
		for (ToolWindow w : toolWindows)
		{
			w.closeWindow();
		}
		this.toolWindows.clear();
	}

	public void fontChanged(String aFontKey, Font newFont)
	{
		if (aFontKey.equals(Settings.PROPERTY_DATA_FONT))
		{
			UIManager.put("Table.font", newFont);
			UIManager.put("TableHeader.font", newFont);
		}
	}

	public boolean isWindowsClassic() { return isWindowsClassic; }
	
	private boolean isWindowsClassic = false;
	private boolean isWindowsLNF = false;
	
	public boolean isWindowsLNF()
	{
		return isWindowsLNF;
	}
	
	private void initializeLookAndFeel()
	{
		trace("WbManager.initializeLookAndFeel() - start");
		try
		{
			String className = Settings.getInstance().getLookAndFeelClass();
			if (StringUtil.isEmptyString(className))
			{
				className = UIManager.getSystemLookAndFeelClassName();
			}
			LnFManager mgr = new LnFManager();
			LnFDefinition def = mgr.findLookAndFeel(className);
			if (def == null) 
			{
				LogMgr.logError("WbManager.initializeLookAndFeel()", "Specified Look & Feel " + className + " not available!", null);
				return;
			}
			
			LnFLoader loader = new LnFLoader(def);
			UIManager.put("jgoodies.useNarrowButtons", Boolean.FALSE);
			LookAndFeel lnf = loader.getLookAndFeel();
			UIManager.setLookAndFeel(lnf);
			try
			{
				String cls = lnf.getClass().getName();
				if (cls.indexOf("com.sun.java.swing.plaf.windows") > -1)
				{
					isWindowsLNF = true;
					String osVersion = System.getProperty("os.version", "1.0");
					Float version = Float.valueOf(osVersion);
					if (version.floatValue() <= 5.0)
					{
						isWindowsClassic = true;
					}
					else
					{
						isWindowsClassic = (cls.indexOf("WindowsClassicLookAndFeel") > -1);
						if (!isWindowsClassic)
						{
							Toolkit toolkit = Toolkit.getDefaultToolkit();
							Boolean themeActive = (Boolean)toolkit.getDesktopProperty("win.xpstyle.themeActive");
							if (themeActive != null)
							{
								isWindowsClassic = !themeActive.booleanValue();
							}
							else
							{
								isWindowsClassic = true;
							}
						}
					}
				}
			}
			catch (Throwable e)
			{
				isWindowsClassic = false;
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("Settings.initializeLookAndFeel()", "Could not set look and feel", e);
		}

		try
		{
			Toolkit.getDefaultToolkit().setDynamicLayout(Settings.getInstance().getUseDynamicLayout());
		}
		catch (Exception e)
		{
			LogMgr.logError("WbManager.initializeLookAndFeel()", "Error setting dynamic layout property", e);
		}
		trace("WbManager.initializeLookAndFeel() - done");
	}

	public String getJarPath()
	{
		URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		File f = null;
		try
		{
			// Sending the path through the URLDecoder is important
			// because otherwise a path with %20 will be created 
			// if the directory contains spaces!
			URLDecoder decoder = new URLDecoder();
			String p = decoder.decode(url.getFile(), "UTF-8");
			f = new File(p);
		}
		catch (Exception e)
		{
			// Fallback, should not happen
			String p = url.getFile();
			p = StringUtil.replace(p, "%20", " ");
			f = new File(p);
		}
		return f.getParentFile().getAbsolutePath();
	}
	
	private void initUI()
	{
		trace("WbManager.initUI() - start");
		this.initializeLookAndFeel();
		
		Settings settings = Settings.getInstance();
		UIDefaults def = UIManager.getDefaults();
		
		Font stdFont = settings.getStandardFont();
		if (stdFont != null)
		{
			def.put("Button.font", stdFont);
			def.put("CheckBox.font", stdFont);
			def.put("CheckBoxMenuItem.font", stdFont);
			def.put("ColorChooser.font", stdFont);
			def.put("ComboBox.font", stdFont);
			def.put("EditorPane.font", stdFont);
			def.put("FileChooser.font", stdFont);
			def.put("Label.font", stdFont);
			def.put("List.font", stdFont);
			def.put("Menu.font", stdFont);
			def.put("MenuItem.font", stdFont);
			def.put("OptionPane.font", stdFont);
			def.put("Panel.font", stdFont);
			def.put("PasswordField.font", stdFont);
			def.put("PopupMenu.font", stdFont);
			def.put("ProgressBar.font", stdFont);
			def.put("RadioButton.font", stdFont);
			def.put("TabbedPane.font", stdFont);
			def.put("TextArea.font", stdFont);
			def.put("TextField.font", stdFont);
			def.put("TextPane.font", stdFont);
			def.put("TitledBorder.font", stdFont);
			def.put("ToggleButton.font", stdFont);
			def.put("ToolBar.font", stdFont);
			def.put("ToolTip.font", stdFont);
			def.put("Tree.font", stdFont);
			def.put("ViewPort.font", stdFont);
			
			def.put("OptionPane.yesButtonText",ResourceMgr.getPlainString("LblYes"));
			def.put("OptionPane.yesButtonMnemonic",ResourceMgr.getAcceleratorChar("LblYes"));
			def.put("OptionPane.noButtonText",ResourceMgr.getPlainString("LblNo"));
			def.put("OptionPane.noButtonMnemonic",ResourceMgr.getAcceleratorChar("LblNo"));
			def.put("OptionPane.cancelButtonText",ResourceMgr.getPlainString("LblCancel"));
			def.put("OptionPane.cancelButtonMnemonic",ResourceMgr.getAcceleratorChar("LblCancel"));
			def.put("OptionPane.okButtonText",ResourceMgr.getPlainString("LblOK"));
			def.put("OptionPane.okButtonMnemonic",ResourceMgr.getAcceleratorChar("LblOK"));
			
			FileDialogUtil.initFileChooserLabels();
			
		}
		Font dataFont = settings.getDataFont();
		
		def.put("Table.font", dataFont);
		def.put("TableHeader.font", dataFont);
		
		// Polish up the standard look & feel settings

		Color c = settings.getColor("workbench.table.gridcolor", new Color(215,215,215));
		def.put("Table.gridColor", c);

		// use our own classes for some GUI elements
		def.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
		def.put("SplitPaneUI", "workbench.gui.components.WbSplitPaneUI");
		def.put("TabbedPaneUI", TabbedPaneUIFactory.getTabbedPaneUIClass());
		
		if (settings.getShowMnemonics())
		{
			def.put("Button.showMnemonics", Boolean.TRUE);
		}
		else
		{
			def.put("Button.showMnemonics", Boolean.FALSE);
		}

		settings.addFontChangedListener(this);
		trace("WbManager.initUI() - done");
	}

	public MainWindow createWindow()
	{
		trace("WbManager.createWindow() - start");
		MainWindow win = new MainWindow();
		this.mainWindows.add(win);
		trace("WbManager.createWindow() - done");
		return win;
	}

	private JDialog closeMessage;

	private boolean saveWindowSettings()
	{
		if (!this.writeSettings) return true;
		MainWindow w = this.getCurrentWindow();
		boolean settingsSaved = false;

		// the settings (i.e. size and position) should only be saved
		// for the first visible window
		if (w != null)
		{
			w.saveSettings();
			settingsSaved = true;
		}

		if (!this.checkProfiles(w)) return false;

		boolean result = true;
		for (MainWindow win : mainWindows)
		{
			if (win == null) continue;
			if (!settingsSaved)
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
		return true;
	}

	public boolean isBatchMode()
	{
	  return this.batchMode;
	}

	public void exitWorkbench()
	{
		MainWindow w = this.getCurrentWindow();
		this.exitWorkbench(w);
	}

	public void exitWorkbench(JFrame window)
	{
		// saveSettings() will also prompt if any modified
		// files should be changed
		boolean canExit = this.saveWindowSettings();
		if (!canExit) return;
		
		//shutdownInProgress = true;
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
		this.createCloseMessageWindow(window);
		if (this.closeMessage != null) this.closeMessage.setVisible(true);

		MacroManager.getInstance().saveMacros();
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
	}

	private void createCloseMessageWindow(JFrame parent)
	{
		if (parent == null) return;
		this.closeMessage = new JDialog(parent, false);
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.BEVEL_BORDER_RAISED);
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
	 *	this gets called from the thread that disconnects everything
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
		this.mainWindows.clear();
		this.closeToolWindows();
		ShowHelpAction.getInstance().closeHelp();
	}

	protected void saveSettings()
	{
		if (this.writeSettings && !this.isBatchMode()) 
		{
			Settings s = Settings.getInstance();
			FilterDefinitionManager.getInstance().saveMRUList();
			if (s != null) s.saveSettings();
		}
	}
	
	protected void doShutdown(int errorCode)
	{
		Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		this.closeAllWindows();
		saveSettings();
		LogMgr.logInfo("WbManager.doShutdown()", "Stopping " + ResourceMgr.TXT_PRODUCT_NAME + ", Build " + ResourceMgr.getString("TxtBuildNumber"));
		LogMgr.shutdown();
		boolean doExit = "true".equals(System.getProperty("workbench.system.doexit", "true"));
		if (doExit) System.exit(errorCode);
	}

	private boolean checkAbort(MainWindow win)
	{
		return WbSwingUtilities.getYesNo(win, ResourceMgr.getString("MsgAbortRunningSql"));
	}

	private boolean checkProfiles(MainWindow win)
	{
		if (ConnectionMgr.getInstance().profilesAreModified())
		{
			int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedProfiles"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				ConnectionMgr.getInstance().saveProfiles();
				return true;
			}
			else if (answer == JOptionPane.NO_OPTION)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return true;
	}

	class CloseThread
		extends WbThread
	{
		private MainWindow win;
		public CloseThread(String name, MainWindow w)
		{
			super(name);
			win = w;
		}
		
		public void run()
		{
			// First parameter tells the window to disconnect in the
			// current thread as we are already in a background thread
			// second parameter tells the window not to close the workspace
			// third parameter tells the window not to save the workspace
			win.disconnect(false, false, false);
			win.setVisible(false);
			win.dispose();
		}
	}
	
	public void windowClosing(final MainWindow win)
	{
		if (this.mainWindows.size() == 1)
		{
			// If only one window is present, shut down the application
			this.exitWorkbench(win);
		}
		else
		{
			if (!win.saveWorkspace()) return;
			this.mainWindows.remove(win);
			CloseThread t = new CloseThread("WindowDisconnect", win);
			t.start();
		}
		
	}

	/** 
	 * Open a new main window, but do not check any command line parameters. 
	 * 
	 * This methode will be called from the GUI
	 * when the user requests a new window
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

	private void openNewWindow(boolean checkCmdLine)
	{
		trace("WbManager.openNewWindow()");

		final MainWindow main = this.createWindow();
		main.display();
		boolean connected = false;


		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(ARG_PROFILE);
			String group = cmdLine.getValue(ARG_PROFILE_GROUP);
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

				// the main window will take of displaying the connection dialog
				// if the connection to the requested profile fails.
				connected = true;
			}

		}

		boolean autoSelect = Settings.getInstance().getBoolProperty("workbench.gui.autoconnect", true);
		boolean exitOnCancel = Settings.getInstance().getExitOnFirstConnectCancel();
		
		// no connection? then display the connection dialog
		if (!connected && autoSelect)
		{
			main.selectConnection(exitOnCancel);
		}
	}

	private ArgumentParser cmdLine;

	// Parameters for batch execution used by BatchRunner
	public static final String ARG_SCRIPT = "script";
	public static final String ARG_SCRIPT_ENCODING = "encoding";
	public static final String ARG_ABORT = "abortonerror";

	public static final String ARG_CONN_URL = "url";
	public static final String ARG_CONN_DRIVER = "driver";
	public static final String ARG_CONN_JAR = "driverjar";
	public static final String ARG_CONN_USER = "username";
	public static final String ARG_CONN_PWD = "password";
	public static final String ARG_CONN_AUTOCOMMIT = "autocommit";
	public static final String ARG_CONN_ROLLBACK = "rollbackondisconnect";
	public static final String ARG_IGNORE_DROP = "ignoredroperrors";
	public static final String ARG_DISPLAY_RESULT = "displayresult";
	public static final String ARG_SUCCESS_SCRIPT = "cleanupsuccess";
	public static final String ARG_ERROR_SCRIPT = "cleanuperror";
	public static final String ARG_SHOW_TIMING = "showtiming";
	public static final String ARG_FEEDBACK = "feedback";
	public static final String ARG_WORKSPACE = "workspace";
	public static final String ARG_ALT_DELIMITER = "altdelimiter";
	public static final String ARG_DELIMITER = "delimiter";

	// Other parameters
	public static final String ARG_PROFILE = "profile";
	public static final String ARG_PROFILE_GROUP = "profilegroup";
	public static final String ARG_SHOWPROGRESS = "showprogress";

	private static final String ARG_PROFILE_STORAGE = "profilestorage";

	private static final String ARG_CONFIGDIR = "configdir";
	private static final String ARG_LIBDIR = "libdir";
	private static final String ARG_LOGFILE = "logfile";
	public static final String ARG_VARDEF = "vardef";

	private static final String ARG_SHOW_PUMPER = "datapumper";
	private static final String ARG_SHOW_DBEXP = "dbexplorer";

	public static ArgumentParser createArgumentParser()
	{
		ArgumentParser parser = new ArgumentParser();
		parser.addArgument(ARG_PROFILE);
		parser.addArgument(ARG_FEEDBACK);
		parser.addArgument(ARG_PROFILE_GROUP);
		parser.addArgument(ARG_PROFILE_STORAGE);
		parser.addArgument(ARG_CONFIGDIR);
		parser.addArgument(ARG_LIBDIR);
		parser.addArgument(ARG_SCRIPT);
		parser.addArgument(ARG_SCRIPT_ENCODING);
		parser.addArgument(ARG_LOGFILE);
		parser.addArgument(ARG_ABORT);
		parser.addArgument(ARG_SUCCESS_SCRIPT);
		parser.addArgument(ARG_ERROR_SCRIPT);
		parser.addArgument(ARG_VARDEF);
		parser.addArgument(ARG_CONN_URL);
		parser.addArgument(ARG_CONN_DRIVER);
		parser.addArgument(ARG_CONN_JAR);
		parser.addArgument(ARG_CONN_USER);
		parser.addArgument(ARG_CONN_PWD);
		parser.addArgument(ARG_CONN_AUTOCOMMIT);
		parser.addArgument(ARG_CONN_ROLLBACK);
		parser.addArgument(ARG_SHOW_PUMPER);
		parser.addArgument(ARG_IGNORE_DROP);
		parser.addArgument(ARG_DISPLAY_RESULT);
		parser.addArgument(ARG_SHOW_DBEXP);
		parser.addArgument(ARG_SHOW_TIMING);
		parser.addArgument(ARG_SHOWPROGRESS);
		parser.addArgument(ARG_WORKSPACE);
		parser.addArgument("nosettings");
		parser.addArgument("notemplates");
		parser.addArgument(ARG_ALT_DELIMITER);
		parser.addArgument(ARG_DELIMITER);
		return parser;
	}
	
	private void initCmdLine(String[] args)
	{
		trace("WbManager.initCmdLine() - start");

		cmdLine = createArgumentParser();
		
		try
		{
			cmdLine.parse(args);
			
			String value = cmdLine.getValue(ARG_CONFIGDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.configdir", value);
			}
			value = cmdLine.getValue(ARG_LIBDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.libdir", value);
			}

			value = cmdLine.getValue(ARG_LOGFILE);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.log.filename", value);
			}

			String scriptname = cmdLine.getValue(ARG_SCRIPT);

			boolean readDriverTemplates = true;
			
			if (StringUtil.isEmptyString(scriptname))
			{
				this.batchMode = false;
				String url = cmdLine.getValue(ARG_CONN_URL);
				String jar = cmdLine.getValue(ARG_CONN_JAR);
				//String profile = cmdLine.getValue(ARG_PROFILE);
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
			
			if (cmdLine.isArgPresent("notemplates"))
			{
				readDriverTemplates = false;
			}

			ConnectionMgr.getInstance().setReadTemplates(readDriverTemplates);
			
			value = cmdLine.getValue(ARG_VARDEF);
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
			
			// Setting the profile storage should be done after initializing the configuration
			// stuff correctly!
			value = cmdLine.getValue(ARG_PROFILE_STORAGE);
			Settings.getInstance().setProfileStorage(value);
			
			if (cmdLine.isArgPresent("nosettings"))
			{
				trace("WbManager.initCmdLine() - Parameter nosettings specified. Settings will not be written!");
				this.writeSettings = false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		trace("WbManager.initCmdLine() - done");
	}

	public void init()
	{
		trace("WbManager.init() - start");

		LogMgr.logInfo("WbManager.init()", "Starting " + ResourceMgr.TXT_PRODUCT_NAME + ", " + ResourceMgr.getBuildInfo());
		LogMgr.logInfo("WbManager.init()", "Using Java version=" + System.getProperty("java.version")  + ", java.home=" + System.getProperty("java.home") + ", vendor=" + System.getProperty("java.vendor") );
		LogMgr.logDebug("WbManager.init()", "Use -Dworkbench.startuptrace=true to log messages during startup");

    if (this.cmdLine == null) this.initCmdLine(null);

		// batchMode flag is set by initCmdLine()
		if (this.batchMode)
		{
			runBatch();
		}
		else
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					runGui();
				}
			});
		}
		trace("WbManager.init() - done.");
	}

	protected void runGui()
	{
		WbSplash splash = null;
		if (Settings.getInstance().getShowSplash())
		{
			trace("WbManager.init() - opening splash window");
			splash = new WbSplash();
			splash.setVisible(true);
		}
		try
		{
			trace("WbManager.init() - initializing UI defaults");
			this.initUI();
			boolean pumper = cmdLine.isArgPresent(ARG_SHOW_PUMPER);
			boolean explorer = cmdLine.isArgPresent(ARG_SHOW_DBEXP);
			if (pumper)
			{
				DataPumper p = new DataPumper(null, null);
				p.showWindow(null);
			}
			else if (explorer)
			{
				DbExplorerWindow.showWindow();
			}
			else
			{
				this.openNewWindow(true);
			}
		}
		finally
		{
			if (splash != null)
			{
				trace("WbManager.init() - closing splash window");
				splash.setVisible(false);
				splash.dispose();
			}
		}
	}

	private void runBatch()
	{
		int exitCode = 0;
		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine);
		int step = 1;

		if (runner != null)
		{
			try
			{
				runner.connect();
				if (runner.isSuccess())
				{
					step = 2;
					runner.execute();
				}
			}
			catch (Exception e)
			{
				exitCode = 1;
				// no need to log connect errors, already done by ConnectionMgr
				if (step == 2)
				{
					LogMgr.logError("WbManager.runBatch()", "Error running batch scripts", e);
				}
			}
			finally
			{
				ConnectionMgr mgr = ConnectionMgr.getInstance();
				if (mgr != null) mgr.disconnectAll();
			}
			if (!runner.isSuccess() && exitCode == 0) exitCode = 2;
		}
		else
		{
			exitCode = 3;
		}
		this.doShutdown(exitCode);
	}

	/**
	 * For testing purposes only!
	 */
	public static void prepareForTest(String configDir)
	{
		wb = new WbManager();
		
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
		String args[] = { "-notemplates -nosettings -configdir=" + configDir };
		
		wb.initCmdLine(args);
	}
	
	public static void main(String[] args)
	{
		wb = new WbManager();
		trace("WbManager.main() - start");
		// the command line needs to be initialized before everything
		// else, in order to set some of the system poperties correctly
		// e.g. the configdir.
		wb.initCmdLine(args);
		wb.init();
		trace("WbManager.main() - done");
	}

	/**
	 *  this is for the shutdownhook
	 */
	public void run()
	{
		LogMgr.logDebug("WbManager.run()", "Shutdownhook called!");
		saveSettings();
	}

}
