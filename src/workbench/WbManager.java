/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.util.HashMap;
import java.awt.Font;
import java.sql.Connection;
import java.util.Iterator;
import javax.swing.UIManager;

import workbench.gui.display.MainWindow;
import workbench.db.ConnectionMgr;
import workbench.resource.Settings;
import java.util.List;
import workbench.db.ConnectionProfile;
import workbench.util.WbCipher;

/**
 *	The main application "controller" for the jWorkbench
 * @author  thomas
 * @version
 */
public class WbManager
{
	private static WbManager wb = new WbManager();
	private Settings settings = new Settings();
	private ConnectionMgr connMgr = new ConnectionMgr();
	private HashMap windowList = new HashMap();
	private WbCipher cipher = null;
	
	private WbManager() 
	{
		this.initFonts();
		this.cipher = new WbCipher();
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
	
	private void initFonts() 
	{
		long end;
		long start = System.currentTimeMillis();
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
		UIManager.put("Table.font", stdFont);
		UIManager.put("TableHeader.font", stdFont);
		UIManager.put("TextArea.font", stdFont);
		UIManager.put("TextField.font", stdFont);
		UIManager.put("TextPane.font", stdFont);
		UIManager.put("TitledBorder.font", stdFont);
		UIManager.put("ToggleButton.font", stdFont);
		UIManager.put("ToolBar.font", stdFont);
		UIManager.put("ToolTip.font", stdFont);
		UIManager.put("Tree.font", stdFont);
		UIManager.put("ViewPort.font", stdFont);
		end = System.currentTimeMillis();
		//System.out.println("initFonts=" + (end - start));
	}

	public MainWindow createWindow()
	{
		int count = this.windowList.size() + 1;
		String key = "jWbWindow_" + count;
		MainWindow result = new MainWindow(key);
		this.windowList.put(key, result);
		return result;
	}
	
	public void exitWorkbench()
	{
		this.getConnectionMgr().disconnectAll();
		Iterator values = this.windowList.values().iterator();
		while (values.hasNext())
		{
			MainWindow win = (MainWindow)values.next();
			win.saveSettings();
			win.dispose();
		}
		//this.connMgr.writeSettings();
		this.settings.saveSettings();
		System.exit(0);
	}
	
	public static void main(String args[])
	{
		// init() may not be called in the constructor
		// because the creation of the settings and
		// connectionMgr object rely on an existing
		// wb instance which is not the case during
		// the constructor
		MainWindow main = wb.createWindow();
		main.show();
		main.selectConnection();
		/*
		Connection conn = null;
		ConnectionProfile profile = wb.connMgr.selectConnection();
		try
		{
			main.connectTo(profile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		*/
	}
	
}
