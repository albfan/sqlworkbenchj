/*
 * MainWindow.java
 *
 * Created on November 25, 2001, 3:10 PM
 */

package workbench.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
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
import workbench.gui.sql.SqlHistory;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.BrowserLauncher;
import workbench.util.WbWorkspace;




/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class MainWindow
	extends JFrame
	implements ActionListener, MouseListener, WindowListener, ChangeListener, FilenameChangeListener
{
	private static final String DEFAULT_WORKSPACE = "%ConfigDir%/Default.wksp";
	private static int instanceCount;
	private String windowId;

	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;

	private DbExplorerPanel dbExplorerPanel;

	private FileDisconnectAction disconnectAction;
	private ShowDbExplorerAction dbExplorerAction;

	private JTabbedPane sqlTab = new JTabbedPane();
	private WbToolbar currentToolbar;
	private ArrayList panelMenus = new ArrayList(5);
	private int nextConnectionId = 0;

	private boolean dbExplorerTabVisible = false;
	private String currentWorkspaceFile = null;

	private CloseWorkspaceAction closeWorkspaceAction;
	private SaveWorkspaceAction saveWorkspaceAction;
	private SaveAsNewWorkspaceAction saveAsWorkspaceAction;
	private LoadWorkspaceAction loadWorkspaceAction;
	private AssignWorkspaceAction assignWorkspaceAction;
	//private ClearWorkspaceAction clearWorkspace;

	private boolean isProfileWorkspace = false;
	private boolean workspaceLoaded = false;

	/** Creates new MainWindow */
	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		instanceCount ++;
		this.windowId = "MainWindow" + Integer.toString(instanceCount);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);

		this.sqlTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.currentProfile = null;

		this.disconnectAction = new FileDisconnectAction(this);
		this.disconnectAction.setEnabled(false);
		this.assignWorkspaceAction = new AssignWorkspaceAction(this);
		this.closeWorkspaceAction = new CloseWorkspaceAction(this);
	  this.saveAsWorkspaceAction = new SaveAsNewWorkspaceAction(this);

		this.loadWorkspaceAction = new LoadWorkspaceAction(this);
		this.saveWorkspaceAction = new SaveWorkspaceAction(this);

		this.initMenu();
		this.getContentPane().add(this.sqlTab, BorderLayout.CENTER);
		this.setIconImage(ResourceMgr.getPicture("workbench16").getImage());

		this.addTab(false);

		if (WbManager.getSettings().getShowDbExplorerInMainWindow() &&
				WbManager.getSettings().getDbExplorerVisible())
		{
			this.addDbExplorerTab();
		}
		// this necessary to initialize the size of the panel!
		// so that sql.initDefaults() will actually be able to
		// to set the divider at 50%
		this.pack();

		this.restorePosition();

		this.sqlTab.setSelectedIndex(0);
		this.tabSelected(0);

		SqlPanel sql = (SqlPanel)this.getSqlPanel(0);
		sql.initDefaults();

		this.updateWindowTitle();
		this.checkWorkspaceActions();

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

	private void checkWorkspaceActions()
	{
		this.saveWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
		this.assignWorkspaceAction.setEnabled(this.currentWorkspaceFile != null && this.currentProfile != null);
		this.closeWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
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

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_WORKSPACE));
		menu.setName(ResourceMgr.MNU_TXT_WORKSPACE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_WORKSPACE, menu);
		menu.add(this.saveWorkspaceAction);
		menu.add(this.saveAsWorkspaceAction);
		menu.add(this.loadWorkspaceAction);
		menu.addSeparator();
		menu.add(this.closeWorkspaceAction);
		menu.addSeparator();
		menu.add(this.assignWorkspaceAction);
		//menu.addSeparator();
		//menu.add(this.clearWorkspace);

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
		menu.add(new ManageDriversAction(this));
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
		if (this.dbExplorerTabVisible)
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

	private void checkConnectionForPanel(int anIndex)
	{
		MainPanel p = this.getSqlPanel(anIndex);
		this.checkConnectionForPanel(p);
	}

	private void checkConnectionForPanel(MainPanel aPanel)
	{
		this.checkConnectionForPanel(aPanel, true);
	}
	
	private void checkConnectionForPanel(MainPanel aPanel, boolean createConnection)
	{
		try
		{
			WbConnection conn = aPanel.getConnection();
			if (conn == null)
			{
				if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab() && createConnection)
				{
					WbSwingUtilities.showWaitCursor(this);
					try
					{
						aPanel.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
						aPanel.setConnection(this.getConnectionForTab());
					}
					catch (Exception e)
					{
						LogMgr.logError("MainWindow.checkConnectionForPanel()", "Error setting up connection for selected panel", e);
						WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("ErrorNoConnectionAvailable"));
					}
					aPanel.showStatusMessage("");
					WbSwingUtilities.showDefaultCursor(this);
				}
				else if (this.currentConnection != null)
				{
					aPanel.setConnection(this.currentConnection);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkConnectionForPanel()", "Error when checking connection", e);
		}
	}
	private void tabSelected(int anIndex)
	{
		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();

		this.checkConnectionForPanel(current);

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
			this.setSize(800,600);
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
		sett.setDbExplorerVisible(this.dbExplorerTabVisible);

		try
		{
			if (this.workspaceLoaded) this.saveWorkspace(this.currentWorkspaceFile);
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.saveSettings()", "Could not save workspace!", e);
		}

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
		if (!(sender instanceof SqlPanel)) return;
		
		int index = -1;
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			if (this.sqlTab.getComponentAt(i) == sender)
			{
				index = i;
				break;
			}
		}
		if (index == -1) return;

		SqlPanel sql = (SqlPanel)sender;
		sql.setTabTitle(this.sqlTab, index);
	}
	
	public void windowOpened(WindowEvent windowEvent)
	{
	}

	public void windowClosed(WindowEvent e)
	{
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
		if (this.isProfileWorkspace && this.currentWorkspaceFile != null)
		{
			this.saveWorkspace(this.currentWorkspaceFile);
		}
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
			this.disconnect(false);
			this.isProfileWorkspace = false;
			this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			this.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
			this.paint(this.getGraphics());
			
			try
			{
				ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
				WbConnection conn = null;

				this.loadWorkspaceForProfile(aProfile);
				
				if (aProfile.getUseSeperateConnectionPerTab())
				{
					// getConnectionForTab() checks these variables
					// so they have to be initialized before setting
					// up the different connections
					this.currentConnection = null;
					this.currentProfile = aProfile;
					MainPanel p = this.getCurrentPanel();
					conn = this.getConnectionForTab();
					p.setConnection(conn);
				}
				else
				{
					conn = mgr.getConnection(aProfile, this.windowId);
					this.setConnection(conn);
				}
				this.getCurrentPanel().clearLog();
				this.getCurrentPanel().showResultPanel();
				
				String warn = conn.getWarnings(true);
				if (warn != null)
				{
					this.getCurrentPanel().showLogMessage(warn);
				}
				
				this.currentProfile = aProfile;
				connected = true;
			}
			catch (ClassNotFoundException cnf)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_DRIVER_NOT_FOUND));
				LogMgr.logError("MainWindow.connectTo()", "Error when connecting", cnf);
			}
			catch (SQLException se)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + se.toString());
				LogMgr.logError("MainWindow.connectTo()", "SQL Exception when connecting", se);
			}
			catch (Throwable e)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + e.toString());
				LogMgr.logError("MainWindow.connectTo()", "Error during connect", e);
			}
			
			this.dbExplorerAction.setEnabled(true);
			this.disconnectAction.setEnabled(true);
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
		finally
		{
			this.showStatusMessage("");
			this.updateWindowTitle();
			this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		if (connected)
		{
			selectCurrentEditorLater();
		}
		return connected;
	}

	private void loadWorkspaceForProfile(ConnectionProfile aProfile)
	{
		String realFilename = null;
		try
		{
			String file = aProfile.getWorkspaceFile();
			realFilename = WbManager.getInstance().replaceConfigDir(file);
			if (realFilename != null && realFilename.length() > 0)
			{
				File f = new File(realFilename);
				if (!f.exists())
				{
					boolean open = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgProfileWorkspaceNotFound"));
					if (open)
					{
						file = WbManager.getInstance().getWorkspaceFilename(this, false, true);
						aProfile.setWorkspaceFile(file);
					}
				}
				if (file != null)
				{
					// loadWorkspace will replace the %ConfigDir% placeholder,
					// so we need to pass the original filename
					this.loadWorkspace(file);
					this.isProfileWorkspace = true;
				}
			}
			else
			{
				this.loadDefaultWorkspace();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.loadCurrentWorkspace()", "Error reading workspace " + realFilename, e);
			this.loadDefaultWorkspace();
		}
	}
	
	public void disconnect()
	{
		this.disconnect(true);
	}
	
	private void disconnect(boolean closeWorkspace)
	{
		if (this.currentWorkspaceFile != null)
		{
			this.saveWorkspace(this.currentWorkspaceFile);
		}
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
		}
		this.currentProfile = null;
		this.currentConnection = null;
		if (closeWorkspace)
		{
			this.closeWorkspace();
		}
		else
		{
			this.isProfileWorkspace = false;
			this.currentWorkspaceFile = null;
		}
		
		this.updateWindowTitle();
		this.disconnectAction.setEnabled(false);
		if (this.dbExplorerAction != null) this.dbExplorerAction.setEnabled(false);
	}

	public boolean abortAll()
	{
		boolean success = true;
		try
		{
			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					SqlPanel sp = (SqlPanel)sql;
					success = success && sp.abortExecution();
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.abortAll()", "Error stopping execution",e);
			success = false;
		}
		return success;
	}
	
	public void selectCurrentEditorLater()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run() { selectCurrentEditor(); }
		});
	}
	public void selectCurrentEditor()
	{
		MainPanel p = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)p;
			sql.selectEditor();
		}
	}
	
	private void updateWindowTitle()
	{
		StringBuffer title = new StringBuffer(ResourceMgr.TXT_PRODUCT_NAME);

		title.append("  [");

		if (this.currentProfile == null)
		{
			title.append(ResourceMgr.getString("TxtNotConnected"));
		}
		else
		{
			title.append(this.currentProfile.getName());
		}
		boolean appended = false;
		
		if (this.currentWorkspaceFile != null)
		{
			File f = new File(this.currentWorkspaceFile);
			String baseName = f.getName();
			if (!this.isProfileWorkspace) 
			{
				title.append("]");
				appended = true;
			}
			title.append(" - (");
			title.append(baseName);
			title.append(") ");
		}
		
		if (!appended) title.append("]");
		
		this.setTitle(title.toString());
	}

	/**
	 * used internally to store the current connection 
	 * if one connection for all tabs is used
	 */
	private void setConnection(WbConnection con)
	{
		boolean explorerIncluded = false;
		for (int i=0; i < this.sqlTab.getTabCount(); i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			sql.setConnection(con);
			explorerIncluded = (sql instanceof DbExplorerPanel);
		}
		this.currentConnection = con;
		this.currentProfile = con.getProfile();

		if (this.dbExplorerPanel != null && !explorerIncluded)
		{
			try
			{
				this.dbExplorerPanel.setConnection(con, this.currentProfile.getName());
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
					this.currentProfile = prof;
					WbManager.getSettings().setLastConnection(this.currentProfile.getName());
          this.connectTo(prof);
        }
      }
			if (dialog != null) dialog.dispose();
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.selectConnection()", "Error during connect", th);
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

	/**
	 *	Remove the action to select a specific SQL tab
	 *  from the view menu
	 */
	private void removeFromViewMenu(int sqlTabIndex)
	{
		int panelCount = this.panelMenus.size();
		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);

			int count = view.getItemCount();
			for (int k=0; k < count; k++)
			{
        JMenuItem item = view.getItem(k);
        if (item == null) continue;
        Action ac = item.getAction();
        if (ac == null) continue;

        if (ac instanceof SelectTabAction)
				{
	        SelectTabAction a = (SelectTabAction)ac;

					if (a.getIndex() == sqlTabIndex)
					{
						view.remove(k);
						break;
					}
				}
			}
		}
	}

	private void updateViewMenu(int sqlTabIndex, String aName)
	{
		int panelCount = this.panelMenus.size();
		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);

			int count = view.getItemCount();
			for (int k=0; k < count; k++)
			{
        JMenuItem item = view.getItem(k);
        if (item == null) continue;
        Action ac = item.getAction();
        if (ac == null) continue;

        if (ac instanceof SelectTabAction)
				{
	        SelectTabAction a = (SelectTabAction)ac;

					if (a.getIndex() == sqlTabIndex)
					{
						a.setName(aName);
						break;
					}
				}
			}
		}
	}

	public void addToViewMenu(SelectTabAction anAction)
	{
		int panelCount = this.panelMenus.size();
		int lastActionIndex = -1;

		for (int i=0; i < panelCount; i++)
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
				lastActionIndex = k;

				if (a.getIndex() >= anAction.getIndex())
				{
					view.insert(anAction.getMenuItem(), k);
					inserted = k;
					break;
				}
			}

			if (inserted == -1)
			{
				if (lastActionIndex == -1)
				{
					// no index found which is greater or equal than the new one
					// so add it to the end
	        if (!(view.getItem(count -1).getAction() instanceof SelectTabAction))
          view.addSeparator();

					view.add(anAction.getMenuItem());
				}
				else
				{
					// we found at least one SelectTabAction, so we'll
					// insert the new one right behind the last one.
					// (there might be other items in the view menu!)
					view.insert(anAction.getMenuItem(), lastActionIndex + 1);
				}
			}
			else
			{
				// renumber the shortcuts for the remaining actions
				int newIndex = anAction.getIndex() + 1;
				for (int k=inserted + 1; k < panelCount; k++)
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

		this.sqlTab.add(this.dbExplorerPanel);
		this.dbExplorerPanel.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);
		
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
			this.dbExplorerPanel.openWindow(this.currentProfile.getName());
			this.dbExplorerTabVisible = false;
		}
	}

	public String getCurrentProfileName()
	{
		if (this.currentProfile == null) return null;
		return this.currentProfile.getName();
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

	public void loadWorkspace()
	{
		String filename = WbManager.getInstance().getWorkspaceFilename(this, false, true);
		if (filename == null) return;
		this.loadWorkspace(filename);
		this.isProfileWorkspace = this.checkMakeProfileWorkspace();
		this.updateWindowTitle();
	}

	private boolean checkMakeProfileWorkspace()
	{
		boolean assigned = false;
		if (this.isProfileWorkspace)
		{
			boolean saveIt = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgAttachWorkspaceToProfile"));
			if (saveIt)
			{
				this.assignWorkspace();
				assigned = false;
			}
		}
		return assigned;
	}
	
	private void adjustTabCount(int newCount)
	{
		int tabCount = this.sqlTab.getTabCount();
		if (this.dbExplorerTabVisible) tabCount --;

		if (newCount > tabCount)
		{
			for (int i=0; i < (newCount - tabCount); i++)
			{
				this.addTab(false);
			}
		}
		else if (newCount < tabCount)
		{
			for (int i=0; i < (tabCount - newCount); i++)
			{
				this.removeLastTab();
			}
		}

	}

	private void loadDefaultWorkspace()
	{
		if (!this.loadWorkspace(DEFAULT_WORKSPACE))
		{
			this.currentWorkspaceFile = DEFAULT_WORKSPACE;
			this.adjustTabCount(1);
			this.resetTabTitles();
		}
	}

	public boolean loadWorkspace(String filename)
	{
		if (filename == null) return false;
		String realFilename = WbManager.getInstance().replaceConfigDir(filename);

		File f = new File(realFilename);
	 	if (!f.exists()) return false;

		boolean result = false;
		int index = 0;
		WbWorkspace w = null;
		this.currentWorkspaceFile = null;
		try
		{
			w = new WbWorkspace(realFilename, false);
			int entryCount = w.getEntryCount();
			if (entryCount == 0) entryCount = 1;
			this.adjustTabCount(entryCount);

			for (int i=0; i < entryCount; i++)
			{
				SqlPanel sql = (SqlPanel)this.getSqlPanel(i);
				sql.closeFile(true);
				sql.readFromWorkspace(w);
				sql.setTabTitle(this.sqlTab, i);
			}
			this.currentWorkspaceFile = realFilename;
			index = w.getSelectedTab();
			result = true;
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + filename + ": " + e.getMessage(), e);
			this.currentWorkspaceFile = null;
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
		}
		if (index < this.sqlTab.getTabCount())
		{
			this.sqlTab.setSelectedIndex(index);
		}
		this.workspaceLoaded = true;
		this.updateWindowTitle();
		this.checkWorkspaceActions();
		return result;
	}

	private void resetTabTitles()
	{
		String defaultTitle = ResourceMgr.getString("LabelTabStatement");
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			SqlPanel sql = (SqlPanel)this.getSqlPanel(i);
			this.setTabTitle(i, defaultTitle);
		}
	}

	public void closeWorkspace()
	{
		this.currentWorkspaceFile = null;
		this.isProfileWorkspace = false;
		this.workspaceLoaded = false;
		this.resetWorkspace();
		this.updateWindowTitle();
		this.checkWorkspaceActions();
	}

	public String getCurrentWorkspaceFile()
	{
		return this.currentWorkspaceFile;
	}

	public void resetWorkspace()
	{
		try
		{
			this.adjustTabCount(1);
			this.resetTabTitles();
			SqlPanel sql = (SqlPanel)this.getSqlPanel(0);
			sql.clearSqlStatements();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.resetWorkspace()", "Error when resetting workspace", e);
		}
	}

	public void assignWorkspace()
	{
		if (this.currentWorkspaceFile == null) return;
		if (this.currentProfile == null) return;
		String filename = WbManager.getInstance().putConfigDirKey(this.currentWorkspaceFile);
		this.currentProfile.setWorkspaceFile(filename);
		this.isProfileWorkspace = true;
	}

	public void saveWorkspace(String filename)
	{
		WbWorkspace w = null;
		boolean interactive = false;
		if (filename == null)
		{
			interactive = true;
			filename = WbManager.getInstance().getWorkspaceFilename(this, true);
			if (filename == null) return;
		}

		String realFilename = WbManager.getInstance().replaceConfigDir(filename);

		try
		{
			String defaultLabel = ResourceMgr.getString("LabelTabStatement");
			w = new WbWorkspace(realFilename, true);
			int count = this.sqlTab.getTabCount();
			int selected = this.sqlTab.getSelectedIndex();
			for (int i=0; i < count; i++)
			{
				if (this.sqlTab.getComponentAt(i) instanceof SqlPanel)
				{
					if (i == selected)
					{
						w.setSelectedTab(i);
					}
					SqlPanel sql = (SqlPanel)this.sqlTab.getComponentAt(i);
					sql.saveToWorkspace(w);

					if (sql.hasFileLoaded())
					{
						w.setExternalFileName(i, sql.getCurrentFileName());
					}
					else
					{
						String title = this.getPlainTabTitle(i);
						if (!title.startsWith(defaultLabel))
						{
							w.setTabTitle(i, title);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.saveWorkspace()", "Error saving workspace: " + filename, e);
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
		}
		if (this.isProfileWorkspace && interactive)
		{
			this.checkMakeProfileWorkspace();
		}
		this.currentWorkspaceFile = filename;
		this.updateWindowTitle();
		this.checkWorkspaceActions();
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

	public void addTab()
	{
		this.addTab(true);
	}

	/**
	 *	Adds a new SQL tab to the main window. This will be inserted
	 *	before the DbExplorer (if that is displayed as a tab)
	 */
	public void addTab(boolean selectNew)
	{
		int index = this.sqlTab.getTabCount();

		if (index > 0 && this.getSqlPanel(index - 1) instanceof DbExplorerPanel)
		{
			index --;
		}
		SqlPanel sql = new SqlPanel(index + 1);

		this.checkConnectionForPanel(sql, false);
		sql.addFilenameChangeListener(this);
		this.sqlTab.add(sql, index);
		this.setTabTitle(index, ResourceMgr.getString("LabelTabStatement") + " ");

		JMenuBar menuBar = this.getMenuForPanel(sql);
		this.panelMenus.add(index, menuBar);

		SelectTabAction a = new SelectTabAction(this.sqlTab, index);
		this.addToViewMenu(a);

		if (selectNew) this.sqlTab.doLayout();

		// initDefaults has to be called after doLayout()!!!
		sql.initDefaults();
		if (selectNew) sqlTab.setSelectedIndex(index);
	}

	/**
	 *	Returns the title of a tab without the index number
	 */
	private String getPlainTabTitle(int index)
	{
		String title = this.sqlTab.getTitleAt(index);
		int pos = title.lastIndexOf(' ');
		if (pos > -1)
			title = title.substring(0, pos);
			
		return title;
	}

	private void setTabTitle(int anIndex, String aName)
	{
		this.sqlTab.setTitleAt(anIndex, aName + " " + Integer.toString(anIndex+1));
		char c = Integer.toString(anIndex+1).charAt(0);
		this.sqlTab.setMnemonicAt(anIndex, c);
		this.updateViewMenu(anIndex, aName);
	}

	public void removeLastTab()
	{
		int index = this.sqlTab.getTabCount() - 1;
		MainPanel p = this.getSqlPanel(index);
		if (p instanceof DbExplorerPanel)
			index --;
		this.removeTab(index);
	}

	public boolean canRenameTab()
	{
		boolean canRename = (this.currentWorkspaceFile != null);
		canRename = canRename && (this.getCurrentPanel() instanceof SqlPanel);
		return canRename;
	}

	public void renameTab()
	{
		if (this.getCurrentPanel() instanceof DbExplorerPanel) return;

		int index = this.sqlTab.getSelectedIndex();

		String oldName = this.getPlainTabTitle(index);
		String newName = WbSwingUtilities.getUserInput(this.sqlTab, ResourceMgr.getString("MsgEnterNewTabName"), oldName);
		if (newName != null)
		{
			this.setTabTitle(index, newName);
			this.updateViewMenu(index, newName);
		}
	}

	public void removeTab()
	{
		int index = this.sqlTab.getSelectedIndex();
		this.removeTab(index);
	}

	/**
	 *	Removes the current SQL Tab. The DbExplorer will not be removed!
	 */
	public void removeTab(int index)
	{
		MainPanel panel = this.getSqlPanel(index);

		if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab())
		{
			WbConnection conn = panel.getConnection();
			if (conn != null)
			{
				WbManager.getInstance().getConnectionMgr().disconnect(conn.getId());
			}
		}

		if (panel instanceof DbExplorerPanel)
		{
			this.dbExplorerTabVisible = false;
		}

		this.panelMenus.remove(index);
		this.sqlTab.remove(index);
		this.removeFromViewMenu(index);

		int count = this.sqlTab.getTabCount();
		for (int i=index; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			p.setTabTitle(this.sqlTab, i);
		}
		int newTab = this.sqlTab.getSelectedIndex();
		this.tabSelected(newTab);
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
				try
				{
					BrowserLauncher.openURL("www.kellerer.org/workbench/manual/SQL Workbench Manual.html");
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(this, "The documentation is currently available at www.kellerer.org/workbench");
				}
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