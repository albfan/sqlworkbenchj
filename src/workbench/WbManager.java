/*
 * WbManager.java
 *
 * Created on November 25, 2001, 3:48 PM
 */

package workbench;

import java.util.HashMap;
import java.awt.Font;
import javax.swing.UIManager;

import workbench.gui.display.MainWindow;
import workbench.db.ConnectionMgr;
import workbench.resource.Settings;

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
	
	private WbManager() {}
	
	public static WbManager getInstance()
	{
		return wb;
	}
	
	private void init()
	{
		this.initFonts();
	}
	
	private void initGui()
	{
	}

	public Settings getSettings()
	{
		return this.settings;
	}
	
	public ConnectionMgr getConnectionMgr()
	{
		return this.connMgr;
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
	
	public static void main(String args[])
	{
		WbManager mgr = WbManager.getInstance();
		mgr.init();
		
		MainWindow main = mgr.createWindow();
		main.show();
	}
	
}
