/*
 * MainWindow.java
 *
 * Created on November 25, 2001, 3:10 PM
 */

package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.gui.actions.AddMacroAction;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.AssignWorkspaceAction;
import workbench.gui.actions.CloseWorkspaceAction;
import workbench.gui.actions.DataPumperAction;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileDisconnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.FileNewWindowAction;
import workbench.gui.actions.LoadWorkspaceAction;
import workbench.gui.actions.ManageDriversAction;
import workbench.gui.actions.ManageMacroAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.actions.SaveAsNewWorkspaceAction;
import workbench.gui.actions.SaveWorkspaceAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.ShowDbExplorerAction;
import workbench.gui.actions.VersionCheckAction;
import workbench.gui.actions.ViewLineNumbers;
import workbench.gui.actions.WbAction;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;

import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.help.WhatsNewViewer;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.settings.SettingsPanel;
import workbench.gui.settings.ShortcutEditor;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.MacroManager;
import workbench.util.WbWorkspace;


/**
 *
 * @author  workbench@kellerer.org
 * @version
 */
public class MainWindow
	extends JFrame
	implements ActionListener, MouseListener, WindowListener, ChangeListener,
						FilenameChangeListener, MacroChangeListener, DbExecutionListener
{
	private static final String DEFAULT_WORKSPACE = "%ConfigDir%/Default.wksp";
	private static int instanceCount;
	private String windowId;

	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;

	private DbExplorerPanel dbExplorerPanel;

	private FileDisconnectAction disconnectAction;
	private ShowDbExplorerAction dbExplorerAction;

	private WbTabbedPane sqlTab = new WbTabbedPane();
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

	// will indicate a connect or disconnect in progress
	// connecting and disconnecting is done a separate thread
	// so that slow connections do not block the GUI
	private boolean connectInProgress = false;

	private AddMacroAction createMacro;
	private ManageMacroAction manageMacros;

	private int runningJobs = 0;

	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		instanceCount ++;
		this.windowId = "WbWin-" + Integer.toString(instanceCount);
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

		this.addTab(false, false);

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
		this.updateGuiForTab(0);

		SqlPanel sql = (SqlPanel)this.getSqlPanel(0);
		sql.initDefaults();

		this.updateWindowTitle();
		this.checkWorkspaceActions();

		this.sqlTab.addChangeListener(this);
		this.sqlTab.addMouseListener(this);
		//this.sqlTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		MacroManager.getInstance().addChangeListener(this);
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

		this.createMacro = new AddMacroAction();
		this.createMacro.setEnabled(false);
		this.manageMacros = new ManageMacroAction(this);

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

		if (aPanel instanceof SqlPanel)
		{
			menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_MACRO));
			menu.setName(ResourceMgr.MNU_TXT_MACRO);
			menu.setVisible(true);
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_MACRO, menu);
			this.buildMacroMenu(menu);
		}

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

		menu = (JMenu)menus.get(ResourceMgr.MNU_TXT_FILE);
		menu.addSeparator();
		menu.add(new ManageDriversAction(this));
		menu.addSeparator();

		action = new FileExitAction();
		menu.add(action.getMenuItem());

		menu = (JMenu)menus.get(ResourceMgr.MNU_TXT_VIEW);
		AddTabAction add = new AddTabAction(this);
		menu.addSeparator();
		menu.add(add.getMenuItem());

		RemoveTabAction rem = new RemoveTabAction(this);
		menu.add(rem.getMenuItem());
		menu.addSeparator();
		ViewLineNumbers v = new ViewLineNumbers();
		v.addToMenu(menu);

		menuBar.add(this.buildToolsMenu());
		menuBar.add(this.buildHelpMenu());

		aPanel.addToToolbar(this.dbExplorerAction, true);

		return menuBar;
	}

	private void setMacroMenuEnabled(boolean enabled)
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macro = this.getMacroMenu(i);
			if (macro != null)
			{
				int itemCount = macro.getItemCount();
				for (int in=2; in < itemCount; in++)
				{
					JMenuItem item = macro.getItem(in);
					if (item != null) item.setEnabled(enabled);
				}
			}
		}
	}

	public void macroListChanged()
	{
		this.updateMacroMenus();
	}

	private void updateMacroMenus()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macros = (JMenu)this.getMacroMenu(i);
			if (macros != null)
			{
				this.buildMacroMenu(macros);
			}
		}
	}

	private void buildMacroMenu(JMenu macroMenu)
	{
		macroMenu.removeAll();
		this.createMacro.addToMenu(macroMenu);
		this.manageMacros.addToMenu(macroMenu);

		List macros = MacroManager.getInstance().getMacroList();
		if (macros == null || macros.size() == 0) return;

		macroMenu.addSeparator();

		Collections.sort(macros);
		int count = macros.size();
		RunMacroAction run = null;
		for (int i=0; (i < count && i < 10); i++)
		{
			String name = (String)macros.get(i);
			run = new RunMacroAction(this, name, i+1);
			run.addToMenu(macroMenu);
		}
	}

	public int getCurrentPanelIndex()
	{
		return this.sqlTab.getSelectedIndex();
	}

	public int getIndexForPanel(MainPanel panel)
	{
		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.getId().equals(panel.getId())) return i;
		}
		return -1;
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

	public SqlPanel getCurrentSqlPanel()
	{
		MainPanel p = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			return (SqlPanel)p;
		}
		return null;
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
		this.checkConnectionForPanel(p, true);
	}

	private void checkConnectionForPanel(MainPanel aPanel)
	{
		this.checkConnectionForPanel(aPanel, true);
	}

	private void clearConnectInProgress()
	{
		synchronized (this)
		{
			this.connectInProgress = false;
		}
	}

	private synchronized void setConnectInProgress() { this.connectInProgress = true; }

	private void checkConnectionForPanel(final MainPanel aPanel, boolean createConnection)
	{
		if (aPanel.isConnected()) return;
		if (this.connectInProgress) return;

		try
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab() && createConnection)
			{
				Thread t = new Thread(new Runnable()
				{
					public void run()
					{
						connectPanel(aPanel);
					}
				});
				t.setName("Panel connection thread for " + aPanel.getId());
				t.start();
			}
			else if (this.currentConnection != null)
			{
				aPanel.setConnection(this.currentConnection);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkConnectionForPanel()", "Error when checking connection", e);
		}
	}

	/**
	 *	This method will be executed in a separate thread!
	 */
	private void connectPanel(final MainPanel aPanel)
	{
		this.showConnectingInfo();
		try
		{
			final WbConnection conn = this.getConnectionForTab(aPanel);
			final int index = this.getIndexForPanel(aPanel);
			this.tabConnected(aPanel, conn, index);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void tabConnected(final MainPanel panel, WbConnection conn, final int anIndex)
	{
		this.closeConnectingInfo();
		panel.setConnection(conn);

		if (SwingUtilities.isEventDispatchThread())
		{
			this.updateGuiForTab(anIndex);
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						updateGuiForTab(anIndex);
						selectCurrentEditor();
					}
				});
			}
			catch (Exception e)
			{
				LogMgr.logError("MainWindow.doUpdateForTab()", "Error executing GUI update on AWT thread", e);
			}
		}
	}

	private void updateGuiForTab(int anIndex)
	{
		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();
		if (current == null) return;

		JMenuBar menu = (JMenuBar)this.panelMenus.get(anIndex);
		this.setJMenuBar(menu);

		if (this.currentToolbar != null) content.remove(this.currentToolbar);
		if (current != null)
		{
			this.currentToolbar = current.getToolbar();
			content.add(this.currentToolbar, BorderLayout.NORTH);
		}
		this.setMacroMenuEnabled(current.isConnected());
		this.doLayout();
	}

	private void tabSelected(int anIndex)
	{
		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();

		if (!this.connectInProgress)	this.checkConnectionForPanel(current);
		this.updateGuiForTab(anIndex);
		if (current instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)current;
			sql.selectEditorLater();
			this.createMacro.setClient(sql.getEditor());
		}
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
    Settings sett = WbManager.getSettings();

    // this stores the state of the dbExplorer tab
    // if the DbExplorer should be displayed as a tab
    // it will be restored the next time if is't visible when
    // closing the window
		sett.setDbExplorerVisible(this.dbExplorerTabVisible);

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

	/**
	 *	Display a message in the status bar
	 */
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

	public void connectTo(final ConnectionProfile aProfile)
	{
		this.connectTo(aProfile, false);
	}
	public void connectTo(final ConnectionProfile aProfile, final boolean showDialogOnError)
	{
		if (this.connectInProgress) return;

		if (this.currentWorkspaceFile != null)
		{
			this.saveWorkspace(this.currentWorkspaceFile);
		}

		try
		{
			this.showConnectingInfo();

			Thread t = new Thread()
			{
				public void run()
				{
					disconnect(false, false);
					currentProfile = aProfile;
					setConnectInProgress();
					showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
					loadWorkspaceForProfile(aProfile);
					showStatusMessage(ResourceMgr.getString("MsgConnecting"));
					Thread.yield();
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							repaint();
						}
					});
					Thread.yield();
					doConnect(showDialogOnError);
				}
			};
			t.setName("MainWindow connection thread");
			t.start();

		}
		catch (Exception e)
		{
		}
	}

	private void doConnect(final boolean showSelectDialogOnError)
	{
		boolean connected = false;
		WbConnection conn = null;
		String error = null;

		try
		{
			ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();

			String id = "Wb-0";

			if (this.currentProfile.getUseSeperateConnectionPerTab())
			{
				this.currentConnection = null;
				MainPanel p = this.getCurrentPanel();
				id = p.getId();
			}
			conn = mgr.getConnection(this.currentProfile, id);
			connected = true;
		}
		catch (ClassNotFoundException cnf)
		{
			this.currentProfile = null;
			this.showLogMessage(ResourceMgr.getString("ErrorDriverNotFoundExtended"));
			LogMgr.logError("MainWindow.connectTo()", "Error when connecting", cnf);
			error = ResourceMgr.getString("ErrorDriverNotFound");
		}
		catch (SQLException se)
		{
			this.currentProfile = null;
			this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + se.toString());
			LogMgr.logError("MainWindow.connectTo()", "SQL Exception when connecting", se);
			error = se.getMessage();
		}
		catch (Throwable e)
		{
			this.currentProfile = null;
			this.showLogMessage(ResourceMgr.getString(ResourceMgr.ERR_CONNECTION_ERROR) + "\r\n\n" + e.toString());
			LogMgr.logError("MainWindow.connectTo()", "Error during connect", e);
			error = ExceptionUtil.getDisplay(e);
		}

		final WbConnection usedConnection = conn;
		this.showStatusMessage("");

		try
		{
			if (connected)
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						connected(usedConnection);
					}
				});
			}
			else
			{
				final String msg = error;
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						connectFailed(msg);
						if (showSelectDialogOnError)
						{
							selectConnection();
						}
					}
				});
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 *	Call back functioin which gets executed on the AWT thread after
	 *  the initial connection has been completed
	 */
	private void connected(WbConnection conn)
	{
		WbManager.getSettings().setLastConnection(this.currentProfile.getName());
		if (this.currentProfile.getUseSeperateConnectionPerTab())
		{
			this.getCurrentPanel().setConnection(conn);
		}
		else
		{
			this.setConnection(conn);
		}
		this.setMacroMenuEnabled(true);
		this.updateWindowTitle();
		this.dbExplorerAction.setEnabled(true);
		this.disconnectAction.setEnabled(true);
		selectCurrentEditorLater();
		this.getCurrentPanel().clearLog();
		this.getCurrentPanel().showResultPanel();

		String warn = conn.getWarnings(true);
		if (warn != null)
		{
			this.getCurrentPanel().showLogMessage(warn);
		}
		this.clearConnectInProgress();
		this.closeConnectingInfo();
	}

	private void connectFailed(String error)
	{
		this.setMacroMenuEnabled(false);
		this.updateWindowTitle();
		this.dbExplorerAction.setEnabled(false);
		this.disconnectAction.setEnabled(false);
		this.clearConnectInProgress();
		this.closeConnectingInfo();
		String msg = ResourceMgr.getString("ErrorConnectFailed").replaceAll("%msg%", error.trim());
		WbManager.getInstance().showErrorMessage(this, msg);
	}

	private static final int CREATE_WORKSPACE = 0;
	private static final int LOAD_OTHER_WORKSPACE = 1;
	private static final int IGNORE_MISSING_WORKSPACE = 2;

	private int checkNonExistingWorkspace()
	{
		String[] options = new String[] { ResourceMgr.getString("LabelCreateWorkspace"), ResourceMgr.getString("LabelLoadWorkspace"), ResourceMgr.getString("LabelIgnore")};
		JOptionPane ignorePane = new JOptionPane(ResourceMgr.getString("MsgProfileWorkspaceNotFound"), JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(this, ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setResizable(true);
		dialog.pack();
		dialog.show();
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return CREATE_WORKSPACE;
		else if (result.equals(options[0])) return CREATE_WORKSPACE;
		else if (result.equals(options[1])) return LOAD_OTHER_WORKSPACE;
		else return IGNORE_MISSING_WORKSPACE;
	}

	private void loadWorkspaceForProfile(ConnectionProfile aProfile)
	{
		//this.showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
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
					int action = this.checkNonExistingWorkspace();
					if (action == LOAD_OTHER_WORKSPACE)
					{
						file = WbManager.getInstance().getWorkspaceFilename(this, false, true);
						aProfile.setWorkspaceFile(file);
					}
					else if (action == IGNORE_MISSING_WORKSPACE)
					{
						file = null;
						aProfile.setWorkspaceFile(null);
					}
				}
				if (file != null)
				{
					// loadWorkspace will replace the %ConfigDir% placeholder,
					// so we need to pass the original filename
					this.isProfileWorkspace = true;
					this.loadWorkspace(file);
				}
				else
				{
					this.loadDefaultWorkspace();
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
		finally
		{
			//this.showStatusMessage("");
		}
	}

	public void disconnect(boolean background, final boolean closeWorkspace)
	{
		if (this.connectInProgress) return;
		this.setConnectInProgress();

		if (background)
		{
			Thread t = new Thread()
			{
				public void run()
				{
					if (closeWorkspace) saveWorkspace();
					showDisconnectInfo();
					doDisconnect();
					if (closeWorkspace) closeWorkspace();
					closeConnectingInfo();
				}
			};
			t.setName("MainWindow - disconnect thread");
			t.start();
		}
		else
		{
			if (closeWorkspace) saveWorkspace();
			this.doDisconnect();
			if (closeWorkspace) closeWorkspace();
		}
	}

	/**
	 *	This does the real disconnect action.
	 *  It needs to be public in order for the WbManager to be
	 *  able to initiate a synchronous disconnect. In any other
	 *  case disconnect() should be used!
	 */
	public void doDisconnect()
	{
		try
		{
			ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
			WbConnection conn = null;
			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					((SqlPanel)sql).abortExecution();
				}
				conn = sql.getConnection();
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
					}
				});
				if (conn != null) mgr.disconnect(conn.getId());
				// make sure this is executed on the AWT Thread!
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						sql.disconnect();
					}
				});
			}

			if (this.dbExplorerPanel != null)
			{
				// the Explorer panel might still be connected if
				// it was opened in a window (the loop over the tabs
				// did not include the panel)
				conn = this.dbExplorerPanel.getConnection();
				if (conn != null) mgr.disconnect(conn.getId());

				// disconnect will close the window as well!
				this.dbExplorerPanel.disconnect();
				this.dbExplorerPanel.dispose();
				this.dbExplorerPanel = null;
			}
		}
		finally
		{
			this.disconnected();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					showStatusMessage("");
				}
			});
		}
	}

	private void disconnected()
	{
		this.currentProfile = null;
		this.currentConnection = null;

		this.setMacroMenuEnabled(false);
		this.updateWindowTitle();
		this.disconnectAction.setEnabled(false);
		if (this.dbExplorerAction != null) this.dbExplorerAction.setEnabled(false);
		this.clearConnectInProgress();
		this.showStatusMessage("");
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
		StringBuffer title = new StringBuffer(50);
		if (this.runningJobs > 0)
		{
			title.append("» ");
		}
		title.append(ResourceMgr.TXT_PRODUCT_NAME);

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


	private JDialog connectingInfo;
	private JLabel connectLabel;

	private void closeConnectingInfo()
	{
		if (this.connectingInfo != null)
		{
			this.connectLabel = null;
			this.connectingInfo.setVisible(false);
			this.connectingInfo.dispose();
			this.connectingInfo = null;
			this.repaint();
		}
	}

	private void showDisconnectInfo()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgDisconnecting"));
	}

	/** Display a little PopupWindow to tell the user that the
	 *  workbench is currently connecting to the DB
	 */
	private void showConnectingInfo()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgConnecting"));
	}

	private void showPopupMessagePanel(String aMsg)
	{
		if (this.connectingInfo != null)
		{
			this.connectLabel.setText(aMsg);
			this.connectingInfo.repaint();
			Thread.yield();
			return;
		}
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.BEVEL_BORDER_RAISED);
		p.setLayout(new BorderLayout());
		this.connectLabel = new JLabel(aMsg);
		this.connectLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(this.connectLabel, BorderLayout.CENTER);
		this.connectingInfo = new JDialog(this, false);
		this.connectingInfo.getContentPane().setLayout(new BorderLayout());
		this.connectingInfo.getContentPane().add(p, BorderLayout.CENTER);
		this.connectingInfo.setUndecorated(true);
		this.connectingInfo.setSize(200,50);
		WbSwingUtilities.center(this.connectingInfo, this);
		this.connectingInfo.show();
		Thread.yield();
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
		if (this.currentProfile == null) this.currentProfile = con.getProfile();

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

	public void startSelectConnection()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						selectConnection();
					}
				});
			}
		};
		t.setName("Select Connection thread");
		t.start();
	}

	public void selectConnection()
	{
		if (this.connectInProgress) return;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			ProfileSelectionDialog dialog = new ProfileSelectionDialog(this, true);
			WbSwingUtilities.center(dialog, this);
			WbSwingUtilities.showDefaultCursor(this);
			dialog.setVisible(true);
      ConnectionProfile prof = dialog.getSelectedProfile();
			boolean cancelled = dialog.isCancelled();
			dialog.setVisible(false);
			dialog.dispose();

			this.repaint();

      if (!cancelled && prof != null)
      {
				this.connectTo(prof);
      }
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.selectConnection()", "Error during connect", th);
		}
	}

	public JMenu getMacroMenu(int anIndex)
	{
		JMenu menu = this.getMenu(ResourceMgr.MNU_TXT_MACRO, anIndex);
		return menu;
	}

	public JMenu getViewMenu(int anIndex)
	{
		return this.getMenu(ResourceMgr.MNU_TXT_VIEW, anIndex);
	}

	public JMenu getMenu(String aName, int anIndex)
	{
		if (anIndex < 0 || anIndex >= this.panelMenus.size()) return null;
		JMenuBar menubar = (JMenuBar)this.panelMenus.get(anIndex);
		int count = menubar.getMenuCount();
		for (int k=0; k < count; k++)
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

	/**
	 *	Add the approriate menu item to select a given tab
	 *  to the View menu.
	 */
	public void addToViewMenu(SelectTabAction anAction)
	{
		int panelCount = this.panelMenus.size();
		int lastActionIndex = -1;

		SelectTabAction lastAction = null;

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
				lastAction = a;
				lastActionIndex = k;

				if (a.getIndex() > anAction.getIndex())
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
				else if (lastAction != null && lastAction.getIndex() != anAction.getIndex())
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

	private WbConnection getConnectionForTab(MainPanel aPanel)
		throws Exception
	{
		if (this.currentConnection != null) return this.currentConnection;
		String id = aPanel.getId();
		aPanel.showStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + this.currentProfile.getName() + " ...");
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		WbConnection conn = mgr.getConnection(this.currentProfile, id);
		return conn;
	}

	private void createDbExplorerPanel()
	{
		if (this.dbExplorerPanel == null)
		{
			int count = this.sqlTab.getTabCount() + 1;
			this.dbExplorerPanel = new DbExplorerPanel(this, count);
			this.dbExplorerPanel.restoreSettings();
		}
	}
	public void addDbExplorerTab()
	{
		this.createDbExplorerPanel();
		JMenuBar dbmenu = this.getMenuForPanel(this.dbExplorerPanel);

		this.sqlTab.add(this.dbExplorerPanel);
		this.dbExplorerPanel.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);

		SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);

		this.panelMenus.add(dbmenu);
		this.addToViewMenu(action);
		this.dbExplorerTabVisible = true;
	}

	/**
	 *	Displays the DbExplorer. Either in a separate tab,
	 *  or as a new window.
	 */
	public void showDbExplorer()
	{
		this.createDbExplorerPanel();
		if (WbManager.getSettings().getShowDbExplorerInMainWindow())
		{
			Component c = this.sqlTab.getComponentAt(this.sqlTab.getTabCount() - 1);
			if (!(c instanceof DbExplorerPanel))
			{
				this.addDbExplorerTab();
				// we cannot activate the tab yet, as that will trigger
				// the connection process, and we want to control
				// that here, so that the a separate thread can be used
				this.dbExplorerTabVisible = true;
			}
			this.sqlTab.setSelectedIndex(this.sqlTab.getTabCount() - 1);
		}
		else
		{
			this.dbExplorerPanel.openWindow(this.currentProfile.getName());
			this.dbExplorerTabVisible = false;
		}

		// connecting can be pretty time consuming on a slow system
		// so move it into its own thread...
		if (!this.dbExplorerPanel.isConnected())
		{
			Thread t = new Thread()
			{
				public void run()
				{
					setConnectionForDbExplorer();
				}
			};
			t.setDaemon(true);
			t.setName("DbExplorer connection thread");
			t.start();
		}

	}

	/**
	 *	Set the connection for the current DbExplorer. This is started
	 *  as a separate thread from showDbExplorer() in order not to block
	 *  the AWT thread.
	 */
	private void setConnectionForDbExplorer()
	{
		try
		{
			final WbConnection conn = this.getConnectionForTab(this.dbExplorerPanel);

			// make sure the setConnection is called on the AWT thread, so that the
			// GUI update will work properly
//			SwingUtilities.invokeLater(new Runnable()
//			{
//				public void run()
//				{
					dbExplorerPanel.setConnection(conn);
					if (dbExplorerTabVisible)
					{
						sqlTab.setSelectedIndex(sqlTab.getTabCount() - 1);
						if (WbManager.getSettings().getRetrieveDbExplorer())
						{
							dbExplorerPanel.startRetrieve();
						}
					}
					dbExplorerPanel.updateUI();
//				}
//			});
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.showDbExplorer()", "Error getting new connection for DbExplorer tab. Using connection from current panel", e);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					dbExplorerPanel.setConnection(getCurrentPanel().getConnection());
					dbExplorerPanel.updateUI();
				}
			});
		}
	}

	public ConnectionProfile getCurrentProfile() { return this.currentProfile; }
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
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		item.putClientProperty("command", "helpContents");
		item.addActionListener(this);
		result.add(item);

		result.addSeparator();

		item = new WbMenuItem(ResourceMgr.getString("MnuTxtWhatsNew"));
		item.putClientProperty("command", "whatsNew");
		item.addActionListener(this);
		result.add(item);

		VersionCheckAction version = new VersionCheckAction();
		result.add(version);

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

		DataPumperAction pumper = new DataPumperAction(this);
		result.add(pumper);

		result.addSeparator();

		JMenuItem options = new WbMenuItem(ResourceMgr.getString(ResourceMgr.MNU_TXT_OPTIONS));
		options.setName(ResourceMgr.MNU_TXT_OPTIONS);
		options.putClientProperty("command", "optionsDialog");
		options.addActionListener(this);
		result.add(options);

		JMenuItem shortcuts = new WbMenuItem(ResourceMgr.getString("MnuTxtConfigureShortcuts"));
		shortcuts.setName("shortcuts");
		shortcuts.putClientProperty("command", "keyboardDialog");
		shortcuts.addActionListener(this);
		result.add(shortcuts);

		JMenu lnf = new WbMenu(ResourceMgr.getString("MnuTxtLookAndFeel"));
		lnf.setName("lnf");
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		String current = UIManager.getLookAndFeel().getClass().getName();

		for (int i=0; i < info.length; i++)
		{
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(info[i].getName());
			if (current.equals(info[i].getClassName()))
			{
				item.setSelected(true);
			}
			else
			{
				item.setSelected(false);
			}
			item.putClientProperty("command", "lnf");
			item.putClientProperty("class", info[i].getClassName());
			item.addActionListener(this);
			lnf.add(item);
		}
		result.add(lnf);
		return result;
	}

	public void updateToolsMenu()
	{
		int count = this.panelMenus.size();
		for (int i=0; i < count; i++)
		{
			JMenu tools = this.getMenu(ResourceMgr.MNU_TXT_TOOLS, i);

			int toolCount = tools.getItemCount();

			for (int ti = 0; ti < toolCount; ti++)
			{
				JMenuItem titem = tools.getItem(ti);
				if (titem == null) continue;
				if (!"lnf".equals(titem.getName())) continue;
				if (!(titem instanceof JMenu)) continue;

				JMenu lnf = (JMenu)titem;

				String current = UIManager.getLookAndFeel().getClass().getName();
				int items = lnf.getItemCount();
				for (int j=0; j < items; j++)
				{
					JMenuItem item = lnf.getItem(j);
					if (item instanceof JCheckBoxMenuItem)
					{
						String lnfclass = (String)item.getClientProperty("class");

						if (current.equals(lnfclass))
						{
							item.setSelected(true);
						}
						else
						{
							item.setSelected(false);
						}
					}
				}
			}
		}
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
		boolean saveIt = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgAttachWorkspaceToProfile"));
		if (saveIt)
		{
			this.assignWorkspace();
			assigned = true;
		}
		return assigned;
	}

	/**
	 *	Creates or removes SQL tabs until newCount tabs are displayed
	 */
	private void adjustTabCount(int newCount)
	{
		this.sqlTab.setSuspendRepaint(true);
		int tabCount = this.sqlTab.getTabCount();
		if (this.dbExplorerTabVisible) tabCount --;

		if (newCount > tabCount)
		{
			for (int i=0; i < (newCount - tabCount); i++)
			{
				this.addTab(false, false);
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
	 	if (!f.exists())
		{
			// if the file does not exist, we are setting all
			// variables as it would. Thus the file will be automatically
			// created...
			this.currentWorkspaceFile = realFilename;
			this.workspaceLoaded = true;
			this.updateWindowTitle();
			this.checkWorkspaceActions();
			return true;
		}

		this.sqlTab.setSuspendRepaint(true);
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
				MainPanel p = this.getSqlPanel(i);
				if (p instanceof SqlPanel)
				{
					SqlPanel sql = (SqlPanel)p;
					sql.closeFile(true);
					sql.clearSqlStatements();
					sql.readFromWorkspace(w);
					sql.setTabTitle(this.sqlTab, i);
				}
			}
			this.currentWorkspaceFile = realFilename;
			index = w.getSelectedTab();
			result = true;

			if (index < this.sqlTab.getTabCount())
			{
				this.sqlTab.setSelectedIndex(index);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + filename + ": " + e.getMessage(), e);
			this.currentWorkspaceFile = null;
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
			this.sqlTab.setSuspendRepaint(false);
		}

		this.workspaceLoaded = true;
		this.updateWindowTitle();
		this.checkWorkspaceActions();
		return result;
	}

	/**
	 *	Sets the default title for all tab titles
	 */
	private void resetTabTitles()
	{
		String defaultTitle = ResourceMgr.getString("LabelTabStatement");
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)p;
				sql.closeFile(true);
				this.setTabTitle(i, defaultTitle);
			}
		}
	}

	public String getCurrentWorkspaceFile()
	{
		return this.currentWorkspaceFile;
	}

	/**
	 *	Closes the current workspace.
	 *  The tab count is reset to 1, the SQL history for the tab will be emptied
	 *  and the workspace filename be "forgotten".
	 */
	public void closeWorkspace()
	{
		this.currentWorkspaceFile = null;
		this.isProfileWorkspace = false;
		this.workspaceLoaded = false;
		try
		{
			this.adjustTabCount(1);
			this.resetTabTitles();
			SqlPanel sql = (SqlPanel)this.getSqlPanel(0);
			sql.clearSqlStatements();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.closeWorkspace()", "Error when resetting workspace", e);
		}
		this.updateWindowTitle();
		this.checkWorkspaceActions();
	}

	/**
	 *	This will assigne the current workspace name to the current profile.
	 */
	public void assignWorkspace()
	{
		if (this.currentWorkspaceFile == null) return;
		if (this.currentProfile == null) return;
		String filename = WbManager.getInstance().putConfigDirKey(this.currentWorkspaceFile);
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		this.currentProfile.setWorkspaceFile(filename);
		this.isProfileWorkspace = true;
		this.updateWindowTitle();
	}

	/**
	 *	Returns true if at least one of the SQL panels is currently
	 *  executing a SQL statement.
	 *  This method calls isBusy() for each tab.
	 */
	public boolean isBusy()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isBusy()) return true;
		}
		return false;
	}

	/**
	 *	Save the currently loaded workspace
	 */
	public void saveWorkspace()
	{
		if (this.currentWorkspaceFile != null)
		{
			this.saveWorkspace(this.currentWorkspaceFile);
		}
	}


	/**
	 *	Saves the current SQL history to a workspace with the given filename
	 *  If filename == null, a SaveAs dialog will be displayed.
	 *  If the workspace is saved with a new name (filename == null) the user
	 *  will be asked if the workspace should be assigned to the current profile
	 */
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

		File f = new File(realFilename);
		if (WbManager.getSettings().getCreateWorkspaceBackup())
		{
			File bck = new File(realFilename + ".bck");
			try
			{
				bck.delete();
				f.renameTo(bck);
			}
			catch (Exception e)
			{
				LogMgr.logWarning("MainWindow.saveWorkspace()", "Error when creating backup file!", e);
			}
		}

		try
		{
			int count = this.sqlTab.getTabCount();

			for (int i=0; i < count; i++)
			{
				if (this.sqlTab.getComponentAt(i) instanceof SqlPanel)
				{
					SqlPanel sql = (SqlPanel)this.sqlTab.getComponentAt(i);
					sql.prepareWorkspaceSaving();
				}
			}

			String defaultLabel = ResourceMgr.getString("LabelTabStatement");
			w = new WbWorkspace(realFilename, true);
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

					if (!sql.hasFileLoaded())
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

		this.currentWorkspaceFile = filename;

		if (interactive)
		{
			this.checkMakeProfileWorkspace();
		}
		this.updateWindowTitle();
		this.checkWorkspaceActions();
	}

	/**
	 *	Invoked when the a different SQL panel has been selected
	 *  This fires the tabSelected() method
	 *  @param e  a ChangeEvent object
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
	 *
	 *  @param selectNew if true the new tab is automatically selected
	 *
	 */
	public void addTab(boolean selectNew)
	{
		this.addTab(selectNew, true);
	}


	/**
	 *	Adds a new SQL tab to the main window. This will be inserted
	 *	before the DbExplorer (if that is displayed as a tab)
	 *
	 *  @param selectNew if true the new tab is automatically selected
	 *  @param checkConnection if true, the panel will automatically be connected
	 *  this is important if a Profile is used where each panel gets its own
	 *  connection
	 */
	public void addTab(boolean selectNew, boolean checkConnection)
	{
		int index = this.sqlTab.getTabCount();

		if (index > 0 && this.getSqlPanel(index - 1) instanceof DbExplorerPanel)
		{
			index --;
		}
		SqlPanel sql = new SqlPanel(index + 1);
		sql.addDbExecutionListener(this);
		sql.addFilenameChangeListener(this);
		if (checkConnection) this.checkConnectionForPanel(sql, false);
		this.sqlTab.add(sql, index);
		this.setTabTitle(index, ResourceMgr.getString("LabelTabStatement"));

		JMenuBar menuBar = this.getMenuForPanel(sql);
		this.panelMenus.add(index, menuBar);

		SelectTabAction a = new SelectTabAction(this.sqlTab, index);
		this.addToViewMenu(a);

		if (selectNew) this.sqlTab.doLayout();

		// initDefaults has to be called after doLayout()!!!
		sql.initDefaults();
		if (selectNew) sqlTab.setSelectedIndex(index);

		this.setMacroMenuEnabled(sql.isConnected());
	}

	/**
	 *	Returns the real title of a tab (without the index number)
	 */
	private String getPlainTabTitle(int index)
	{
		String title = this.sqlTab.getTitleAt(index);
		int pos = title.lastIndexOf(' ');
		if (pos > -1)
			title = title.substring(0, pos);

		return title;
	}

	/**
	 *	Sets the tible of a tab and appends the index number to
	 *  the title, so that a shortcut Ctrl-n can be defined
	 */
	private void setTabTitle(int anIndex, String aName)
	{
		MainPanel p = this.getSqlPanel(anIndex);
		if (p instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)p;
			sql.setTabName(aName);
			sql.setTabTitle(this.sqlTab, anIndex);
		}
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

	public boolean canCloseTab()
	{
		int numTabs = this.sqlTab.getTabCount();
		if (this.dbExplorerTabVisible) numTabs --;
		return numTabs > 1;
	}

	public boolean canRenameTab()
	{
		boolean canRename = (this.currentWorkspaceFile != null);
		MainPanel p = this.getCurrentPanel();
		if (p instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)p;
			canRename = canRename && (!sql.hasFileLoaded());
		}
		else
		{
			canRename = false;
		}
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
		}
		this.selectCurrentEditorLater();
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
		int newTab = -1;

		this.setConnectInProgress();
		try
		{

			if (this.currentProfile != null && this.currentProfile.getUseSeperateConnectionPerTab())
			{
				WbConnection conn = panel.getConnection();
				if (conn != null)
				{
					final String id = conn.getId();
					Thread t = new Thread()
					{
						public void run()
						{
							WbManager.getInstance().getConnectionMgr().disconnect(id);
						}
					};
					t.setName("Disconnect thread for " + id);
					t.setDaemon(true);
					t.start();
				}
			}

			if (panel instanceof DbExplorerPanel)
			{
				((DbExplorerPanel)panel).saveSettings();
				this.dbExplorerTabVisible = false;
				this.dbExplorerPanel = null;
			}

			panel.disconnect();
			panel.dispose();

			this.panelMenus.remove(index);
			this.sqlTab.remove(index);
			this.removeFromViewMenu(index);

			int count = this.sqlTab.getTabCount();
			for (int i=index; i < count; i++)
			{
				MainPanel p = this.getSqlPanel(i);
				p.setTabTitle(this.sqlTab, i);
			}
			newTab = this.sqlTab.getSelectedIndex();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindows.removeTab()", "Error removing tab index=" + index,e);
		}
		finally
		{
			this.clearConnectInProgress();
		}
		if (newTab >= 0) this.tabSelected(newTab);
	}

	/**
	 *	Some menu items are built without an Action, so the window
	 *  is registered as the ActionListener...
	 *	These menu items are:
	 *	- Change look and feel
	 *  - What's new
	 *  - Help
	 *  - About
	 *  - Tools/Options
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
					}
					for (int i=0; i < this.sqlTab.getTabCount(); i ++)
					{
						JMenuBar menu = (JMenuBar)this.panelMenus.get(i);
						SwingUtilities.updateComponentTreeUI(menu);
					}
					try
					{
						this.updateToolsMenu();
					}
					catch (Exception upe)
					{
						LogMgr.logWarning("MainWindow.actionPerformed()", "Error updating LNF menu", upe);
					}
				}
				catch (Exception ex)
				{
					LogMgr.logError("MainWindow.actionPerformed()", "Could not change look and feel", ex);
				}
			}
			else if ("whatsNew".equals(command))
			{
				new WhatsNewViewer(this).show();
			}
			else if ("optionsDialog".equals(command))
			{
				SettingsPanel panel = new SettingsPanel();
				panel.showSettingsDialog(this);
			}
			else if ("helpContents".equals(command))
			{
				this.showHelp();
			}
			else if ("keyboardDialog".equals(command))
			{
				final ShortcutEditor editor = new ShortcutEditor(this);
				SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							editor.showWindow();
						}
					});
			}
			else if ("helpAbout".equals(command))
			{
				WbAboutDialog about = new WbAboutDialog(this, true);
				WbSwingUtilities.center(about, this);
				about.show();
			}
		}
	}

	public void showHelp()
	{
		WbManager.getInstance().getHelpViewer().showIndex();
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

	public void executionEnd(WbConnection conn, Object source)
	{
		this.runningJobs --;
		this.updateWindowTitle();
	}

	public void executionStart(WbConnection conn, Object source)
	{
		if (WbManager.getSettings().getAutoSaveWorkspace())
		{
			this.saveWorkspace();
		}
		this.runningJobs ++;
		this.updateWindowTitle();
	}

}