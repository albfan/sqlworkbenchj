/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.MacroManager;
import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.util.WbCipher;
import workbench.util.WbNullCipher;


/**
 * The main application "controller" for the jWorkbench
 * @author  workbench@kellerer.org
 */
public class WbManager implements FontChangedListener
{
	private static final String CONFIG_DIR_KEY = "%ConfigDir%/";
	private static WbManager wb;
	private Settings settings;
	private ConnectionMgr connMgr = new ConnectionMgr();
	private ArrayList mainWindows = new ArrayList();
	private WbCipher desCipher = null;
	private boolean batchMode = false;
	public static boolean trace = "true".equalsIgnoreCase(System.getProperty("workbench.startuptrace", "false"));

	static
	{
		wb = new WbManager();
		wb.connMgr = new ConnectionMgr();
	}

	private WbManager()
	{
	}

	/**
	 *	Return an instance of the WbDesCipher.
	 *
	 *	This method uses Class.forName() to create a new
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

	public static Settings getSettings()
	{
		return wb.settings;
	}

	public ConnectionMgr getConnectionMgr()
	{
		return this.connMgr;
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
	public String getWorkspaceFilename(Window parent, boolean toSave)
	{
		return this.getWorkspaceFilename(parent, toSave, false);
	}

	public String getWorkspaceFilename(Window parent, boolean toSave, boolean replaceConfigDir)
	{
		String lastDir = settings.getLastWorkspaceDir();
		JFileChooser fc = new JFileChooser(lastDir);
		FileFilter wksp = ExtensionFileFilter.getWorkspaceFileFilter();
		fc.addChoosableFileFilter(wksp);
		String filename = null;

		int answer = JFileChooser.CANCEL_OPTION;
		if (toSave)
		{
			answer = fc.showSaveDialog(parent);
		}
		else
		{
			answer = fc.showOpenDialog(parent);
		}
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File fl = fc.getSelectedFile();
			FileFilter ff = fc.getFileFilter();
			if (ff == wksp)
			{
				filename = fl.getAbsolutePath();

				String ext = ExtensionFileFilter.getExtension(fl);
				if (ext.length() == 0)
				{
					if (!filename.endsWith(".")) filename = filename + ".";
					filename = filename + ExtensionFileFilter.WORKSPACE_EXT;
				}
			}
			else
			{
				filename = fl.getAbsolutePath();
			}

			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			settings.setLastWorkspaceDir(lastDir);
		}
		if (replaceConfigDir && filename != null)
		{
			filename = this.putConfigDirKey(filename);
		}
		return filename;
	}

	public String putConfigDirKey(String aPathname)
	{
		File f = new File(aPathname);
		String fname = f.getName();
		File dir = f.getParentFile();
		File config = new File(this.settings.getConfigDir());
		if (dir.equals(config))
		{
			return CONFIG_DIR_KEY + fname;
		}
		else
		{
			return aPathname;
		}
	}
	
	public String replaceConfigDir(String aPathname)
	{
		if (aPathname == null) return null;
		return StringUtil.replace(aPathname, CONFIG_DIR_KEY, this.settings.getConfigDir());
	}
	
	public String getExportFilename(boolean includeSqlType)
	{
		return this.getExportFilename(null, includeSqlType);
	}

	public String getExportFilename(Component caller, boolean includeSqlType)
	{
		String lastDir = settings.getLastExportDir();
		JFileChooser fc = new JFileChooser(lastDir);
		FileFilter text = ExtensionFileFilter.getTextFileFilter();
		fc.addChoosableFileFilter(text);
		fc.addChoosableFileFilter(ExtensionFileFilter.getHtmlFileFilter());
		if (includeSqlType)
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		}
		fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
		
		fc.setFileFilter(text);
		String filename = null;

		Window parent;
		parent = SwingUtilities.getWindowAncestor(caller);

		int answer = fc.showSaveDialog(parent);
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File fl = fc.getSelectedFile();
			FileFilter ff = fc.getFileFilter();
			if (ff instanceof ExtensionFileFilter)
			{
				ExtensionFileFilter eff = (ExtensionFileFilter)ff;
				filename = fl.getAbsolutePath();

				String ext = ExtensionFileFilter.getExtension(fl);
				if (ext.length() == 0)
				{
					if (!filename.endsWith(".")) filename = filename + ".";
					filename = filename + eff.getDefaultExtension();
				}
			}
			else
			{
				filename = fl.getAbsolutePath();
			}

			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			settings.setLastExportDir(lastDir);
		}

		return filename;
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
		}
		catch (Exception e)
		{
			LogMgr.logInfo("Settings.setLookAndFeel()", "Could not set look and feel", e);
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

		LookAndFeel lnf = UIManager.getLookAndFeel();
		String lnfClass = lnf.getClass().getName();

		if (lnfClass.equals("javax.swing.plaf.metal.MetalLookAndFeel") ||
		    lnfClass.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel") ||
		    lnfClass.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
		{
			def.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
			def.put("SplitPaneUI", "com.sun.java.swing.plaf.windows.WindowsSplitPaneUI");
		}

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

	public void showErrorMessage(Component aCaller, String aMsg)
	{
		WbSwingUtilities.showErrorMessage(aCaller, aMsg);
	}

	private JDialog closeMessage;
	
	public void exitWorkbench()
	{
		//boolean first = true;
		if (!this.batchMode)
		{
			MainWindow w = this.getCurrentWindow();

			if (w.isBusy())
			{
				if (!this.checkAbort(w)) return;
			}
			if (!this.checkProfiles(w)) return;
			w.saveSettings();
			w.saveWorkspace();
			
			
			// When disconnecting it can happen that the disconnect itself
			// takes several minutes. Because of this, a small window is displayed
			// that the disconnect takes place, and the actual disconnect is
			// carried out in a different thread to not block the AWT thread.
			
			// If it takes too long the user can still abort the JVM ...
			this.createCloseMessageWindow(w);
			this.closeMessage.show();

			this.settings.saveSettings();
			MacroManager.getInstance().saveMacros();
			Thread t = new Thread()
			{
				public void run()
				{
					disconnectWindows();
					getConnectionMgr().disconnectAll();
					disconnected();
				}
			};
			t.setName("WbManager exit disconnect thread");
			t.setDaemon(false);
			t.start();
		}
		else
		{
			getConnectionMgr().disconnectAll();
			doShutdown();
		}
	}

	private void createCloseMessageWindow(MainWindow parent)
	{
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
		p2.setLayout(new FlowLayout(FlowLayout.CENTER));
		p2.add(b);
		p.add(p2, BorderLayout.SOUTH);
		this.closeMessage.getContentPane().setLayout(new BorderLayout());
		this.closeMessage.getContentPane().add(p, BorderLayout.CENTER);
		this.closeMessage.setUndecorated(true);
		this.closeMessage.setSize(200,70);
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
			w.doDisconnect();
		}
	}
	
	/**
	 *	This gets called from the thread that disconnects everything
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
		for (int i=0; i < mainWindows.size(); i ++)
		{
			MainWindow w = (MainWindow)this.mainWindows.get(i);
			if (w != null)
			{
				this.mainWindows.remove(w);
				w.setVisible(false);
				w.dispose();
			}
		}
	}
	
	private void doShutdown()
	{
		LogMgr.shutdown();
		System.exit(0);
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
    if (getConnectionMgr().profilesChanged())
    {
      int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedProfiles"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION)
      {
        this.getConnectionMgr().saveProfiles();
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
			this.exitWorkbench();
    }
		else
		{
			this.mainWindows.remove(win);
			Thread t = new Thread()
			{
				public void run()
				{
					// no need to do the disconnect on the window in its
					// own thread as we are already in a background thread
					win.disconnect(false);
					win.hide();
					win.dispose();
				}
			};
			t.setName("WbManager window disconnect thread");
			t.setDaemon(true);
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

		/*
		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(ARG_PROFILE);
			if (profilename != null && profilename.trim().length() > 0)
			{
				ConnectionProfile prof = connMgr.getProfile(profilename);
				if (prof != null)
				{
					LogMgr.logDebug("WbManager.openNewWindow()", "Connecting to " + prof.getName());
					// try to connect to the profile passed on the
					// command line. If this fails the connection
					// dialog will be show to the user
					connected = main.connectTo(prof);
				}
			}
		}
		*/
		// no connection, display the connection dialog
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
	private static final String ARG_PROFILE = "profile";
	private static final String ARG_CONFIGDIR = "configdir";
	private static final String ARG_SCRIPT = "script";
	private static final String ARG_LOGFILE = "logfile";
	private static final String ARG_ABORT = "abortonerror";
	private static final String ARG_SUCCESS_SCRIPT = "cleanupsuccess";
	private static final String ARG_ERROR_SCRIPT = "cleanuperror";

