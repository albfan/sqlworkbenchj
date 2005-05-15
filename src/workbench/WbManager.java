/*
 * WbManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.help.HtmlViewer;
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
	private Settings settings;
	private ArrayList mainWindows = new ArrayList();
	private ArrayList toolWindows = new ArrayList();
	private WbCipher desCipher = null;
	private boolean batchMode = false;
	public static boolean trace = "true".equalsIgnoreCase(System.getProperty("workbench.startuptrace", "false"));
	private HtmlViewer helpWindow;
	private boolean shutdownInProgress = false;

	private Thread shutdownHook = new Thread(this);

	private WbManager()
	{
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
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
				this.desCipher = new WbNullCipher();
			}
		}
		return desCipher;
	}

	public static WbManager getInstance()
	{
		return wb;
	}

	public MainWindow getCurrentWindow()
	{
		if (this.mainWindows == null) return null;
		if (this.mainWindows.size() == 1)
		{
			return (MainWindow)this.mainWindows.get(0);
		}
		for (int i=0; i < this.mainWindows.size(); i++)
		{
			MainWindow w = (MainWindow)this.mainWindows.get(i);
			if (w.hasFocus()) return w;
		}
		return null;
	}

	public void changeLookAndFeel(String className)
	{
		try
		{
			Settings.getInstance().setLookAndFeelClass(className);
			UIManager.setLookAndFeel(className);
			for (int i=0; i < this.mainWindows.size(); i++)
			{
				MainWindow w = (MainWindow)this.mainWindows.get(i);
				SwingUtilities.updateComponentTreeUI(w);
			}
			for (int i=0; i < this.toolWindows.size(); i++)
			{
				Component c = (Component)this.toolWindows.get(i);
				SwingUtilities.updateComponentTreeUI(c);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbManager.changeLookAndFeel()", "Error setting new look and feel to " + className, e);
		}
	}
	
	public ToolWindow findToolWindow(Class clz)
	{
		int count = this.toolWindows.size();
		if (count == 0) return null;
		for (int i=0; i < count; i ++)
		{
			ToolWindow w = (ToolWindow)this.toolWindows.get(i);
			if (clz.isInstance(w)) return w;
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
		for (int i=0; i < count; i ++)
		{
			ToolWindow w = (ToolWindow)this.toolWindows.get(i);
			w.closeWindow();
		}
		this.toolWindows.clear();
	}

	public void fontChanged(String aFontKey, Font newFont)
	{
		if (aFontKey.equals(Settings.DATA_FONT_KEY))
		{
			UIManager.put("Table.font", newFont);
			UIManager.put("TableHeader.font", newFont);
		}
	}

	private void setLookAndFeel()
	{
		if (trace) System.out.println("WbManager.setLookAndFeel() - start");
		try
		{
			String className = this.settings.getLookAndFeelClass();
			if (className != null && className.trim().length() > 0)
			{
				UIManager.setLookAndFeel(className);
			}
			else
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("Settings.setLookAndFeel()", "Could not set look and feel", e);
		}


		try
		{
			Toolkit.getDefaultToolkit().setDynamicLayout(settings.getUseDynamicLayout());
		}
		catch (Exception e)
		{
		}
		if (trace) System.out.println("WbManager.setLookAndFeel() - done");
	}

	private void initUI()
	{
		if (trace) System.out.println("WbManager.initUI() - start");
		this.setLookAndFeel();

		UIDefaults def = UIManager.getDefaults();
		Font stdFont = this.settings.getStandardFont();

		def.put("Button.font", stdFont);
		def.put("CheckBox.font", stdFont);
		def.put("CheckBoxMenuItem.font", stdFont);
		def.put("ColorChooser.font", stdFont);
		def.put("ComboBox.font", stdFont);
		def.put("EditorPane.font", stdFont);
		def.put("FileChooser.font", stdFont);
		def.put("InternalFrame.font", stdFont);
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

		Font dataFont = this.settings.getDataFont();

		def.put("Table.font", dataFont);
		def.put("TableHeader.font", dataFont);

		// Polish up the standard look & feel settings
		Color c = this.settings.getColor("workbench.table.gridcolor");
		if (c == null)
		{
			c = Color.LIGHT_GRAY;
		}
		def.put("Table.gridColor", c);

		// use our own classes for some GUI elements
		def.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
		def.put("SplitPaneUI", "workbench.gui.components.WbSplitPaneUI");

		if (settings.getShowMnemonics())
		{
			def.put("Button.showMnemonics", Boolean.TRUE);
		}
		else
		{
			def.put("Button.showMnemonics", Boolean.FALSE);
		}

		this.settings.addFontChangedListener(this);
		if (trace) System.out.println("WbManager.initUI() - done");
	}

	public MainWindow createWindow()
	{
		if (trace) System.out.println("WbManager.createWindow() - start");
		MainWindow win = new MainWindow();
		this.mainWindows.add(win);
		if (trace) System.out.println("WbManager.createWindow() - done");
		return win;
	}

	private JDialog closeMessage;

	private boolean saveWindowSettings()
	{
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

		int count = this.mainWindows.size();
		boolean result = true;
		for (int i=0; i < count; i++)
		{
			w = (MainWindow)this.mainWindows.get(i);
			if (w == null) continue;
			if (i == 0 && !settingsSaved)
			{
				w.saveSettings();
				settingsSaved = true;
			}
			if (w.isBusy())
			{
				if (!this.checkAbort(w)) return false;
			}
			result = w.saveWorkspace();
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
		shutdownInProgress = true;
		if (window == null)
		{
			ConnectionMgr.getInstance().disconnectAll();
			this.doShutdown();
			return;
		}

		// When disconnecting it can happen that the disconnect itself
		// takes some time. Because of this, a small window is displayed
		// that the disconnect takes place, and the actual disconnect is
		// carried out in a different thread to not block the AWT thread.

		// If it takes too long the user can still abort the JVM ...
		this.createCloseMessageWindow(window);
		if (this.closeMessage != null) this.closeMessage.show();

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

	public boolean isShutdownInProgress() { return this.shutdownInProgress; }

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
					doShutdown();
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

	private void disconnectWindows()
	{
		MainWindow w = null;
		boolean aborted = false;
		for (int i=0; i < mainWindows.size(); i ++)
		{
			w = (MainWindow)this.mainWindows.get(i);
			if (w == null) continue;
			aborted = w.abortAll();
			w.disconnect(false, true);
		}
	}

	/**
	 *	this gets called from the thread that disconnects everything
	 */
	private void disconnected()
	{
		if (this.closeMessage != null)
		{
			this.closeMessage.setVisible(false);
			this.closeMessage.dispose();
		}

		try
		{
			if (SwingUtilities.isEventDispatchThread())
			{
				closeAllWindows();
			}
			else
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						closeAllWindows();
					}
				});
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			doShutdown();
		}
	}

	private void closeAllWindows()
	{
		int size = this.mainWindows.size();
		for (int i=0; i < size; i ++)
		{
			MainWindow w = (MainWindow)this.mainWindows.get(i);
			if (w != null)
			{
				w.setVisible(false);
				w.dispose();
			}
		}
		this.mainWindows.clear();
		this.closeToolWindows();
	}

	private void doShutdown()
	{
		doShutdown(0);
	}

	private void doShutdown(int errorCode)
	{
		Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		this.shutdownInProgress = true;
		this.closeAllWindows();
		if (!this.isBatchMode()) settings.saveSettings();
		LogMgr.logInfo("WbManager.doShutdown()", "Stopping " + ResourceMgr.TXT_PRODUCT_NAME + ", Build " + ResourceMgr.getString("TxtBuildNumber"));
		LogMgr.shutdown();
		System.exit(errorCode);
	}

	private boolean checkAbort(MainWindow win)
	{
		return WbSwingUtilities.getYesNo(win, ResourceMgr.getString("MsgAbortRunningSql"));
	}

	private boolean checkMacros(MainWindow win)
	{
		if (MacroManager.getInstance().isModified())
		{
      int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedMacros"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION)
      {
        MacroManager.getInstance().saveMacros();
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
  private boolean checkProfiles(MainWindow win)
  {
    if (ConnectionMgr.getInstance().profilesChanged())
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
			Thread t = new WbThread("WbManager Window Disconnect")
			{
				public void run()
				{
					// First parameter tells the window to disconnect the window in a
					// separate thread as we are already in a background thread
					// second parameter tells the window not to save the workspace
					win.disconnect(false, false);
					win.hide();
					win.dispose();
				}
			};
			t.start();
		}

	}

	// open a new window, but do not check any command line
	// parameters. This methode will be called from the GUI
	// when the user requests a new window
	public void openNewWindow()
	{
		this.openNewWindow(false);
	}

	private void openNewWindow(boolean checkCmdLine)
	{
		if (trace) System.out.println("WbManager.openNewWindow()");
		final MainWindow main = this.createWindow();

		main.show();
		main.restoreState();
		boolean connected = false;

		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(ARG_PROFILE);
			if (profilename != null && profilename.trim().length() > 0)
			{
				ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(profilename);
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
		}

		// no connection? then display the connection dialog
		if (!connected)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					main.selectConnection();
				}
			});
		}
	}

	private ArgumentParser cmdLine;

	// Parameters for batch execution used by BatchRunner
	public static final String ARG_SCRIPT = "script";
	public static final String ARG_ABORT = "abortonerror";

	public static final String ARG_CONN_URL = "url";
	public static final String ARG_CONN_DRIVER = "driver";
	public static final String ARG_CONN_JAR = "driverjar";
	public static final String ARG_CONN_USER = "username";
	public static final String ARG_CONN_PWD = "password";
	public static final String ARG_IGNORE_DROP = "ignoredroperrors";
	public static final String ARG_DISPLAY_RESULT = "displayresult";
	public static final String ARG_SUCCESS_SCRIPT = "cleanupsuccess";
	public static final String ARG_ERROR_SCRIPT = "cleanuperror";
	public static final String ARG_SHOW_TIMING = "showtiming";

	// Other parameters
	public static final String ARG_PROFILE = "profile";
	private static final String ARG_CONFIGDIR = "configdir";
	private static final String ARG_LOGFILE = "logfile";
	private static final String ARG_VARDEF = "vardef";

	private static final String ARG_SHOW_PUMPER = "datapumper";
	private static final String ARG_SHOW_DBEXP = "dbexplorer";

	private void initCmdLine(String[] args)
	{
		if (trace) System.out.println("WbManager.initCmdLine() - start");
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_PROFILE);
		cmdLine.addArgument(ARG_CONFIGDIR);
		cmdLine.addArgument(ARG_SCRIPT);
		cmdLine.addArgument(ARG_LOGFILE);
		cmdLine.addArgument(ARG_ABORT);
		cmdLine.addArgument(ARG_SUCCESS_SCRIPT);
		cmdLine.addArgument(ARG_ERROR_SCRIPT);
		cmdLine.addArgument(ARG_VARDEF);

		cmdLine.addArgument(ARG_CONN_URL);
		cmdLine.addArgument(ARG_CONN_DRIVER);
		cmdLine.addArgument(ARG_CONN_JAR);
		cmdLine.addArgument(ARG_CONN_USER);
		cmdLine.addArgument(ARG_CONN_PWD);
		cmdLine.addArgument(ARG_SHOW_PUMPER);
		cmdLine.addArgument(ARG_IGNORE_DROP);
		cmdLine.addArgument(ARG_DISPLAY_RESULT);
		cmdLine.addArgument(ARG_SHOW_DBEXP);
		cmdLine.addArgument(ARG_SHOW_TIMING);

		try
		{
			cmdLine.parse(args);
			String value = cmdLine.getValue(ARG_CONFIGDIR);
			if (value != null && value.length() > 0)
			{
				System.setProperty("workbench.configdir", value);
			}

			value = cmdLine.getValue(ARG_LOGFILE);
			if (value != null && value.length() > 0)
			{
				System.setProperty("workbench.log.filename", value);
			}

			String scriptname = cmdLine.getValue(ARG_SCRIPT);
			if (scriptname == null || scriptname.length() == 0)
			{
				this.batchMode = false;
				ConnectionMgr.getInstance().setReadTemplates(true);
			}
			else
			{
				this.batchMode = true;
				ConnectionMgr.getInstance().setReadTemplates(false);
			}

			value = cmdLine.getValue(ARG_VARDEF);
			if (value != null && value.length() > 0)
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
		}
		catch (Exception e)
		{
		}
		if (trace) System.out.println("WbManager.initCmdLine() - done");
	}

	public void init()
	{
		if (trace) System.out.println("WbManager.init() - start");
		this.settings = Settings.getInstance();
		LogMgr.logInfo("WbManager.init()", "Starting " + ResourceMgr.TXT_PRODUCT_NAME + ", " + ResourceMgr.getBuildInfo());
		LogMgr.logInfo("WbManager.init()", "Using Java version=" + System.getProperty("java.version")  + ", java.home=" + System.getProperty("java.home") + ", vendor=" + System.getProperty("java.vendor") );
		LogMgr.logDebug("WbManager.init()", "Use -Dworkbench.startuptrace=true to display trace messages during startup");
    if (this.cmdLine == null) this.initCmdLine(null);
		if (this.cmdLine.hasUnknownArguments())
		{
			LogMgr.logWarning("WbManager.init()", "Ignoring unknown argument(s) " + StringUtil.listToString(this.cmdLine.getUnknownArguments(), ','));
		}

		if (!this.batchMode)
		{
			WbSplash splash = null;
			if (wb.settings.getShowSplash())
			{
				if (trace) System.out.println("WbManager.init() - opening splash window");
				splash = new WbSplash(null, false);
				splash.setVisible(true);
			}
			if (trace) System.out.println("WbManager.init() - initializing UI defaults");
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
			if (splash != null)
			{
				if (trace) System.out.println("WbManager.init() - closing splash window");
				splash.setVisible(false);
				splash.dispose();
			}
		}
		else
		{
			int exitCode = 0;
			BatchRunner runner = BatchRunner.initFromCommandLine(cmdLine);

			if (runner != null)
			{
				try
				{
					runner.connect();
					runner.execute();
				}
				catch (Exception e)
				{
					exitCode = 1;
					LogMgr.logError("WbManager", "Could not initialize the batch runner\n", e);
				}
				finally
				{
					// disconnects everything
					ConnectionMgr mgr = ConnectionMgr.getInstance();
					mgr.disconnectAll();
				}
				if (!runner.isSuccess()) exitCode = 1;
			}

			this.doShutdown(exitCode);
		}
		if (trace) System.out.println("WbManager.init() - done.");
	}

	private void showHelp(String aFile)
	{
		try
		{
			if (this.helpWindow == null)
			{
				this.helpWindow = new HtmlViewer(this.getCurrentWindow(), aFile);
			}
			else
			{
				this.helpWindow.showIndex();
			}
			if (aFile != null)
			{
				this.helpWindow.show();
				this.helpWindow.requestFocus();
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("MainWindow", "Error when displaying HTML help", ex);
			JOptionPane.showMessageDialog(this.getCurrentWindow(), "The documentation is currently available at www.kellerer.org/workbench");
		}
	}

	public HtmlViewer getHelpViewer()
	{
		if (this.helpWindow == null) this.showHelp(null);
		return this.helpWindow;
	}

	public static void main(String args[])
	{
		wb = new WbManager();
		if (trace) System.out.println("WbManager.main() - start");
		// the command line needs to be initialized before everything
		// else, in order to set some of the system poperties correctly
		// e.g. the configdir.
		wb.initCmdLine(args);
		wb.init();
		if (trace) System.out.println("WbManager.main() - done");
	}

	/**
	 *  this is for the shutdownhook
	 */
	public void run()
	{
		LogMgr.logDebug("WbManager.run()", "Shutdownhook called!");
		Settings.getInstance().saveSettings();
	}

}
