/*
 * MainWindow.java
 *
 * Created on November 25, 2001, 3:10 PM
 */

package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbToolbar;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 *
 * @author  sql.workbench@freenet.de
 * @version
 */
public class MainWindow extends JFrame implements ActionListener, WindowListener, ChangeListener
{
	private String windowId;

	//private JToolBar currentToolbar;
	private JMenuBar currentMenu;
	private FileConnectAction connectAction;
	
	//private WindowStatusBar statusBar;
	private ProfileSelectionDialog profileDialog;
	private JTabbedPane sqlTab = new JTabbedPane();
	private WbToolbar currentToolbar;
	private List sqlPanels = new ArrayList(10);
	private List panelMenus = new ArrayList(10);
	private int tabCount = 0;

	/** Creates new MainWindow */
	public MainWindow(String id)
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		this.windowId = id;
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(this);
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.tabCount = WbManager.getSettings().getDefaultTabCount();
		if (tabCount <= 0) tabCount = 1;

		for (int i=0; i < tabCount; i++)
		{
			SqlPanel sql = new SqlPanel(i + 1);
			this.sqlPanels.add(sql);
			this.sqlTab.addTab(ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(i+1), sql);
		}
		this.initMenu();
		
		this.getContentPane().add(this.sqlTab, BorderLayout.CENTER);
		this.setTitle(ResourceMgr.getString("MsgNotConnected"));
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.restorePosition();
		this.setIconImage(ResourceMgr.getPicture("workbench16").getImage());

		int lastIndex = WbManager.getSettings().getLastSqlTab();
		if (lastIndex < 0 || lastIndex > this.sqlTab.getTabCount())
		{
			lastIndex = 0;
		}
		this.sqlTab.setSelectedIndex(lastIndex);
		this.tabSelected(lastIndex);
		
