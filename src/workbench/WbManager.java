/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.util.CmdLineParser;
import workbench.util.StringUtil;
import workbench.util.WbCipher;
import workbench.util.WbNullCipher;
import workbench.util.WbPersistence;

/**
 * The main application "controller" for the jWorkbench
 * @author  workbench@kellerer.org
 */
public class WbManager
	implements FontChangedListener
{
	private static WbManager wb = new WbManager();
	private Settings settings;
	private ConnectionMgr connMgr = new ConnectionMgr();
	private ArrayList mainWindows = new ArrayList();
	private WbCipher desCipher = null;
	private boolean batchMode = false;
	public static boolean trace = "true".equalsIgnoreCase(System.getProperty("workbench.startuptrace", "false"));
	
	private WbManager() 
	{
	}

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
		fc.setFileFilter(text);
		String filename = null;

		Window parent;
		parent = SwingUtilities.getWindowAncestor(caller);
		
		int answer = fc.showSaveDialog(parent);
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File fl = fc.getSelectedFile();
			FileFilter ff = fc.getFileFilter();
			if (ff == ExtensionFileFilter.getSqlFileFilter())
			{
				filename = fl.getAbsolutePath();

				String ext = ExtensionFileFilter.getExtension(fl);
				if (ext.length() == 0)
				{
					if (!filename.endsWith(".")) filename = filename + ".";
					filename = filename + "sql";
				}
			}
			else if (ff == ExtensionFileFilter.getHtmlFileFilter())
			{
				filename = fl.getAbsolutePath();

				String ext = ExtensionFileFilter.getExtension(fl);
				if (ext.length() == 0)
				{
					if (!filename.endsWith(".")) filename = filename + ".";
					filename = filename + "html";
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
		if (trace) System.err.println("WbManager.setLookAndFeel() - start");
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
		if (trace) System.err.println("WbManager.setLookAndFeel() - done");
	}
	
	private void initUI() 
	{
		if (trace) System.err.println("WbManager.initUI() - start");
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

		def.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
		def.put("SplitPaneUI", "com.sun.java.swing.plaf.windows.WindowsSplitPaneUI");
		
		if (settings.getShowMnemonics())
			def.put("Button.showMnemonics", Boolean.TRUE);
		else
			def.put("Button.showMnemonics", Boolean.FALSE);
		
		this.settings.addFontChangedListener(this);
		if (trace) System.err.println("WbManager.initUI() - done");
	}

	public MainWindow createWindow()
	{
		if (trace) System.err.println("WbManager.createWindow() - start");
		MainWindow win = new MainWindow();
		this.mainWindows.add(win);
		if (trace) System.err.println("WbManager.createWindow() - done");
		return win;
	}
	
	public void showErrorMessage(Component aCaller, String aMsg)
	{
		Window w = SwingUtilities.getWindowAncestor(aCaller);
		WbSwingUtilities.showErrorMessage(aCaller, aMsg);
	}
	
	public void exitWorkbench()
	{
		this.getConnectionMgr().disconnectAll();
		boolean first = true;
		MainWindow w;
		for (int i=0; i < mainWindows.size(); i ++)
		{
			w = (MainWindow)this.mainWindows.get(i);
			if (w == null) continue;
      // If there are multiple Windows open, we only save the 
      // settings for the currently active window
			if (w.isFocused()) 
			{
        if (!this.checkProfiles(w)) return;
				w.saveSettings();
			}
      this.mainWindows.remove(w);
			w.setVisible(false);
			w.dispose();
		}
		this.settings.saveSettings();
		LogMgr.shutdown();
		System.exit(0);
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
	public void windowClosing(MainWindow win)
	{
		if (this.mainWindows.size() == 1)
    {
			this.exitWorkbench();
    }
	}
	
	public void openNewWindow()
	{
		this.openNewWindow(false);
	}
	
	public void openNewWindow(boolean checkCmdLine)
	{
		if (trace) System.err.println("WbManager.openNewWindow()");
		final MainWindow main = this.createWindow();
		
		main.show();
		main.restoreState();
		boolean connected = false;
		
		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = (String)cmdLine.getOptionValue(profileNameOption);
			if (profilename != null && profilename.trim().length() > 0)
			{
				ConnectionProfile prof = connMgr.getProfile(profilename);
				if (prof != null)
				{
					connected = main.connectTo(prof);
				}
			}
		}
		
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
	
	private CmdLineParser cmdLine;
	private CmdLineParser.Option profileNameOption;
	private CmdLineParser.Option configDirOption;
	private CmdLineParser.Option scriptOption;
	private CmdLineParser.Option logFileOption;
	
	private void initCmdLine(String[] args)
	{
		if (trace) System.err.println("WbManager.initCmdLine()");
		cmdLine = new CmdLineParser();
		profileNameOption = cmdLine.addStringOption('p', "profile");
		configDirOption = cmdLine.addStringOption('c', "configdir");
		scriptOption = cmdLine.addStringOption('s', "script");
		logFileOption = cmdLine.addStringOption('l', "logfile");
		
		try
		{
			cmdLine.parse(args);
			String value = (String)cmdLine.getOptionValue(configDirOption);
			if (value != null && value.length() > 0)
			{
				System.setProperty("workbench.configdir", value);
			}
			
			value = (String)cmdLine.getOptionValue(logFileOption);
			if (value != null && value.length() > 0)
			{
				System.setProperty("workbench.log.filename", value);
			}
			
			String scriptname = this.getScriptFile();
			if (scriptname == null || scriptname.length() == 0)
			{
				this.batchMode = false;
			}
			else
			{
				this.batchMode = true;
			}

		}
		catch (Exception e)
		{
		}
	}

	public String getScriptFile()
	{
		String scriptname = (String)cmdLine.getOptionValue(scriptOption);
		return scriptname;
	}
	
	private void init()
	{
		if (trace) System.err.println("WbManager.init()");
		this.settings = new Settings();
		if (!this.batchMode)
		{
			WbSplash splash = null;
			if (wb.settings.getShowSplash())
			{
				splash = new WbSplash(null, false);
				splash.setVisible(true);
			}
			this.initUI();
			this.openNewWindow(true);
			if (splash != null)
			{
				splash.setVisible(false);
				splash.dispose();
			}
		}
		else
		{
			BatchRunner runner = new BatchRunner(this.getScriptFile());
			String profilename = (String)cmdLine.getOptionValue(profileNameOption);
			try
			{
				runner.setProfile(profilename);
			}
			catch (Exception e)
			{
				LogMgr.logError("WbManager", "Could not initialize the batch runner", e);
			}
		}
	}

	public static void main(String args[])
	{
		if (trace) System.err.println("WbManager.main() - start");
		wb.initCmdLine(args);
		wb.init();
		if (trace) System.err.println("WbManager.main() - done");
	}
}
