/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import workbench.db.ConnectionMgr;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.FontChangedListener;
import workbench.resource.Settings;
import workbench.util.WbCipher;
import workbench.util.WbPersistence;

/**
 *	The main application "controller" for the jWorkbench
 * @author  thomas
 * @version
 */
public class WbManager
	implements FontChangedListener
{
	private static WbManager wb = new WbManager();
	private Settings settings = new Settings();
	private ConnectionMgr connMgr = new ConnectionMgr();
	private MainWindow mainWindow;
	private WbCipher cipher = null;
	
	private WbManager() 
	{
		this.setLookAndFeel();
		this.initFonts();
		this.cipher = new WbCipher();
		this.settings.addFontChangedListener(this);
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
	
	public WbCipher getCipher()
	{
		return this.cipher;
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
		if (caller == null)
			parent = this.mainWindow;
		else
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
		//UIManager.put("Button.showMnemonics", Boolean.TRUE);
	}

	public MainWindow createWindow()
	{
		this.mainWindow = new MainWindow();
		return this.mainWindow;
	}
	
	public void showErrorMessage(String aMsg)
	{
		WbSwingUtilities.showErrorMessage(this.mainWindow, aMsg);
	}
	public void exitWorkbench()
	{
		this.getConnectionMgr().disconnectAll();
		this.mainWindow.saveSettings();
		this.mainWindow.dispose();
		this.settings.saveSettings();
		System.exit(0);
	}
	
	public static void startup()
	{
		MainWindow main = wb.createWindow();
		main.show();
		main.selectConnection();
	}

	public static void main(String args[])
	{
		startup();
	}
}