	private static final String ARG_CONN_URL = "url";
	private static final String ARG_CONN_DRIVER = "driver";
	private static final String ARG_CONN_JAR = "driverjar";
	private static final String ARG_CONN_USER = "username";
	private static final String ARG_CONN_PWD = "password";
	
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
		
		cmdLine.addArgument(ARG_CONN_URL);
		cmdLine.addArgument(ARG_CONN_DRIVER);
		cmdLine.addArgument(ARG_CONN_JAR);
		cmdLine.addArgument(ARG_CONN_USER);
		cmdLine.addArgument(ARG_CONN_PWD);

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
				this.connMgr.setReadTemplates(true);
			}
			else
			{
				this.batchMode = true;
				this.connMgr.setReadTemplates(false);
				LogMgr.logInfo("WbManager", "Executing script " + scriptname);
			}

		}
		catch (Exception e)
		{
		}
		if (trace) System.out.println("WbManager.initCmdLine() - done");
	}

	public void initSettings()
	{
		this.settings = new Settings();
	}
	
	public void init()
	{
		if (trace) System.out.println("WbManager.init() - start");
		this.initSettings();
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
			this.openNewWindow(true);
			if (splash != null)
			{
				if (trace) System.out.println("WbManager.init() - closing splash window");
				splash.setVisible(false);
				splash.dispose();
			}
		}
		else
		{
			String scripts = cmdLine.getValue(ARG_SCRIPT);
			String profilename = cmdLine.getValue(ARG_PROFILE);
			String errorHandling = cmdLine.getValue(ARG_ABORT);
			boolean abort = true;
			if (errorHandling != null)
			{
				abort = StringUtil.stringToBool(errorHandling);
			}

			ConnectionProfile profile = null;
			if (profilename == null)
			{
				String url = StringUtil.trimQuotes(cmdLine.getValue(ARG_CONN_URL));
				String driver = StringUtil.trimQuotes(cmdLine.getValue(ARG_CONN_DRIVER));
				String user = StringUtil.trimQuotes(cmdLine.getValue(ARG_CONN_USER));
				String pwd = StringUtil.trimQuotes(cmdLine.getValue(ARG_CONN_PWD));
				String jar = StringUtil.trimQuotes(cmdLine.getValue(ARG_CONN_JAR));
				DbDriver drv = this.connMgr.findRegisteredDriver(driver);
				if (drv == null)
				{
					this.connMgr.registerDriver(driver, jar);
				}
				profile = new ConnectionProfile(driver, url, user, pwd);
			}
			else
			{
				profile = this.connMgr.getProfile(StringUtil.trimQuotes(profilename));
			}
			
			String success = cmdLine.getValue(ARG_SUCCESS_SCRIPT);
			String error = cmdLine.getValue(ARG_ERROR_SCRIPT);
			if (scripts != null && profile != null)
			{
				if (trace) System.out.println("WbManager.init() - initializing BatchRunner");
				BatchRunner runner = new BatchRunner(scripts);
				try
				{
					runner.setAbortOnError(abort);
					runner.setErrorScript(error);
					runner.setSuccessScript(success);
					// set profile will connect to the database using the
					// connection manager. It will throw an exception
					// if the connection fails.
					if (trace) System.out.println("WbManager.init() - connecting BatchRunner");
					runner.setProfile(profile);
					if (trace) System.out.println("WbManager.init() - starting BatchRunner");
					runner.execute();
				}
				catch (Exception e)
				{
					LogMgr.logError("WbManager", "Could not initialize the batch runner\n", e);
				}
				finally
				{
					runner.done();
				}
			}
		}
		if (trace) System.out.println("WbManager.init() - done.");
	}

	public static void main(String args[])
	{
		if (trace) System.out.println("WbManager.main() - start");
		wb.initCmdLine(args);
		wb.init();
		if (trace) System.out.println("WbManager.main() - done");
	}
}