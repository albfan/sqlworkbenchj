/*
 * MainWindow.java
 *
 * Created on November 25, 2001, 3:10 PM
 */

package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.gui.actions.*;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.settings.SettingsPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;



/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class MainWindow extends JFrame implements ActionListener, MouseListener, WindowListener, ChangeListener, FilenameChangeListener
{
	private static int instanceCount;
	private String windowId;
	private String currentProfileName;
	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;

	private DbExplorerPanel dbExplorerPanel;

	//private JMenuBar currentMenu;
	private FileDisconnectAction disconnectAction;
	private ShowDbExplorerAction dbExplorerAction;

	private JTabbedPane sqlTab = new JTabbedPane();
	private WbToolbar currentToolbar;
	private ArrayList panelMenus = new ArrayList(5);
	private int nextConnectionId = 0;

	private boolean dbExplorerTabVisible = false;

	/** Creates new MainWindow */
	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		instanceCount ++;
		this.windowId = "MainWindow" + Integer.toString(instanceCount);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(this);

		this.sqlTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);

		int tabCount = WbManager.getSettings().getDefaultTabCount();
		if (tabCount <= 0) tabCount = 1;

		this.disconnectAction = new FileDisconnectAction(this);
		this.disconnectAction.setEnabled(false);

		//ImageIcon dummy = ResourceMgr.getPicture("small_blank");
		for (int i=0; i < tabCount; i++)
		{
			SqlPanel sql = new SqlPanel(i + 1);
			sql.addFilenameChangeListener(this);
			char c = Integer.toString(i+1).charAt(0);
			this.sqlTab.addTab(ResourceMgr.getString("LabelTabStatement") + " " + c, null, sql);
			this.sqlTab.setMnemonicAt(i, c);
			sql.restoreSettings();
		}
		this.initMenu();

		this.getContentPane().add(this.sqlTab, BorderLayout.CENTER);
		this.setDisplayTitle(null);
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.restorePosition();
		this.setIconImage(ResourceMgr.getPicture("workbench16").getImage());

		if (WbManager.getSettings().getShowDbExplorerInMainWindow() &&
				WbManager.getSettings().getDbExplorerVisible())
		{
			this.addDbExplorerTab();
		}

		int lastIndex = WbManager.getSettings().getLastSqlTab();
		if (lastIndex < 0 || lastIndex > this.sqlTab.getTabCount() - 1)
		{
			lastIndex = 0;
		}

		this.sqlTab.setSelectedIndex(lastIndex);
		this.tabSelected(lastIndex);

		// now that we have setup the SplitPane we can add the
		// change listener
		this.sqlTab.addChangeListener(this);
		this.sqlTab.addMouseListener(this);
	}

	public String getWindowId() { return this.windowId; }

	public void addFilenameChangeListener(FilenameChangeListener aListener)
	{
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel panel = this.getSqlPanel(i);
			if (panel instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)panel;
				sql.addFilenameChangeListener(aListener);
			}
		}
	}

	public void removeFilenameChangeListener(FilenameChangeListener aListener)
	{
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel panel = this.getSqlPanel(i);
			if (panel instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)panel;
				sql.removeFilenameChangeListener(aListener);
			}
		}
	}

  public void addIndexChangeListener(ChangeListener aListener)
  {
    this.sqlTab.addChangeListener(aListener);
  }

  public void removeIndexChangeListener(ChangeListener aListener)
  {
    this.sqlTab.removeChangeListener(aListener);
  }

	private void initMenu()
	{
		this.dbExplorerAction = new ShowDbExplorerAction(this);
		this.dbExplorerAction.setEnabled(false);

		int tabCount = this.sqlTab.getTabCount();
		for (int tab=0; tab < tabCount; tab ++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(tab);
			JMenuBar menuBar = this.getMenuForPanel(sql);
			this.panelMenus.add(menuBar);
		}
	}

	private JMenuBar getMenuForPanel(MainPanel aPanel)
	{
		HashMap menus = new HashMap(10);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorderPainted(false);

		// Create the file menu for all tabs
		JMenu menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_FILE));
		menu.setName(ResourceMgr.MNU_TXT_FILE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_FILE, menu);

		WbAction action;

		action = new FileConnectAction(this);
		action.addToMenu(menu);
		this.disconnectAction.addToMenu(menu);

		action = new FileNewWindowAction();
		action.addToMenu(menu);
		//menu.addSeparator();
		menu.add(new ManageDriversAction(this));

		// now create the menus for the current tab
		List actions = aPanel.getActions();

		// Create the menus in the correct order
		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_EDIT));
		menu.setName(ResourceMgr.MNU_TXT_EDIT);
		menu.setVisible(false);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_EDIT, menu);

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_VIEW));
		menu.setName(ResourceMgr.MNU_TXT_VIEW);
		menu.setVisible(true);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_VIEW, menu);

		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i ++)
		{
			action = new SelectTabAction(this.sqlTab, i);
			menu.add(action.getMenuItem());
		}

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_DATA));
		menu.setName(ResourceMgr.MNU_TXT_DATA);
		menu.setVisible(false);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_DATA, menu);

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_SQL));
		menu.setName(ResourceMgr.MNU_TXT_SQL);
		menu.setVisible(false);
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
				menu = new WbMenu(ResourceMgr.getString(menuName));
				menuBar.add(menu);
				menus.put(menuName, menu);
			}
			boolean menuSep = "true".equals((String)action.getValue(WbAction.MENU_SEPARATOR));

			if (menuSep)
			{
				menu.addSeparator();
			}
			action.addToMenu(menu);
			menu.setVisible(true);
		}


		action = new FileExitAction();
		menu = (JMenu)menus.get(ResourceMgr.MNU_TXT_FILE);
		menu.addSeparator();
		menu.add(action.getMenuItem());

		menuBar.add(this.buildToolsMenu());
		menuBar.add(this.buildHelpMenu());

		aPanel.addToToolbar(this.dbExplorerAction, true);

		return menuBar;
	}

	public int getCurrentPanelIndex()
	{
		return this.sqlTab.getSelectedIndex();
	}

	public String[] getPanelLabels()
	{
		int tabCount = this.sqlTab.getTabCount();
		if (WbManager.getSettings().getShowDbExplorerInMainWindow() && this.dbExplorerPanel != null)
		{
			tabCount --;
		}

		String[] result = new String[tabCount];

		for (int i=0; i < tabCount; i++)
		{
			result[i] = this.sqlTab.getTitleAt(i);
		}
		return result;
	}

	public MainPanel getCurrentPanel()
	{
		int index = this.sqlTab.getSelectedIndex();
		return this.getSqlPanel(index);
	}

	public MainPanel getSqlPanel(int anIndex)
	{
		return (MainPanel)this.sqlTab.getComponentAt(anIndex);
	}

	public void selectTab(int anIndex)
	{
		this.sqlTab.setSelectedIndex(anIndex);
	}

	private void tabSelected(int anIndex)
	{
		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();

		WbConnection conn = current.getConnection();
		if (conn == null)
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab())
			{
				WbSwingUtilities.showWaitCursor(this);
				try
				{
					current.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
					current.setConnection(this.getConnectionForTab());
				}
				catch (Exception e)
				{
					LogMgr.logError("MainWindow.tabSelected()", "Error setting up connection for selected panel", e);
					WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("ErrorNoConnectionAvailable"));
				}
				current.showStatusMessage("");
				WbSwingUtilities.showDefaultCursor(this);
			}
			else 
			{
				current.setConnection(this.currentConnection);
			}
		}
		JMenuBar menu = (JMenuBar)this.panelMenus.get(anIndex);
		this.setJMenuBar(menu);

		if (this.currentToolbar != null) content.remove(this.currentToolbar);
		if (current != null)
		{
			this.currentToolbar = current.getToolbar();
			content.add(this.currentToolbar, BorderLayout.NORTH);
		}
		this.doLayout();
	}

	public void restoreState()
	{
		String state = WbManager.getSettings().getProperty(this.getClass().getName(), "state", "0");
		int i = 0;
		try { i = Integer.parseInt(state); } catch (Exception e) { i = 0; }
		if (i == MAXIMIZED_BOTH)
		{
			this.setExtendedState(i);
		}
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
    Settings sett = WbManager.getSettings();
		sett.setLastSqlTab(index);
		sett.setDbExplorerVisible(this.dbExplorerTabVisible);

		int tabCount = this.sqlTab.getTabCount();
		int realTabs = 0;


		for (int i=0; i < tabCount; i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			sql.saveSettings();
			if (sql instanceof SqlPanel) realTabs++;
		}
    sett.setDefaultTabCount(realTabs);

		int state = this.getExtendedState();
		sett.setProperty(this.getClass().getName(), "state", state);

		if (state != MAXIMIZED_BOTH)
		{
			sett.storeWindowPosition(this);
			sett.storeWindowSize(this);
		}
		if (dbExplorerPanel != null)
		{
			this.dbExplorerPanel.saveSettings();
      if (this.dbExplorerPanel.getWindow() != null)
      {
        this.dbExplorerPanel.getWindow().saveSettings();
      }
		}
	}

	public void fileNameChanged(Object sender, String newFilename)
	{
		String fname;
		int index = -1;
		String tooltip = null;
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			if (this.sqlTab.getComponentAt(i) == sender)
			{
				index = i;
				break;
			}
		}
		if (index == -1) return;

		if (newFilename == null)
		{
			fname = ResourceMgr.getString("LabelTabStatement") + " " + (index + 1);
		}
		else
		{
			File f = new File(newFilename);
			fname = f.getName();
			tooltip = f.getAbsolutePath();
		}
		this.sqlTab.setTitleAt(index, fname);
		this.sqlTab.setToolTipTextAt(index, tooltip);
	}
	public void windowOpened(WindowEvent windowEvent)
	{
	}

	public void windowClosed(WindowEvent e)
	{
		//WbManager.getInstance().windowClosed(e);//exitWorkbench();
	}

	public void windowDeiconified(WindowEvent windowEvent)
	{
		if (this.dbExplorerPanel != null)
		{
			this.dbExplorerPanel.mainWindowDeiconified();
		}
	}

	public void windowClosing(WindowEvent windowEvent)
	{
		//this.saveSettings();
		WbManager.getInstance().windowClosing(this);
	}

	public void windowDeactivated(WindowEvent windowEvent)
	{
	}

	public void windowActivated(WindowEvent windowEvent)
	{
	}

	public void windowIconified(WindowEvent windowEvent)
	{
		if (this.dbExplorerPanel != null)
		{
			this.dbExplorerPanel.mainWindowIconified();
		}
	}

	public void showStatusMessage(String aMsg)
	{
		MainPanel current = this.getCurrentPanel();
		if (current != null) current.showStatusMessage(aMsg);
	}

	public void showLogMessage(String aMsg)
	{
		MainPanel current = this.getCurrentPanel();
		if (current != null) current.showLogMessage(aMsg);
	}

	public boolean connectTo(ConnectionProfile aProfile)
	{
		boolean connected = false;
		try
		{
			this.disconnect();
			this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			this.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
			try
			{
				ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
				WbConnection conn = null;

				if (aProfile.getUseSeperateConnectionPerTab())
				{
					// getConnectionForTab() checks these variables
					// so they have to be initialized before setting
					// up the different connections
					this.currentConnection = null;
					this.currentProfile = aProfile;
					MainPanel p = this.getCurrentPanel();
					p.setConnection(this.getConnectionForTab());
					/*
					for (int i=0; i < this.sqlTab.getTabCount(); i++)
					{
						conn = this.getConnectionForTab();
						MainPanel p = this.getSqlPanel(i);
						p.setConnection(conn);
					}
					*/
				}
				else
				{
					conn = mgr.getConnection(aProfile, this.windowId);
					this.setConnection(conn);
				}
				this.getCurrentPanel().clearLog();
				this.getCurrentPanel().showResultPanel();
			}
			catch (ClassNotFoundException cnf)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_DRIVER_NOT_FOUND));
			}
			catch (SQLException se)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + se.toString());
			}
			this.showStatusMessage("");
			this.dbExplorerAction.setEnabled(true);
			this.disconnectAction.setEnabled(true);
			this.setDisplayTitle(aProfile);
			connected = true;
		}
		catch (Exception e)
		{
			String msg = e.getMessage();
			if (msg == null)
			{
				ExceptionUtil.getDisplay(e, true);
			}
			this.showLogMessage("Could not connect\n" + msg);
			LogMgr.logError("MainWindow.connectTo()", "Could not connect", e);
		}
		this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		return connected;
	}

	public void disconnect()
	{
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		WbConnection conn = null;
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			conn = sql.getConnection();
			if (conn != null) mgr.disconnect(conn.getId());
			sql.disconnect();
		}
		if (this.dbExplorerPanel != null)
		{
			// the Explorer panel might still be connected if
			// it was opened in a window (the loop over the tabs
			// did not include the panel)
			conn = this.dbExplorerPanel.getConnection();
			if (conn != null) mgr.disconnect(conn.getId());

			this.dbExplorerPanel.disconnect();
			DbExplorerWindow w = this.dbExplorerPanel.getWindow();
			if (w != null)
			{
				w.setVisible(false);
				w.dispose();
				w = null;
			}
		}
		this.setDisplayTitle(null);
		this.disconnectAction.setEnabled(false);
		if (this.dbExplorerAction != null) this.dbExplorerAction.setEnabled(false);
	}

	private void setDisplayTitle(ConnectionProfile aProfile)
	{
		if (aProfile == null)
		{
			this.setTitle(ResourceMgr.TXT_PRODUCT_NAME + " [" + ResourceMgr.getString("TxtNotConnected") + "]");
		}
		else
		{
			this.setTitle(ResourceMgr.TXT_PRODUCT_NAME + " [" + aProfile.getName() + "]");
		}
	}

	public void setConnection(WbConnection con)
	{
		boolean explorerIncluded = false;
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			sql.setConnection(con);
			explorerIncluded = (sql instanceof DbExplorerPanel);
		}
		this.currentConnection = con;

		if (this.dbExplorerPanel != null && !explorerIncluded)
		{
			try
			{
				this.dbExplorerPanel.setConnection(con, this.currentProfileName);
			}
			catch (Exception e)
			{
				LogMgr.logError(this, "Could not set connection for DbExplorerWindow", e);
				this.dbExplorerPanel.disconnect();
				this.dbExplorerPanel = null;
			}
		}
	}

	private boolean connecting = false;

	public void selectConnection()
	{
		if (connecting) return;
		try
		{
			// prevent a second
			this.connecting = true;
			WbSwingUtilities.showWaitCursor(this);
			ProfileSelectionDialog dialog = new ProfileSelectionDialog(this, true);
			WbSwingUtilities.center(dialog, this);
			WbSwingUtilities.showDefaultCursor(this);
			dialog.setVisible(true);
      if (!dialog.isCancelled())
      {
        ConnectionProfile prof = dialog.getSelectedProfile();
        if (prof != null)
        {
          this.currentProfileName = prof.getName();
          this.connectTo(prof);
          WbManager.getSettings().setLastConnection(this.currentProfileName);
        }
      }
			if (dialog != null) dialog.dispose();
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.selectConnection()", "Error when disposing dialog", th);
		}
		finally
		{
			this.connecting = false;
		}
	}

	public JMenu getViewMenu(int anIndex)
	{
		return this.getMenu(ResourceMgr.MNU_TXT_VIEW, anIndex);
	}

	public JMenu getMenu(String aName, int anIndex)
	{
		JMenuBar menubar = (JMenuBar)this.panelMenus.get(anIndex);
		for (int k=0; k < menubar.getMenuCount(); k++)
		{
			JMenu item = menubar.getMenu(k);
			if (aName.equals(item.getName())) return item;
		}
		return null;
	}

	public void addToViewMenu(SelectTabAction anAction)
	{
		for (int i=0; i < this.panelMenus.size(); i++)
		{
			JMenu view = this.getViewMenu(i);

			// insert the item at the correct index
      // (if it is a SelectTabAction)
      // otherwise insert it after the last SelectTabAction
			int count = view.getItemCount();
			int inserted = -1;
			for (int k=0; k < count; k++)
			{
        JMenuItem item = view.getItem(k);
        if (item == null) continue;
        Action ac = item.getAction();
        if (ac == null) continue;
        if (!(ac instanceof SelectTabAction))
				{
					break;
				}
        SelectTabAction a = (SelectTabAction)ac;

				if (a.getIndex() >= anAction.getIndex())
				{
					view.insert(anAction.getMenuItem(), k);
					inserted = k;
					break;
				}
			}

			if (inserted == -1)
			{
				// no index found which is greate or equal than the new one
				// so add it to the end
        if (!(view.getItem(count -1).getAction() instanceof SelectTabAction))
          view.addSeparator();

				view.add(anAction.getMenuItem());
			}
			else
			{
				// renumber the shortcuts for the remaining actions
				int newIndex = anAction.getIndex() + 1;
				for (int k=inserted + 1; k < count; k++)
				{
					SelectTabAction a = (SelectTabAction)view.getItem(k).getAction();
					a.setNewIndex(newIndex);
					newIndex ++;
				}
			}
		}
	}

	private WbConnection getConnectionForTab()
		throws Exception
	{
		if (this.currentConnection != null) return this.currentConnection;
		String id = this.windowId + "-" + this.nextConnectionId;
		LogMgr.logInfo("MainWindow.getConnectionForTab()", "Creating new connection for " + this.currentProfile.getName() + " with ID=" + id);
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		WbConnection conn = mgr.getConnection(this.currentProfile, id);
		this.nextConnectionId ++;
		return conn;
	}

	public void addDbExplorerTab()
	{
		if (this.dbExplorerPanel == null)
		{
			this.dbExplorerPanel = new DbExplorerPanel(this);
			this.dbExplorerPanel.restoreSettings();
		}
		JMenuBar dbmenu = this.getMenuForPanel(this.dbExplorerPanel);

		this.sqlTab.addTab(ResourceMgr.getString("LabelDbExplorer"), this.dbExplorerPanel);

		SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);

		this.panelMenus.add(dbmenu);
		this.addToViewMenu(action);
	}

	public void showDbExplorer()
	{
		if (this.dbExplorerPanel == null)
		{
			this.dbExplorerPanel = new DbExplorerPanel(this);
			this.dbExplorerPanel.restoreSettings();
			try
			{
				this.dbExplorerPanel.setConnection(this.getConnectionForTab());
			}
			catch (Exception e)
			{
				LogMgr.logError("MainWindow.showDbExplorer()", "Error getting new connection for DbExplorer tab. Using connection from current panel", e);
				this.dbExplorerPanel.setConnection(this.getCurrentPanel().getConnection());
			}
		}

		if (WbManager.getSettings().getShowDbExplorerInMainWindow())
		{
			Component c = this.sqlTab.getComponentAt(this.sqlTab.getTabCount() - 1);
			if (!(c instanceof DbExplorerPanel))
			{
				this.addDbExplorerTab();
				this.dbExplorerTabVisible = true;
			}
			this.sqlTab.setSelectedIndex(this.sqlTab.getTabCount() - 1);
		}
		else
		{
			this.dbExplorerPanel.openWindow(this.currentProfileName);
			this.dbExplorerTabVisible = false;
		}
	}

	public String getCurrentProfileName()
	{
		return this.currentProfileName;
	}

	public JMenu buildHelpMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_HELP));
		result.setName(ResourceMgr.MNU_TXT_HELP);
		JMenuItem item = new WbMenuItem(ResourceMgr.getString("MnuTxtHelpContents"));
		item.putClientProperty("command", "helpContents");
		item.addActionListener(this);
		result.add(item);

		item = new WbMenuItem(ResourceMgr.getString("MnuTxtAbout"));
		item.putClientProperty("command", "helpAbout");
		item.addActionListener(this);
		result.add(item);
		return result;
	}

	public JMenu buildToolsMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TOOLS));
		result.setName(ResourceMgr.MNU_TXT_TOOLS);

		result.add(this.dbExplorerAction);
		result.addSeparator();

		JMenuItem options = new WbMenuItem(ResourceMgr.getString(ResourceMgr.MNU_TXT_OPTIONS));
		options.setName(ResourceMgr.MNU_TXT_OPTIONS);
		options.putClientProperty("command", "optionsDialog");
		options.addActionListener(this);
		result.add(options);
		JMenu lnf = new WbMenu(ResourceMgr.getString("MnuTxtLookAndFeel"));
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		for (int i=0; i < info.length; i++)
		{
			JMenuItem item = new WbMenuItem(info[i].getName());
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
	 *	Adds a new SQL tab to the main window. This will be inserted
	 *	before the DbExplorer (if that is displayed as a tab)
	 */
	public void addTab()
	{
		int index = this.sqlTab.getTabCount();

		if (this.getSqlPanel(index - 1) instanceof DbExplorerPanel)
		{
			index --;
		}
		SqlPanel sql = new SqlPanel(index + 1);
		boolean newConnection = false;
		if (this.currentProfile != null)
		{
			newConnection = this.currentProfile.getUseSeperateConnectionPerTab();
		}

		if (newConnection)
		{
			try
			{
				WbConnection conn = WbManager.getInstance().getConnectionMgr().getConnection(this.currentProfile, this.windowId + "-" + this.nextConnectionId);
				sql.setConnection(conn);
			}
			catch (Exception e)
			{
				LogMgr.logError("MainWindow.addTab()", "Failed to obtain new connection for new SqlTab. Using connection from current tab!", e);
				sql.setConnection(this.getSqlPanel(0).getConnection());
			}
		}
		else
		{
			sql.setConnection(this.currentConnection);
		}
		this.sqlTab.add(sql, index);
		this.sqlTab.setTitleAt(index, ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(index+1));
		char c = Integer.toString(index+1).charAt(0);
		this.sqlTab.setMnemonicAt(index, c);
		this.sqlTab.doLayout();
		SelectTabAction a = new SelectTabAction(this.sqlTab, index);
		this.addToViewMenu(a);
		JMenuBar menuBar = this.getMenuForPanel(sql);
		this.panelMenus.add(index, menuBar);
		sql.initDefaults();
		this.sqlTab.setSelectedIndex(index);
	}

	/**
	 *	Removes the last SQL Tab. The DbExplorer will not be removed!
	 */
	public void removeTab()
	{
		MainPanel panel = this.getCurrentPanel();

		if (this.currentProfile != null && this.currentConnection == null)
		{
			WbConnection conn = panel.getConnection();
			WbManager.getInstance().getConnectionMgr().disconnect(conn.getId());
		}

		if (this.getCurrentPanel() instanceof DbExplorerPanel)
		{
			this.dbExplorerTabVisible = false;
		}

		int index = this.sqlTab.getSelectedIndex();
		this.sqlTab.remove(index);
		this.panelMenus.remove(index);

		int count = this.sqlTab.getTabCount();
		for (int i=index; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				((SqlPanel)p).setId(i+1);
				this.sqlTab.setTitleAt(i, ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(i + 1));
			}
		}
	}

	public void listMenus()
	{
		for (int i=0; i < this.panelMenus.size(); i ++)
		{
			JMenuBar bar = (JMenuBar)this.panelMenus.get(i);
			System.out.print("Menu " + i  + "=");
			for (int k=0; k < bar.getComponentCount(); k++)
			{
				JMenuItem item = (JMenuItem)bar.getComponent(k);
				System.out.print("/" + item.getText());
			}
			System.out.println("");
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
					WbManager.getSettings().setLookAndFeelClass(className);
					UIManager.setLookAndFeel(className);
					SwingUtilities.updateComponentTreeUI(this);
					if (this.dbExplorerPanel != null)
					{
						DbExplorerWindow win = this.dbExplorerPanel.getWindow();
						if (win != null)
						{
							SwingUtilities.updateComponentTreeUI(win);
						}
						else
						{
							//SwingUtilities.updateComponentTreeUI(this.dbExplorerPanel);
						}
					}
					for (int i=0; i < this.sqlTab.getTabCount(); i ++)
					{
						JMenuBar menu = (JMenuBar)this.panelMenus.get(i);
						SwingUtilities.updateComponentTreeUI(menu);
					}
				}
				catch (Exception ex)
				{
					LogMgr.logError(this, "Could not change look and feel", ex);
				}
			}
			else if ("optionsDialog".equals(command))
			{
				SettingsPanel panel = new SettingsPanel();
				panel.showSettingsDialog(this);
			}
			else if ("helpContents".equals(command))
			{
				JOptionPane.showMessageDialog(this, "Sorry! Help is not yet available\nFor command details check the tooltip for each menu item");
			}
			else if ("helpAbout".equals(command))
			{
				WbAboutDialog about = new WbAboutDialog(this, true);
				WbSwingUtilities.center(about, this);
				about.show();
			}

		}
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == this.sqlTab && e.getButton() == MouseEvent.BUTTON3)
		{
			MainPanel p = this.getCurrentPanel();
			SqlTabPopup pop = new SqlTabPopup(this);
			pop.show(this.sqlTab,e.getX(),e.getY());
		}
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

}