		// now that we have setup the SplitPane we can add the
		// change listener
		this.sqlTab.addChangeListener(this);
	}

	private void initMenu()
	{
		for (int tab=0; tab < this.tabCount; tab ++)
		{
			JMenuBar menuBar = new JMenuBar();
			menuBar.setBorderPainted(false);

			// Create the file menu for all tabs
			JMenu menu = new JMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_FILE));
			WbAction action;
			JMenuItem item;

			this.connectAction = new FileConnectAction(this);
			item = this.connectAction.getMenuItem();
			menu.add(item);
			menu.addSeparator();
			
			action = new FileExitAction();
			menu.add(action.getMenuItem());
			menuBar.add(menu);
			SqlPanel sql = (SqlPanel)this.sqlPanels.get(tab);
			sql.addToActionMap(this.connectAction);
			
			// now create the menus for the current tab
			List actions = sql.getActions();

			HashMap menus = new HashMap(10);
			
			// Create the menus in the correct order
			menu = new JMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_EDIT));
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_EDIT, menu);

			menu = new JMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_VIEW));
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_VIEW, menu);
			
			menu = new JMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_DATA));
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_DATA, menu);
			
			menu = new JMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_SQL));
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_SQL, menu);
			
			for (int i=0; i < actions.size(); i++)
			{
				action = (WbAction)actions.get(i);
				String menuName = (String)action.getValue(WbAction.MAIN_MENU_ITEM);
				if (menuName == null)
				{
					LogMgr.logWarning(this, "Action " + action.getClass() + " does not define a main menu entry!");
					continue;
				}
				menu = (JMenu)menus.get(menuName);
				if (menu == null)
				{
					menu = new JMenu(ResourceMgr.getString(menuName));
					menuBar.add(menu);
					menus.put(menuName, menu);
				}
				boolean menuSep = "true".equals((String)action.getValue(WbAction.MENU_SEPARATOR));

				if (menuSep)
				{
					menu.addSeparator();
				}
				//menu.add(action.getMenuItem());
				action.addToMenu(menu);
			}
			
			// now put the tabs into the view menu
			menu = (JMenu)menus.get(ResourceMgr.MNU_TXT_VIEW);
			InputMap im = new ComponentInputMap(this.sqlTab);
			ActionMap am = new ActionMap();
			this.sqlTab.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, im);
			this.sqlTab.setActionMap(am);
			for (int i=0; i < this.tabCount; i ++)
			{
				action = new SelectTabAction(this.sqlTab, i);
				KeyStroke key = action.getAccelerator();
				if (key != null)
				{
					im.put(key, action.getActionName());
					am.put(action.getActionName(), action);
				}
				menu.add(action);
			}
			
			menuBar.add(this.buildToolsMenu());
			menuBar.add(this.buildHelpMenu());
			this.panelMenus.add(menuBar);
		}
	}

	private SqlPanel getCurrentSqlPanel()
	{
		int index = this.sqlTab.getSelectedIndex();
		return (SqlPanel)this.sqlPanels.get(index);
	}

	private void tabSelected(int anIndex)
	{
		this.currentMenu = (JMenuBar)this.panelMenus.get(anIndex);
		this.setJMenuBar(currentMenu);
		Container content = this.getContentPane();
		if (this.currentToolbar != null) content.remove(this.currentToolbar);
		this.currentToolbar = this.getCurrentSqlPanel().getToolbar();
		content.add(this.currentToolbar, BorderLayout.NORTH);
		this.doLayout();
	}

	public void restorePosition()
	{
		Settings s = WbManager.getSettings();

		if (!s.restoreWindowSize(this))
		{
			this.setSize(500,500);
		}

		if (!s.restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, null);
		}
	}

	public void saveSettings()
	{
		int index = this.sqlTab.getSelectedIndex();
		WbManager.getSettings().setLastSqlTab(index);
		for (int i=0; i < this.tabCount; i++)
		{
			SqlPanel sql = (SqlPanel)this.sqlPanels.get(i);
			sql.storeSettings();
		}
		WbManager.getSettings().storeWindowPosition(this);
		WbManager.getSettings().storeWindowSize(this);
	}

	public String getWindowId()
	{
		return this.windowId;
	}

	public void windowOpened(WindowEvent windowEvent)
	{
	}

	public void windowClosed(WindowEvent e)
	{
		WbManager.getInstance().exitWorkbench();
	}

	public void windowDeiconified(WindowEvent windowEvent)
	{
	}

	public void windowClosing(WindowEvent windowEvent)
	{
		this.saveSettings();
	}

	public void windowDeactivated(WindowEvent windowEvent)
	{
	}

	public void windowActivated(WindowEvent windowEvent)
	{
	}

	public void windowIconified(WindowEvent windowEvent)
	{
	}

	public void showStatusMessage(String aMsg)
	{
		this.getCurrentSqlPanel().showStatusMessage(aMsg);
	}

	public void showLogMessage(String aMsg)
	{
		this.getCurrentSqlPanel().showLogMessage(aMsg);
	}

	public void setConnection(WbConnection con)
	{
		for (int i=0; i < this.tabCount; i++)
		{
			SqlPanel sql = (SqlPanel)this.sqlPanels.get(i);
			sql.setConnection(con);
		}
		//this.setTitle(ResourceMgr.TXT_PRODUCT_NAME + " [" + ConnectionMgr.getDisplayString(con) + "]");
	}

	public void selectConnection()
	{
		if (this.profileDialog == null)
		{
			this.profileDialog = new ProfileSelectionDialog(this, true);
		}
		WbSwingUtilities.center(this.profileDialog, this);
		this.profileDialog.setVisible(true);
		if (!this.profileDialog.isCancelled())
		{
			ConnectionProfile prof = this.profileDialog.getSelectedProfile();
			if (prof != null)
			{
				String n = prof.getName();
				this.connectTo(prof);
				WbManager.getSettings().setLastConnection(prof.getName());
			}
		}
	}

	public void connectTo(ConnectionProfile aProfile)
	{
		try
		{
			this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			this.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
			try
			{
				ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
				WbConnection conn = mgr.getConnection(aProfile);
				this.setConnection(conn);
				this.setTitle(ResourceMgr.TXT_PRODUCT_NAME + " [" + aProfile.getName() + "]");
			}
			catch (ClassNotFoundException cnf)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_DRIVER_NOT_FOUND));
			}
			catch (SQLException se)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + se.toString());
			}
			this.showStatusMessage(null);
		}
		catch (Exception e)
		{
			this.showLogMessage("Could not connect\r\n" + e.getMessage());
		}
		this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public JMenu buildHelpMenu()
	{
		JMenu result = new JMenu(ResourceMgr.getString("MnuTxtHelp"));
		JMenuItem item = new JMenuItem(ResourceMgr.getString("MnuTxtHelpContents"));
		item.putClientProperty("command", "helpContents");
		item.addActionListener(this);
		result.add(item);
		
		item = new JMenuItem(ResourceMgr.getString("MnuTxtAbout"));
		item.putClientProperty("command", "helpAbout");
		item.addActionListener(this);
		result.add(item);
		return result;
	}
	
	public JMenu buildToolsMenu()
	{
		JMenu result = new JMenu(ResourceMgr.getString("MnuTxtTools"));
		JMenuItem options = new JMenuItem(ResourceMgr.getString("MnuTxtOptions"));
		options.putClientProperty("command", "optionsDialog");
		options.addActionListener(this);
		result.add(options);
		JMenu lnf = new JMenu(ResourceMgr.getString("MnuTxtLookAndFeel"));
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		for (int i=0; i < info.length; i++)
		{
			JMenuItem item = new JMenuItem(info[i].getName());
			item.putClientProperty("command", "lnf");
			item.putClientProperty("class", info[i].getClassName());
			item.addActionListener(this);
			lnf.add(item);
		}
		result.add(lnf);
		return result;
	}

	/** 
	 *	Invoked when the a different SQL panel has been selected
	 *
	 * @param e  a ChangeEvent object
	 *
	 */
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.sqlTab)
		{
			int index = this.sqlTab.getSelectedIndex();
			this.tabSelected(index);
		}
	}


	/** 
	 *	Invoked when any of the main window menu commands are 
	 *
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object sender = e.getSource();
		if (sender instanceof JMenuItem)
		{
			JMenuItem item = (JMenuItem)sender;
			String command = (String)item.getClientProperty("command");
			if ("lnf".equals(command))
			{
				String className = (String)item.getClientProperty("class");
				try
				{
					UIManager.setLookAndFeel(className);
					SwingUtilities.updateComponentTreeUI(this);
					WbManager.getSettings().setLookAndFeelClass(className);
				}
				catch (Exception ex)
				{
					LogMgr.logError(this, "Could not change look and feel", ex);
				}
			}
			else if ("optionsDialog".equals(command))
			{
				JOptionPane.showMessageDialog(this, "Not yet implemented. Please edit workbench.settings");
			}
			else if ("helpContents".equals(command))
			{
				JOptionPane.showMessageDialog(this, "Sorry! Help is not yet available");
			}
			else if ("helpAbout".equals(command))
			{
				WbAboutDialog about = new WbAboutDialog(this, true);
				WbSwingUtilities.center(about, this);
				about.show();
			}
		}
	}

}
