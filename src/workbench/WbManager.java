/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import workbench.db.ConnectionMgr;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.FontChangedListener;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
	private Settings settings = new Settings();
	private ConnectionMgr connMgr = new ConnectionMgr();
	private ArrayList mainWindows = new ArrayList();
	private WbCipher desCipher = null;
	
	private WbManager() 
	{
		this.setLookAndFeel();
		this.initFonts();
		this.settings.addFontChangedListener(this);
	}

	public WbCipher getDesCipher()
	{
		System.out.println("WbManager.getDesCipher()");
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
		if (includeSqlType)
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		}
		fc.addChoosableFileFilter(ExtensionFileFilter.getTextFileFilter());
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
			System.out.println("Could not set look and feel");
		}
		
		try
		{
			Toolkit.getDefaultToolkit().setDynamicLayout(settings.getUseDynamicLayout());
		}
		catch (Exception e)
		{
		}
	}
	
	private void initFonts() 
	{
		Font stdFont = this.settings.getStandardFont();
		UIManager.put("Button.font", stdFont);
		UIManager.put("CheckBox.font", stdFont);
		UIManager.put("CheckBoxMenuItem.font", stdFont);
		UIManager.put("ColorChooser.font", stdFont);
		UIManager.put("ComboBox.font", stdFont);
		UIManager.put("EditorPane.font", stdFont);
		UIManager.put("FileChooser.font", stdFont);
		UIManager.put("InternalFrame.font", stdFont);
		UIManager.put("Label.font", stdFont);
		UIManager.put("List.font", stdFont);
		UIManager.put("Menu.font", stdFont);
		UIManager.put("MenuItem.font", stdFont);
		UIManager.put("OptionPane.font", stdFont);
		UIManager.put("Panel.font", stdFont);
		UIManager.put("PasswordField.font", stdFont);
		UIManager.put("PopupMenu.font", stdFont);
		UIManager.put("ProgressBar.font", stdFont);
		UIManager.put("RadioButton.font", stdFont);
		UIManager.put("TabbedPane.font", stdFont);
		UIManager.put("TextArea.font", stdFont);
		UIManager.put("TextField.font", stdFont);
		UIManager.put("TextPane.font", stdFont);
		UIManager.put("TitledBorder.font", stdFont);
		UIManager.put("ToggleButton.font", stdFont);
		UIManager.put("ToolBar.font", stdFont);
		UIManager.put("ToolTip.font", stdFont);
		UIManager.put("Tree.font", stdFont);
		UIManager.put("ViewPort.font", stdFont);
		UIManager.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
		
		Font dataFont = this.settings.getDataFont();
		UIManager.put("Table.font", dataFont);
		UIManager.put("TableHeader.font", dataFont);
		if (settings.getShowMnemonics())
			UIManager.put("Button.showMnemonics", Boolean.TRUE);
		else
			UIManager.put("Button.showMnemonics", Boolean.FALSE);
	}

	public MainWindow createWindow()
	{
		MainWindow win = new MainWindow();
		this.mainWindows.add(win);
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
			if (w.isFocused()) 
			{
				w.saveSettings();
			}
			w.setVisible(false);
			w.dispose();
		}
		this.settings.saveSettings();
		System.exit(0);
	}
	
	public void windowClosing(MainWindow win)
	{
		if (win != null)
		{
			this.mainWindows.remove(win);
		}
		if (this.mainWindows.size() == 0)
    {
      if (getConnectionMgr().profilesChanged())
      {
        int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedProfiles"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.OK_OPTION)
        {
          this.getConnectionMgr().saveProfiles();
        }
      }
			this.exitWorkbench();
    }
	}
	
	public void openNewWindow()
	{
		MainWindow main = this.createWindow();
		main.show();
		main.restoreState();
		main.selectConnection();
	}
	
	
	public static void startup()
	{
		WbSplash splash = null;
		if (wb.settings.getShowSplash())
		{
			splash = new WbSplash(null, false);
			splash.setVisible(true);
		}

		wb.openNewWindow();
		if (splash != null)
		{
			splash.setVisible(false);
			splash.dispose();
		}
	}

	public static void main(String args[])
	{
		startup();
	}
}
