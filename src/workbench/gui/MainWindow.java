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
import java.awt.EventQueue;
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
import workbench.gui.components.ExtensionFileFilter;
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

	private CloseWorkspaceAction closeWorkspace;
	private SaveWorkspaceAction saveWorkspace;
	private SaveAsNewWorkspaceAction saveAsWorkspace;
	private LoadWorkspaceAction loadWorkspace;
	
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

		int tabCount = WbManager.getSettings().getDefaultTabCount();
		if (tabCount <= 0) tabCount = 1;

		this.disconnectAction = new FileDisconnectAction(this);
		this.disconnectAction.setEnabled(false);

		this.closeWorkspace = new CloseWorkspaceAction(this);
	  this.saveAsWorkspace = new SaveAsNewWorkspaceAction(this);
		this.loadWorkspace = new LoadWorkspaceAction(this);
		this.saveWorkspace = new SaveWorkspaceAction(this);

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
		this.currentProfile = null;
		this.updateWindowTitle();
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

		if (WbManager.getSettings().getRestoreLastWorkspace())
		{
			String filename = WbManager.getSettings().getLastWorkspaceFile();
			if (filename == null)
			{
				File f = new File(WbManager.getSettings().getConfigDir(), "Default." + ExtensionFileFilter.WORKSPACE_EXT);
				filename = f.getAbsolutePath();
			}
			this.loadWorkspace(filename);
		}
		
		this.checkWorkspaceActions();
		
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

	private void checkWorkspaceActions()
	{
		this.saveWorkspace.setEnabled(this.currentWorkspaceFile != null);
		WbManager.getSettings().setLastWorkspaceFile(this.currentWorkspaceFile);
		if (WbManager.getSettings().getRestoreLastWorkspace())
		{
			this.closeWorkspace.setEnabled(false);
		}
		else
		{
			this.closeWorkspace.setEnabled(this.currentWorkspaceFile != null);
		}
		
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
		menu.addSeparator();
		menu.add(new ManageDriversAction(this));
		menu.addSeparator();
		menu.add(this.saveWorkspace);
		menu.add(this.saveAsWorkspace);
		menu.add(this.loadWorkspace);
		menu.add(this.closeWorkspace);
		
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
		WbConnection conn = aPanel.getConnection();
		if (conn == null)
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab())
			{
				WbSwingUtilities.showWaitCursor(this);
				try
				{
					aPanel.showStatusMessage(ResourceMgr.getString("MsgConnecting"));
					aPanel.setConnection(this.getConnectionForTab());
				}
				catch (Exception e)
				{
					LogMgr.logError("MainWindow.tabSelected()", "Error setting up connection for selected panel", e);
					WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("ErrorNoConnectionAvailable"));
				}
				aPanel.showStatusMessage("");
				WbSwingUtilities.showDefaultCursor(this);
			}
			else 
			{
				aPanel.setConnection(this.currentConnection);
			}
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
		sett.setLastSqlTab(index);
		sett.setDbExplorerVisible(this.dbExplorerTabVisible);

		int tabCount = this.sqlTab.getTabCount();
		int realTabs = 0;

		if (this.currentWorkspaceFile != null)
		{
			try
			{
				this.saveWorkspace(this.currentWorkspaceFile);
				WbManager.getSettings().setLastWorkspaceFile(this.currentWorkspaceFile);
			}
			catch (Exception e)
			{
			}
		}
		
		for (int i=0; i < tabCount; i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			if (sql instanceof SqlPanel) realTabs++;
			sql.saveSettings();
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
		/*
		if (WbManager.getSettings().getRestoreLastWorkspace() && this.currentWorkspaceFile == null)
		{
			this.saveWorkspace(null);
		}
		*/
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
				}
				else
				{
					conn = mgr.getConnection(aProfile, this.windowId);
					this.setConnection(conn);
				}
				this.getCurrentPanel().clearLog();
				this.getCurrentPanel().showResultPanel();
				this.currentProfile = aProfile;
				connected = true;
			}
			catch (ClassNotFoundException cnf)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_DRIVER_NOT_FOUND));
			}
			catch (SQLException se)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + se.toString());
			}
			catch (Exception e)
			{
				this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + e.toString());
			}
			this.showStatusMessage("");
			this.dbExplorerAction.setEnabled(true);
			this.disconnectAction.setEnabled(true);
			this.updateWindowTitle();
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
		}
		this.currentProfile = null;
		this.updateWindowTitle();
		this.disconnectAction.setEnabled(false);
		if (this.dbExplorerAction != null) this.dbExplorerAction.setEnabled(false);
	}

	private void updateWindowTitle()
	{
		StringBuffer title = new StringBuffer(ResourceMgr.TXT_PRODUCT_NAME);
		
		if (this.currentWorkspaceFile != null)
		{
			File f = new File(this.currentWorkspaceFile);
			String baseName = f.getName();
			title.append("  -  ");
			title.append(baseName);
		}
		
		title.append("  [");
		
		if (this.currentProfile == null)
		{
			title.append(ResourceMgr.getString("TxtNotConnected"));
		}
		else
		{
			title.append(this.currentProfile.getName());
		}
		title.append("]");
		this.setTitle(title.toString());
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
          if (this.connectTo(prof))
					{
						WbManager.getSettings().setLastConnection(this.currentProfile.getName());
					}
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
		int panelCount = this.panelMenus.size();
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
		String filename = WbManager.getInstance().getWorkspaceFilename(this, false);
		if (filename == null) return;
		this.loadWorkspace(filename);
	}
	
	public void loadWorkspace(String filename)
	{
		if (filename == null) return;
		
		WbWorkspace w = null;
		this.currentWorkspaceFile = null;
		try
		{
			w = new WbWorkspace(filename, false);
			int entryCount = w.getEntryCount();
			int tabCount = this.sqlTab.getTabCount();
			if (this.dbExplorerTabVisible) tabCount --;
				
			if (entryCount > tabCount)
			{
				for (int i=0; i < (entryCount - tabCount); i++)
				{
					this.addTab(false);
				}
			}
			else if (entryCount < tabCount)
			{
				for (int i=0; i < (tabCount - entryCount); i++)
				{
					this.removeLastTab();
				}
			}
			
			for (int i=0; i < entryCount; i++)
			{
				ArrayList data = w.getHistoryData(i);
				SqlPanel sql = (SqlPanel)this.getSqlPanel(i);
				sql.closeFile(true);
				sql.initStatementHistory(data);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + filename + ": " + e.getMessage());
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
		}
		this.currentWorkspaceFile = filename;
		this.updateWindowTitle();
		this.checkWorkspaceActions();
	}
	
	public void closeWorkspace()
	{
		this.currentWorkspaceFile = null;
		this.updateWindowTitle();
		this.checkWorkspaceActions();
	}

	public String getCurrentWorkspaceFile()
	{
		return this.currentWorkspaceFile;
	}
	
	public void saveWorkspace(String filename)
	{
		WbWorkspace w = null;
		
		if (filename == null)
		{
			filename = WbManager.getInstance().getWorkspaceFilename(this, true);
			if (filename == null) return;
		}
		
		try
		{
			w = new WbWorkspace(filename, true);
			int count = this.sqlTab.getComponentCount();
			for (int i=0; i < count; i++)
			{
				if (this.sqlTab.getComponentAt(i) instanceof SqlPanel)
				{
					SqlPanel sql = (SqlPanel)this.sqlTab.getComponentAt(i);
					ArrayList data = sql.getStatementHistory();
					String historyName = sql.getHistoryFilename();
					w.addHistoryEntry(historyName, data);
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

		if (this.getSqlPanel(index - 1) instanceof DbExplorerPanel)
		{
			index --;
		}
		SqlPanel sql = new SqlPanel(index + 1);
		
		this.checkConnectionForPanel(sql);

		this.sqlTab.add(sql, index);
		this.sqlTab.setTitleAt(index, ResourceMgr.getString("LabelTabStatement") + " " + Integer.toString(index+1));
		char c = Integer.toString(index+1).charAt(0);
		this.sqlTab.setMnemonicAt(index, c);
		
		JMenuBar menuBar = this.getMenuForPanel(sql);
		this.panelMenus.add(index, menuBar);
		
		SelectTabAction a = new SelectTabAction(this.sqlTab, index);
		this.addToViewMenu(a);

		this.sqlTab.doLayout();
		
		// initDefaults has to be called after doLayout()!!! 
		sql.initDefaults();
		if (selectNew) sqlTab.setSelectedIndex(index);
	}

	public void removeLastTab()
	{
		int index = this.sqlTab.getTabCount() - 1;
		MainPanel p = this.getSqlPanel(index);
		if (p instanceof DbExplorerPanel)
			index --;
		this.removeTab(index);
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

		// currentConnetion == null means that each panel has its own connection
		if (this.currentProfile != null && this.currentConnection == null)
		{
			WbConnection conn = panel.getConnection();
			WbManager.getInstance().getConnectionMgr().disconnect(conn.getId());
		}

		if (this.getCurrentPanel() instanceof DbExplorerPanel)
		{
			this.dbExplorerTabVisible = false;
		}

		this.panelMenus.remove(index);
		this.sqlTab.remove(index);

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
		int newTab = this.sqlTab.getSelectedIndex();
		this.tabSelected(newTab);
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