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
import java.util.List;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbToolbar;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;



/**
 *
 * @author  thomas
 * @version
 */
public class MainWindow extends JFrame implements ActionListener, WindowListener, ChangeListener
{
	private String windowId;

	private JToolBar currentToolbar;
	private JMenuBar currentMenu;
	private FileConnectAction connectAction;
	
	//private WindowStatusBar statusBar;
	private ProfileSelectionDialog profileDialog;
	private JTabbedPane sqlTab = new JTabbedPane();
	private List sqlPanels = new ArrayList(10);
	private List panelMenus = new ArrayList(10);
	private List panelToolbars = new ArrayList(10);
	private int tabCount = 0;

	/** Creates new MainWindow */
	public MainWindow(String id)
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		this.windowId = id;
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(this);
		this.sqlTab.setBorder(null);

		this.tabCount = WbManager.getSettings().getDefaultTabCount();
		if (tabCount <= 0) tabCount = 1;

		for (int i=0; i < tabCount; i++)
		{
			SqlPanel sql = new SqlPanel(i + 1);
			this.sqlPanels.add(sql);
			this.sqlTab.addTab(ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(i+1), sql);
		}
		long start, end;
		start = System.currentTimeMillis();
		this.initMenu();
		end = System.currentTimeMillis();
		//System.out.println("initMenu()=" + (end - start));

		this.getContentPane().add(this.sqlTab, BorderLayout.CENTER);
		this.getContentPane().add(this.currentToolbar, BorderLayout.NORTH);
		this.setTitle(ResourceMgr.getString("MsgNotConnected"));
		this.sqlTab.addChangeListener(this);
		this.sqlTab.setBorder(null);
		this.restorePosition();
	}

	private void initMenu()
	{
		for (int tab=0; tab < this.tabCount; tab ++)
		{
			JToolBar toolbar = new WbToolbar();
			JMenuBar menuBar = new JMenuBar();
			menuBar.setBorderPainted(false);
			//menuBar.setBorder(new DividerBorder(DividerBorder.BOTTOM));

			//toolbar.setBorder(new DividerBorder(DividerBorder.BOTTOM));
			Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), new EtchedBorder());
			toolbar.setBorder(b);
			toolbar.setBorderPainted(true);

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
			List toolbarActions = sql.getToolbarActions();

			HashMap menus = new HashMap(10);
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
				menu.add(action.getMenuItem());
			}
			for (int i=0; i < toolbarActions.size(); i++)
			{
				Action a = (Action)toolbarActions.get(i);
				boolean toolbarSep = "true".equals((String)a.getValue(WbAction.TBAR_SEPARATOR));
				{
					if (toolbarSep)
					{
						toolbar.addSeparator();
					}
					toolbar.add(a);
				}
			}
			toolbar.addSeparator();
			menuBar.add(this.buildToolsMenu());
			menuBar.add(this.buildHelpMenu());
			this.panelMenus.add(menuBar);
			this.panelToolbars.add(toolbar);
		}
		this.tabSelected(0);
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
		this.currentToolbar = (JToolBar)this.panelToolbars.get(anIndex);
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
		this.setTitle(ResourceMgr.TXT_PRODUCT_NAME + " [" + ConnectionMgr.getDisplayString(con) + "]");
	}

	public void selectConnection()
	{
		if (this.profileDialog == null)
		{
			this.profileDialog = new ProfileSelectionDialog(this, true);
		}
		this.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

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
			this.showStatusMessage("Connecting...");
			try
			{
				ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
				mgr.disconnect(this.windowId);
				WbConnection conn = WbManager.getInstance().getConnectionMgr().getConnection(this.getWindowId(), aProfile);
				this.setConnection(conn);
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
					for (int i=0; i < this.panelToolbars.size(); i++)
					{
						SwingUtilities.updateComponentTreeUI((JToolBar)this.panelToolbars.get(i));
					}
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
