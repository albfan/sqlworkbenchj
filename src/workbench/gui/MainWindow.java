/*
 * MainWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
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
import workbench.util.ExceptionUtil;
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
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.NewDbExplorerWindowAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.actions.SaveAsNewWorkspaceAction;
import workbench.gui.actions.SaveWorkspaceAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.ShowDbExplorerAction;
import workbench.gui.actions.VersionCheckAction;
import workbench.gui.actions.ViewLineNumbers;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.help.HtmlViewer;
import workbench.gui.help.WhatsNewViewer;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.settings.SettingsPanel;
import workbench.gui.settings.ShortcutEditor;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.MacroManager;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;
import workbench.gui.actions.FileSaveProfiles;
import workbench.gui.dialogs.WbAboutDialog;

/**
 * The main window for the Workbench.
 * It will display several {@link workbench.gui.sql.SqlPanel}s in
 * a tabbed pane. Additionally one or more
 * {@link workbench.gui.dbobjects.DbExplorerPanel} might also be displayed
 * inside the JTabbedPane
 *
 * @author  support@sql-workbench.net
 */
public class MainWindow
	extends JFrame
	implements ActionListener, MouseListener, WindowListener, ChangeListener, DropTargetListener,
						FilenameChangeListener, MacroChangeListener, DbExecutionListener, Connectable
{
	private static final String DEFAULT_WORKSPACE = "%ConfigDir%/Default.wksp";
	private static int instanceCount;
	private String windowId;

	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;
	private ConnectionSelector connectionSelector;

	private FileDisconnectAction disconnectAction;
	private ShowDbExplorerAction dbExplorerAction;
	private NewDbExplorerPanelAction newDbExplorerPanel;
	private NewDbExplorerWindowAction newDbExplorerWindow;

	private WbTabbedPane sqlTab = new WbTabbedPane();
	private WbToolbar currentToolbar;
	private ArrayList panelMenus = new ArrayList(5);
	private int nextConnectionId = 0;

	private String currentWorkspaceFile = null;

	private CloseWorkspaceAction closeWorkspaceAction;
	private SaveWorkspaceAction saveWorkspaceAction;
	private SaveAsNewWorkspaceAction saveAsWorkspaceAction;
	private LoadWorkspaceAction loadWorkspaceAction;
	private AssignWorkspaceAction assignWorkspaceAction;

	private boolean isProfileWorkspace = false;
	private boolean workspaceLoaded = false;
	private boolean tabRemovalInProgress = false;

	// will indicate a connect or disconnect in progress
	// connecting and disconnecting is done a separate thread
	// so that slow connections do not block the GUI
	private boolean connectInProgress = false;

	private AddMacroAction createMacro;
	private ManageMacroAction manageMacros;
	private List explorerWindows = new ArrayList();

	private int runningJobs = 0;

	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);
		instanceCount ++;
		this.connectionSelector = new ConnectionSelector(this, this);
		this.windowId = "WbWin-" + Integer.toString(instanceCount);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);

		this.sqlTab.setUI(TabbedPaneUIFactory.getBorderLessUI());
		this.sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.sqlTab.setDoubleBuffered(true);

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

		// this is necessary to initialize the size of the panel!
		// so that sql.initDefaults() will actually be able to
		// to set the divider at 50%
		this.pack();

		this.restoreSettings();

		this.sqlTab.setSelectedIndex(0);
		this.updateGuiForTab(0);

		this.updateWindowTitle();
		this.checkWorkspaceActions();

		this.sqlTab.addChangeListener(this);
		this.sqlTab.addMouseListener(this);
		//this.sqlTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		MacroManager.getInstance().addChangeListener(this);

		new DropTarget(this.sqlTab, DnDConstants.ACTION_COPY, this);
	}

	/**
	 * Return the internal ID of this window. This ID will be used
	 * to generate the ID for each connection done from within this
	 * window.
	 */
	public String getWindowId() { return this.windowId; }

	/**
	 * The listener will be notified when the name of a tab changes.
	 * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
	 * to display the available panels in the context menu
	 * @see workbench.gui.dbobjects.TableListPanel#fileNameChanged(Object, String)
	 */
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

	/**
	 * The listener will be notified when the current tab changes.
	 * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
	 * to highlight the current tab the context menu
	 * @see workbench.gui.dbobjects.TableListPanel#stateChanged(ChangeEvent)
	 */
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
		this.createMacro = new AddMacroAction();
		this.createMacro.setEnabled(false);
		this.manageMacros = new ManageMacroAction(this);

		this.dbExplorerAction = new ShowDbExplorerAction(this);
		this.dbExplorerAction.setEnabled(false);

		this.newDbExplorerPanel = new NewDbExplorerPanelAction(this);
		this.newDbExplorerPanel.setEnabled(false);

		this.newDbExplorerWindow = new NewDbExplorerWindowAction(this);
		this.newDbExplorerWindow.setEnabled(false);

		int tabCount = this.sqlTab.getTabCount();
		for (int tab=0; tab < tabCount; tab ++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(tab);
			JMenuBar menuBar = this.createMenuForPanel(sql);
			this.panelMenus.add(menuBar);
		}
	}

	private JMenuBar createMenuForPanel(MainPanel aPanel)
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

		action = new FileSaveProfiles();
		action.addToMenu(menu);

		action = new FileNewWindowAction();
		menu.addSeparator();
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

		WbMenu submenu = null;
		String menuName = null;
		for (int i=0; i < actions.size(); i++)
		{
			submenu = null;
			action = null;
			menuName = null;
			Object entry = actions.get(i);
			boolean menuSep = false;
			if (entry instanceof WbAction)
			{
				action = (WbAction)actions.get(i);
				menuName = (String)action.getValue(WbAction.MAIN_MENU_ITEM);
				menuSep = "true".equals((String)action.getValue(WbAction.MENU_SEPARATOR));
			}
			else if (entry instanceof WbMenu)
			{
				submenu = (WbMenu)entry;
				menuName = submenu.getParentMenuId();
				menuSep = submenu.getCreateMenuSeparator();
			}

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

			if (menuSep)
			{
				menu.addSeparator();
			}
			if (action != null)
			{
				action.addToMenu(menu);
			}
			else if (submenu != null)
			{
				menu.add(submenu);
			}
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
		for (int i=0; (i < count && i < 9); i++)
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

		int realCount = 0;
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				realCount ++;
			}
		}

		String[] result = new String[realCount];
		for (int i=0; i < realCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (i < 9)
			{
				result[i] = p.getTabTitle() + " &" + Integer.toString(i+1);
			}
			else
			{
				result[i] = p.getTabTitle() + " " + Integer.toString(i+1);
			}
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

	private void checkConnectionForPanel(MainPanel aPanel)
	{
		this.checkConnectionForPanel(aPanel, true);
	}

	private Object connectProgressLock = new Object();
	
	private boolean isConnectInProgress()
	{
		synchronized (connectProgressLock)
		{
			return this.connectInProgress;
		}
	}
	private void clearConnectIsInProgress()
	{
		synchronized (connectProgressLock)
		{
			this.connectInProgress = false;
		}
	}

	private void setConnectIsInProgress() 
	{ 
		synchronized (connectProgressLock)
		{
			this.connectInProgress = true; 
		}
	}

	private void checkConnectionForPanel(final MainPanel aPanel, boolean createConnection)
	{
		if (aPanel.isConnected()) return;
		if (this.isConnectInProgress()) return;

		try
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab() && createConnection)
			{
				WbThread t = new WbThread(new Runnable()
				{
					public void run()
					{
						connectPanel(aPanel);
					}
				}, "Panel Connect " + aPanel.getId());
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
		if (this.isConnectInProgress()) return;
		this.showConnectingInfo();
		try
		{
			final WbConnection conn = this.getConnectionForTab(aPanel);
			final int index = this.getIndexForPanel(aPanel);
			this.tabConnected(aPanel, conn, index);
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.connectPanel()", "Error when connecting SQL panel " + aPanel.getId(), e);
			closeConnectingInfo();
			showStatusMessage("");
			String error = ExceptionUtil.getDisplay(e);
			String msg = ResourceMgr.getString("ErrorConnectFailed").replaceAll("%msg%", error.trim());
			WbSwingUtilities.showErrorMessage(this, msg);
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
		if (anIndex < 0) return;

		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();
		if (current == null) return;

		JMenuBar menu = (JMenuBar)this.panelMenus.get(anIndex);
		if (menu == null) return;

		this.setJMenuBar(menu);

		if (this.currentToolbar != null) content.remove(this.currentToolbar);
		if (current != null)
		{
			this.currentToolbar = current.getToolbar();
			content.add(this.currentToolbar, BorderLayout.NORTH);
		}
		current.panelSelected();
		this.setMacroMenuEnabled(current.isConnected());
		this.doLayout();
	}

	private void tabSelected(int anIndex)
	{
		
		Container content = this.getContentPane();
		MainPanel current = this.getCurrentPanel();
		
		this.updateGuiForTab(anIndex);
		this.updateAddMacroAction();
		this.updateWindowTitle();
		if (!this.isConnectInProgress()) 
		{
			this.checkConnectionForPanel(current);
		}
	}

	private void updateAddMacroAction()
	{
		MainPanel current = this.getCurrentPanel();
		if (current instanceof SqlPanel)
		{
			SqlPanel sql = (SqlPanel)current;
			sql.selectEditorLater();
			this.createMacro.setClient(sql.getEditor());
		}
	}

	public void restoreState()
	{
		String state = Settings.getInstance().getProperty(this.getClass().getName() + ".state", "0");
		int i = 0;
		try { i = Integer.parseInt(state); } catch (Exception e) { i = 0; }
		if (i == MAXIMIZED_BOTH)
		{
			this.setExtendedState(i);
		}
	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();

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
    Settings sett = Settings.getInstance();
		int state = this.getExtendedState();
		sett.setProperty(this.getClass().getName() + ".state", state);

		if (state != MAXIMIZED_BOTH)
		{
			sett.storeWindowPosition(this);
			sett.storeWindowSize(this);
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
//		if (this.dbExplorerPanel != null)
//		{
//			this.dbExplorerPanel.mainWindowDeiconified();
//		}
	}

	public void windowClosing(WindowEvent windowEvent)
	{
		WbManager.getInstance().windowClosing(this);
	}

	public void windowDeactivated(WindowEvent windowEvent)
	{
		SqlPanel p = this.getCurrentSqlPanel();
		if (p != null) p.checkFocus();
	}

	public void windowActivated(WindowEvent windowEvent)
	{
		JMenu m1 = null;
		int index = this.getCurrentPanelIndex();
		if (this.panelMenus != null && index > -1)
		{
			JMenuBar b = (JMenuBar)this.panelMenus.get(index);
			if (b != null)
			{
				try
				{
					m1 = b.getMenu(0);
				}
				catch (Throwable th)
				{
					m1 = null;
				}
			}
		}
		final SqlPanel p = this.getCurrentSqlPanel();
		final JMenu m = m1;
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (m != null) m.setSelected(false);
				if (p != null) p.restoreFocus();			}
		});
	}

	public void windowIconified(WindowEvent windowEvent)
	{
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

	public void connectBegin(final ConnectionProfile aProfile)
	{
		if (this.currentWorkspaceFile != null)
		{
			this.saveWorkspace(this.currentWorkspaceFile, true);
		}
		disconnect(false, false);

		showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));

		// it is important to set this flag, otherwise
		// loading the workspace will already trigger a
		// panel switch which might cause a connect
		// to the current profile before the ConnectionSelector
		// has actually finished.
		// this has to be set AFTER the disconnect, because
		// disconnect respects this flag and does nothing...
		this.setConnectIsInProgress();
		this.currentProfile = aProfile;
		Settings.getInstance().setLastConnection(this.currentProfile.getName());

		loadWorkspaceForProfile(aProfile);
		showStatusMessage(ResourceMgr.getString("MsgConnecting"));
	}

	private String getConnectionIdForPanel(MainPanel p)
	{
		return "Wb" + MainWindow.instanceCount + "-" + p.getId();
	}

	public String getConnectionId(ConnectionProfile aProfile)
	{
		String id;

		if (aProfile.getUseSeparateConnectionPerTab())
		{
			MainPanel p = this.getCurrentPanel();
			id = this.getConnectionIdForPanel(p);
		}
		else
		{
			id = this.getWindowId();
		}
		return id;
	}

	public void connectTo(ConnectionProfile profile, boolean showDialog)
	{
		this.connectionSelector.connectTo(profile, showDialog);
	}

	/**
	 *	Call-back function which gets executed on the AWT thread after
	 *  the initial connection has been completed
	 */
	public void connected(WbConnection conn)
	{
		this.showStatusMessage("");
		
		this.currentProfile = conn.getProfile();
		if (this.currentProfile.getUseSeparateConnectionPerTab())
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
		this.newDbExplorerPanel.setEnabled(true);
		this.newDbExplorerWindow.setEnabled(true);

		this.disconnectAction.setEnabled(true);
		selectCurrentEditorLater();
		this.getCurrentPanel().clearLog();
		this.getCurrentPanel().showResultPanel();

		String warn = conn.getWarnings(true);
		if (warn != null)
		{
			this.getCurrentPanel().showLogMessage(warn);
		}
	}

	public void connectFailed(String error)
	{
		this.showStatusMessage("");
		this.setMacroMenuEnabled(false);
		this.updateWindowTitle();
		this.dbExplorerAction.setEnabled(false);
		this.newDbExplorerPanel.setEnabled(false);
		this.newDbExplorerWindow.setEnabled(false);
		this.disconnectAction.setEnabled(false);
		try
		{
			String msg = ResourceMgr.getString("ErrorConnectFailed");
			msg = StringUtil.replace(msg, "%msg%", error.trim());
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.connectFailed()", "Could not display connection error!", th);
			WbSwingUtilities.showErrorMessage(this, error);
		}
	}

	public void connectCancelled()
	{
		this.showStatusMessage("");
	}

	public void connectEnded()
	{
		this.clearConnectIsInProgress();
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
		dialog.setVisible(true);
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return CREATE_WORKSPACE;
		else if (result.equals(options[0])) return CREATE_WORKSPACE;
		else if (result.equals(options[1])) return LOAD_OTHER_WORKSPACE;
		else return IGNORE_MISSING_WORKSPACE;
	}

	private void handleWorkspaceLoadError(Throwable e, String realFilename)
	{
		String error = ExceptionUtil.getDisplay(e);
		String msg = StringUtil.replace(ResourceMgr.getString("ErrorLoadingWorkspace"), "%error%", error);
		if (e instanceof OutOfMemoryError)
		{
			// try to free memory...
			System.gc();
			msg = ResourceMgr.getString("MsgOutOfMemoryError");
		}
		boolean create = WbSwingUtilities.getYesNo(this, msg);
		if (create)
		{
			this.currentWorkspaceFile = realFilename;
			this.workspaceLoaded = true;
		}
		else
		{
			this.workspaceLoaded = false;
			this.currentWorkspaceFile = null;
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
		FileDialogUtil util = new FileDialogUtil();
		String realFilename = util.replaceConfigDir(filename);

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
			this.closeExplorerPanels();
			w = new WbWorkspace(realFilename, false);
			int entryCount = w.getEntryCount();
			if (entryCount == 0) entryCount = 1;
			//this.sqlTab.setSelectedIndex(0);
			this.adjustTabCount(entryCount);
			//this.sqlTab.setSelectedIndex(0);

			for (int i=0; i < entryCount; i++)
			{
				MainPanel p = this.getSqlPanel(i);
				if (p instanceof SqlPanel)
				{
					SqlPanel sql = (SqlPanel)p;
					sql.closeFile(true);
					sql.clearSqlStatements();
					sql.readFromWorkspace(w,i);
					sql.setTabTitle(this.sqlTab, i);
					updateViewMenu(i, getPlainTabTitle(i));
				}
			}
			this.currentWorkspaceFile = realFilename;

			int explorerCount = w.getDbExplorerVisibleCount();
			this.adjustDbExplorerCount(explorerCount);
			int count = this.sqlTab.getTabCount();
			for (int i=0; i < count; i++)
			{
				MainPanel p = this.getSqlPanel(i);
				if (p instanceof DbExplorerPanel)
				{
					p.readFromWorkspace(w,i);
				}
			}

			this.workspaceLoaded = true;
			result = true;
			
			int newIndex = w.getSelectedTab();
			if (newIndex < this.sqlTab.getTabCount())
			{
				// the stateChanged event will be ignored as we
				// have the repainting for the tab suspended
				sqlTab.setSelectedIndex(newIndex);
				tabSelected(newIndex);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + realFilename, e);
			this.handleWorkspaceLoadError(e, realFilename);
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
			this.sqlTab.setSuspendRepaint(false);
		}


		this.doLayout();
		this.validate();

		this.updateWindowTitle();
		this.checkWorkspaceActions();
		this.updateAddMacroAction();
		return result;
	}

	private void loadWorkspaceForProfile(ConnectionProfile aProfile)
	{
		String realFilename = null;
		try
		{
			String workspaceFilename = aProfile.getWorkspaceFile();
			FileDialogUtil util = new FileDialogUtil();
			realFilename = util.replaceConfigDir(workspaceFilename);
			if (realFilename == null) realFilename = "";

			File f = new File(realFilename);
			if (realFilename.length() > 0 && !f.exists())
			{
				int action = this.checkNonExistingWorkspace();
				if (action == LOAD_OTHER_WORKSPACE)
				{
					workspaceFilename = util.getWorkspaceFilename(this, false, true);
					aProfile.setWorkspaceFile(workspaceFilename);
				}
				else if (action == IGNORE_MISSING_WORKSPACE)
				{
					workspaceFilename = null;
					aProfile.setWorkspaceFile(null);
				}
			}

			if (workspaceFilename != null && workspaceFilename.trim().length() > 0)
			{
				// loadWorkspace will replace the %ConfigDir% placeholder,
				// so we need to pass the original filename
				this.isProfileWorkspace = true;
				this.loadWorkspace(workspaceFilename);
			}
			else
			{
				this.loadDefaultWorkspace();
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.loadWorkspaceForProfile()", "Error reading workspace " + realFilename, e);
			this.handleWorkspaceLoadError(e, realFilename);
		}
	}

	public void disconnect(boolean background, final boolean closeWorkspace)
	{
		if (this.isConnectInProgress()) return;
		this.setConnectIsInProgress();

		if (background)
		{
			Thread t = new WbThread("MainWindow Disconnect")
			{
				public void run()
				{
					if (closeWorkspace) saveWorkspace(false);
					showDisconnectInfo();
					doDisconnect();
					if (closeWorkspace) closeWorkspace();
					closeConnectingInfo();
				}
			};
			t.start();
		}
		else
		{
			if (closeWorkspace) saveWorkspace(false);
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
			ConnectionMgr mgr = ConnectionMgr.getInstance();
			WbConnection conn = null;

			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					((SqlPanel)sql).abortExecution();
				}
				conn = sql.getConnection();
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
					}
				});
				sql.disconnect();
				if (conn != null) mgr.disconnect(conn.getId());
			}
			this.closeExplorerWindows(true);
		}
		finally
		{
			this.disconnected();
			EventQueue.invokeLater(new Runnable()
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
		this.dbExplorerAction.setEnabled(false);
		this.newDbExplorerPanel.setEnabled(false);
		this.newDbExplorerWindow.setEnabled(false);
		this.clearConnectIsInProgress();
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
		int showTitle = Settings.getInstance().getShowFilenameInWindowTitle();
		if (showTitle != Settings.SHOW_NO_FILENAME)
		{
			MainPanel p  = this.getCurrentPanel();
			if (p instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)p;
				String file = sql.getCurrentFileName();
				if (file != null)
				{
					title.append(" - ");
					if (showTitle == Settings.SHOW_FULL_PATH)
					{
						title.append(file);
					}
					else
					{
						File f = new File(file);
						title.append(f.getName());
					}
				}
			}
		}
		this.setTitle(title.toString());
	}

	private void closeConnectingInfo()
	{
		this.connectionSelector.closeConnectingInfo();
	}

	private void showDisconnectInfo()
	{
		this.connectionSelector.showDisconnectInfo();
	}

	/** Display a little PopupWindow to tell the user that the
	 *  workbench is currently connecting to the DB
	 */
	private void showConnectingInfo()
	{
		this.connectionSelector.showConnectingInfo();
	}

	private void setConnection(WbConnection con)
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
			sql.setConnection(con);
		}
		this.currentConnection = con;
		if (this.currentProfile == null) this.currentProfile = con.getProfile();
	}

	public void selectConnection()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				connectionSelector.selectConnection();
			}
		});
	}

	public JMenu getMacroMenu(int panelIndex)
	{
		JMenu menu = this.getMenu(ResourceMgr.MNU_TXT_MACRO, panelIndex);
		return menu;
	}

	public JMenu getViewMenu(int panelIndex)
	{
		return this.getMenu(ResourceMgr.MNU_TXT_VIEW, panelIndex);
	}

	public JMenu getMenu(String aName, int panelIndex)
	{
		if (panelIndex < 0 || panelIndex >= this.panelMenus.size()) return null;
		JMenuBar menubar = (JMenuBar)this.panelMenus.get(panelIndex);
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
		if (aName == null) aName = ResourceMgr.getDefaultTabLabel();
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
			view.validate();
			view.repaint();
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
		String id = this.getConnectionIdForPanel(aPanel);;
		aPanel.showStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + this.currentProfile.getName() + " ...");
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		WbConnection conn = null;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			conn = mgr.getConnection(this.currentProfile, id);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
		return conn;
	}

	public void addDbExplorerTab(DbExplorerPanel explorer)
	{
		JMenuBar dbmenu = this.createMenuForPanel(explorer);

		this.sqlTab.add(explorer);
		explorer.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);

		SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);
		action.setName(explorer.getTabTitle());
		this.panelMenus.add(dbmenu);
		this.addToViewMenu(action);
	}

	/**
	 * Displays the DbExplorer. Either in a separate tab,
	 * or as a new window. If an explorer is already open,
	 * that instance will be re-used
	 */
	public void showDbExplorer()
	{
		boolean useTab = Settings.getInstance().getShowDbExplorerInMainWindow();
		if (useTab)
		{
			int index = this.findFirstExplorerTab();
			if (index > -1)
			{
				this.selectTab(index);
			}
			else
			{
				this.newDbExplorerPanel(true);
			}
		}
		else
		{
			if (this.explorerWindows.size() > 0)
			{
				DbExplorerPanel p = (DbExplorerPanel)this.explorerWindows.get(0);
				p.activateWindow();
			}
			else
			{
				this.newDbExplorerWindow();
			}

		}
	}

	/**
	 * Returns the index of the las SQL Panel
	 */
	public int getLastSqlPanelIndex()
	{
		int explorer = findFirstExplorerTab();
		if (explorer == -1)
			return this.sqlTab.getTabCount() - 1;
		else
			return explorer -1;
	}

	/**
	 *	Returns the index of the first explorer tab
	 */
	private int findFirstExplorerTab()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=count - 1; i > 0; i--)
		{
			Component c = this.sqlTab.getComponentAt(i);
			if (c instanceof DbExplorerPanel)
			{
				return i;
			}
		}
		return -1;
	}

	public void closeExplorerWindows(boolean doDisconnect)
	{
		for (int i=0; i < this.explorerWindows.size(); i++)
		{
			DbExplorerPanel p = (DbExplorerPanel)this.explorerWindows.get(i);

			if (doDisconnect)
			{
				WbConnection conn = p.getConnection();
				if (conn != this.currentConnection)
				{
					ConnectionMgr.getInstance().disconnect(conn.getId());
				}
			}
			p.disconnect();
			p.closeWindow();
		}
	}

	public void closeExplorerPanels()
	{
		int index = this.findFirstExplorerTab();
		if (index < 0) return;
		int count = this.sqlTab.getTabCount();

		for (int i=index; i < count; i++)
		{
			this.removeTab(i);
		}
	}

	public void newDbExplorerWindow()
	{
		DbExplorerPanel explorer = new DbExplorerPanel(this);
		explorer.restoreSettings();
		explorer.openWindow(this.currentProfile.getName());
		if (this.currentProfile.getUseSeparateConnectionPerTab() || this.currentConnection == null)
		{
			explorer.connect(this.currentProfile);
		}
		else
		{
			explorer.setConnection(this.currentConnection);
		}
		this.explorerWindows.add(explorer);
	}

	public void explorerWindowClosed(DbExplorerPanel p)
	{
		this.explorerWindows.remove(p);
	}

	public void newDbExplorerPanel(boolean select)
	{
		DbExplorerPanel explorer = new DbExplorerPanel(this);
		explorer.restoreSettings();
		this.addDbExplorerTab(explorer);
		if (select)
		{
			// Switching to the new tab will initiate the connection if necessary
			this.sqlTab.setSelectedIndex(this.sqlTab.getTabCount() - 1);
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

	/**
	 * Create the tools menu for a panel menu. This will be called
	 * for each panel that gets added to the main window.
	 * Actions that are singletons (like the db explorer stuff
	 * should not be created here)
	 */
	public JMenu buildToolsMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TOOLS));
		result.setName(ResourceMgr.MNU_TXT_TOOLS);

		result.add(this.dbExplorerAction);
		result.add(this.newDbExplorerPanel);
		result.add(this.newDbExplorerWindow);
		result.addSeparator();

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

	private int getNumberOfExplorerPanels()
	{
		int count = 0;
		int num = this.sqlTab.getTabCount();
		for (int i=0; i < num; i++)
		{
			Component c = this.sqlTab.getComponentAt(i);
			if (c instanceof DbExplorerPanel) count++;
		}
		return count;
	}

	private void adjustDbExplorerCount(int newCount)
	{
		int count = this.getNumberOfExplorerPanels();
		if (count == newCount) return;
		if (newCount > count)
		{
			for (int i=0; i < (newCount - count); i++)
			{
				newDbExplorerPanel(false);
			}
		}
		else if (newCount < count)
		{
			for (int i=0; i < (count - newCount); i++)
			{
				this.removeLastTab(true);
			}
		}
	}
	/**
	 *	Creates or removes SQL tabs until newCount tabs are displayed
	 */
	private void adjustTabCount(int newCount)
	{
		boolean suspended = this.sqlTab.isRepaintSuspended();

		if (!suspended) this.sqlTab.setSuspendRepaint(true);
		try
		{
			int tabCount = this.sqlTab.getTabCount() - getNumberOfExplorerPanels();

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
					this.removeLastTab(newCount == 1);
				}
			}
		}
		finally
		{
			if (!suspended) this.sqlTab.setSuspendRepaint(false);
		}
	}

	private void removeAllTabs()
	{
		try
		{
			while (this.sqlTab.getTabCount() > 0)
			{
				this.removeLastTab(true);
			}
		}
		finally
		{
			//if (!suspended) this.sqlTab.setSuspendRepaint(false);
		}
	}


	/**
	 *	Sets the default title for all tab titles
	 */
	private void resetTabTitles()
	{
		String defaultTitle = ResourceMgr.getDefaultTabLabel();
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p == null) continue;
			if (p instanceof SqlPanel)
			{
				SqlPanel sql = (SqlPanel)p;
				sql.closeFile(true);
				this.setTabTitle(i, defaultTitle);
			}
		}
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

	public String getCurrentWorkspaceFile()
	{
		return this.currentWorkspaceFile;
	}

	public void loadWorkspace()
	{
		FileDialogUtil dialog = new FileDialogUtil();
		String filename = dialog.getWorkspaceFilename(this, false, true);
		if (filename == null) return;
		if (this.loadWorkspace(filename))
		{
			this.isProfileWorkspace = this.checkMakeProfileWorkspace();
		}
		else
		{
			this.isProfileWorkspace = false;
		}
		//this.updateWindowTitle();
	}

	/**
	 *	Closes the current workspace.
	 *  The tab count is reset to 1, the SQL history for the tab will be emptied
	 *  and the workspace filename will be "forgotten".
	 */
	public void closeWorkspace()
	{
		this.currentWorkspaceFile = null;
		this.isProfileWorkspace = false;
		this.workspaceLoaded = false;
		try
		{
			this.closeExplorerPanels();
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
		FileDialogUtil util = new FileDialogUtil();
		String filename = util.putConfigDirKey(this.currentWorkspaceFile);
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		this.currentProfile.setWorkspaceFile(filename);
		this.isProfileWorkspace = true;
		this.updateWindowTitle();
	}

	public boolean saveWorkspace()
	{
		return saveWorkspace(true);
	}
	/**
	 *	Save the currently loaded workspace
	 */
	public boolean saveWorkspace(boolean checkUnsaved)
	{
		if (this.currentWorkspaceFile != null)
		{
			return this.saveWorkspace(this.currentWorkspaceFile, checkUnsaved);
		}
		return true;
	}

	/**
	 *	Saves the current SQL history to a workspace with the given filename
	 *  If filename == null, a SaveAs dialog will be displayed.
	 *  If the workspace is saved with a new name (filename == null) the user
	 *  will be asked if the workspace should be assigned to the current profile
	 */
	public boolean saveWorkspace(String filename, boolean checkUnsaved)
	{
		WbWorkspace w = null;
		boolean interactive = false;
		boolean result = true;
		FileDialogUtil util = new FileDialogUtil();

		if (filename == null)
		{
			interactive = true;
			filename = util.getWorkspaceFilename(this, true);
			if (filename == null) return true;
		}

		String realFilename = util.replaceConfigDir(filename);

		File f = new File(realFilename);
		if (Settings.getInstance().getCreateWorkspaceBackup())
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

			if (checkUnsaved)
			{
				for (int i=0; i < count; i++)
				{
					MainPanel p = (MainPanel)this.sqlTab.getComponentAt(i);
					if (!p.prepareWorkspaceSaving()) return false;
				}
			}
			w = new WbWorkspace(realFilename, true);
			int selected = this.sqlTab.getSelectedIndex();
			w.setSelectedTab(selected);
			for (int i=0; i < count; i++)
			{
				MainPanel p = getSqlPanel(i);
				p.saveToWorkspace(w,i);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.saveWorkspace()", "Error saving workspace: " + filename, e);
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrorSavingWorkspace") + "\n" + ExceptionUtil.getDisplay(e));
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
		return true;
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
			if (this.tabRemovalInProgress || this.sqlTab.isRepaintSuspended()) return;
			int index = this.sqlTab.getSelectedIndex();
			this.tabSelected(index);
		}
	}

	public MainPanel addTab()
	{
		return this.addTab(true);
	}

	/**
	 *	Adds a new SQL tab to the main window. This will be inserted
	 *	before the DbExplorer (if that is displayed as a tab)
	 *
	 *  @param selectNew if true the new tab is automatically selected
	 *
	 */
	public MainPanel addTab(boolean selectNew)
	{
		return this.addTab(selectNew, true);
	}

	/**
	 *	Adds a new SQL tab to the main window. This will be inserted
	 *	before the first DbExplorer tab
	 *
	 *  @param selectNew if true the new tab is automatically selected
	 *  @param checkConnection if true, the panel will automatically be connected
	 *  this is important if a Profile is used where each panel gets its own
	 *  connection
	 */
	public MainPanel addTab(boolean selectNew, boolean checkConnection)
	{
		int index = this.findFirstExplorerTab();
		if (index == -1) index = sqlTab.getTabCount();
		SqlPanel sql = new SqlPanel(index+1);
		sql.addDbExecutionListener(this);
		sql.addFilenameChangeListener(this);
		if (checkConnection) this.checkConnectionForPanel(sql, false);

		boolean isSuspended = this.sqlTab.isRepaintSuspended();

		try
		{
			// suspending the repaint will also prevent
			// our own stateChanged event to carry out its work
			if (!isSuspended) this.sqlTab.setSuspendRepaint(true);

			// if the new tab index is lower then the currently
			// selected index, Swing will select the new index
			// as the new tab is not setup completely, we have to
			// prevent our own stateChanged event to do its work!
			JMenuBar menuBar = this.createMenuForPanel(sql);
			this.panelMenus.add(index, menuBar);

			this.sqlTab.add(sql, index);

			// setTabTitle needs to be called after adding the panel!
			// this will set the correct title with Mnemonics
			this.setTabTitle(index, ResourceMgr.getDefaultTabLabel());

			//initDefaults has to be called after doLayout()!!!
			if (selectNew) sqlTab.doLayout();
			sql.initDefaults();
			this.setMacroMenuEnabled(sql.isConnected());

			this.renumberTabs();
		}
		finally
		{
			if (!isSuspended) this.sqlTab.setSuspendRepaint(false);
		}

		// This needs to be done after disabling repaint suspend
		// otherwise the tab will not change properly!
		if (selectNew) 	this.sqlTab.setSelectedIndex(index);

		return sql;
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
	 *	Sets the title of a tab and appends the index number to
	 *  the title, so that a shortcut Ctrl-n can be defined
	 */
	private void setTabTitle(int anIndex, String aName)
	{
		MainPanel p = this.getSqlPanel(anIndex);
		p.setTabName(aName);
		p.setTabTitle(this.sqlTab, anIndex);
		this.updateViewMenu(anIndex, p.getTabTitle());
	}

	public void removeLastTab(boolean includeExplorer)
	{
		int index = this.sqlTab.getTabCount() - 1;
		MainPanel p;
		if (!includeExplorer)
		{
			while ((p = this.getSqlPanel(index)) instanceof DbExplorerPanel)
			{
				index --;
			}
		}
		this.removeTab(index);
	}

	public boolean canCloseTab()
	{
		int numTabs = this.sqlTab.getTabCount();
		return numTabs > 1;
	}

	public boolean canRenameTab()
	{
		boolean canRename = (this.currentWorkspaceFile != null);
		MainPanel p = this.getCurrentPanel();
//		if (p instanceof SqlPanel)
//		{
//			SqlPanel sql = (SqlPanel)p;
//			canRename = canRename && (!sql.hasFileLoaded());
//		}
//		else
//		{
//			canRename = false;
//		}
		canRename = canRename && (p instanceof SqlPanel);
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

	private void renumberTabs()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			p.setTabTitle(sqlTab, i);
		}
		for (int panel=0; panel < count; panel++)
		{
			rebuildViewMenu(panel);
		}
	}

	/**
	 * Rebuild the part of the view menu that handles the
	 * selecting of tabs
	 */
	private void rebuildViewMenu(int panel)
	{
		JMenu menu = this.getViewMenu(panel);
		JMenuItem item = menu.getItem(0);
		while (item != null && (item.getAction() instanceof SelectTabAction))
		{
			menu.remove(0);
			item = menu.getItem(0);
		}
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			SelectTabAction a = new SelectTabAction(sqlTab, i);
			a.setName(getPlainTabTitle(i));
			menu.insert(a, i);
		}
		if (this.sqlTab.getSelectedIndex() == panel)
		{
			menu.validate();
			menu.repaint();
		}
	}

	/**
	 * Moves the current sql tab to the left (i.e. index := index - 1)
	 * If index == 0 nothing happens
	 */
	public void moveTabLeft()
	{
		int index = this.getCurrentPanelIndex();
		if (index <= 0) return;
		SqlPanel p = (SqlPanel) this.getSqlPanel(index);
		this.sqlTab.remove(index);
		index --;
		this.sqlTab.add(p, index);
		this.sqlTab.setSelectedIndex(index);
		renumberTabs();
		this.validate();
	}

	/**
	 * Moves the current sql tab to the right (i.e. index := index + 1)
	 * If index denotes the last SQL Tab, nothing happens
	 */
	public void moveTabRight()
	{
		int index = this.getCurrentPanelIndex();
		int lastIndex = this.getLastSqlPanelIndex();
		if (index >= lastIndex) return;
		SqlPanel p = (SqlPanel) this.getSqlPanel(index);
		this.sqlTab.remove(index);
		//removeFromViewMenu(index);
		index ++;
		this.sqlTab.add(p, index);
		this.sqlTab.setSelectedIndex(index);
		SelectTabAction a = new SelectTabAction(this.sqlTab, index);
		a.setName(getPlainTabTitle(index));
		//addToViewMenu(a);
		renumberTabs();
		this.validate();
	}

	/**
	 * Removes the tab at the give location. If the current profile
	 * uses a separate connection per tab, then a disconnect will be
	 * triggered as well. This disconnect will be started in a
	 * background thread.
	 */
	public void removeTab(int index)
	{
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return;

		int newTab = -1;

		boolean inProgress = this.isConnectInProgress();
		if (!inProgress) this.setConnectIsInProgress();
		try
		{
			this.tabRemovalInProgress = true;
			WbConnection conn = panel.getConnection();

			panel.disconnect();
			try
			{
				panel.dispose();
			}
			catch (Throwable th)
			{
				LogMgr.logError("MainWindow.removeTab()", "Error when removing tab", th);
			}

			if (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab()
			   && conn != null)
			{
				final String id = conn.getId();
				Thread t = new WbThread("Panel " + panel.getId() + " disconnect thread")
				{
					public void run()
					{
						ConnectionMgr.getInstance().disconnect(id);
					}
				};
				t.start();
			}

			this.panelMenus.remove(index);
			this.sqlTab.remove(index);
			this.renumberTabs();
			newTab = this.sqlTab.getSelectedIndex();
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindows.removeTab()", "Error removing tab index=" + index,e);
		}
		finally
		{
			this.tabRemovalInProgress = false;
			if (!inProgress) this.clearConnectIsInProgress();
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
					WbManager.getInstance().changeLookAndFeel(className);
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
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						new WhatsNewViewer(MainWindow.this).setVisible(true);
					}
				});
			}
			else if ("optionsDialog".equals(command))
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						SettingsPanel panel = new SettingsPanel();
						panel.showSettingsDialog(MainWindow.this);
					}
				});
			}
			else if ("helpContents".equals(command))
			{
				this.showHelp();
			}
			else if ("keyboardDialog".equals(command))
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						ShortcutEditor editor = new ShortcutEditor(MainWindow.this);
						editor.showWindow();
					}
				});
			}
			else if ("helpAbout".equals(command))
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						WbAboutDialog about = new WbAboutDialog(MainWindow.this, true);
						WbSwingUtilities.center(about, MainWindow.this);
						about.setVisible(true);
					}
				});
			}
		}
	}

	public void showHelp()
	{
		try
		{
			// Use reflection to load the HtmlViewer in order to
			// avoid unnecessary class loading during startup
			Class cls = Class.forName("workbench.gui.help.HtmlViewer");
			Class[] types = new Class[] { JFrame.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { this };
			cons.newInstance(args);
		}
		catch (Exception ex)
		{
			LogMgr.logError("MainWindow.showHelp()", "Error when displaying HTML help", ex);
			JOptionPane.showMessageDialog(this, "The documentation is currently available at www.kellerer.org/workbench");
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

	public void executionEnd(WbConnection conn, Object source)
	{
		this.runningJobs --;
		this.updateWindowTitle();
	}

	public void executionStart(WbConnection conn, Object source)
	{
		if (Settings.getInstance().getAutoSaveWorkspace())
		{
			this.saveWorkspace();
		}
		this.runningJobs ++;
		this.updateWindowTitle();
	}

	public void dragEnter(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
		dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY);
	}

	public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
	{
	}

	public void dragOver(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

	public void drop(java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
	{
		try
		{
			Transferable tr = dropTargetDropEvent.getTransferable();
			if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
				java.util.List fileList = (java.util.List)tr.getTransferData(DataFlavor.javaFileListFlavor);
				if (fileList != null)
				{
					int files = fileList.size();
					for (int i=0; i < files; i++)
					{
						File file = (File)fileList.get(i);
						this.addTab(true);
						SqlPanel sql = this.getCurrentSqlPanel();
						sql.readFile(file.getAbsolutePath(), null);
					}
				}
			}
			else
			{
				dropTargetDropEvent.rejectDrop();
			}
		}
		catch (IOException io)
		{
			io.printStackTrace();
			dropTargetDropEvent.rejectDrop();
		}
		catch (UnsupportedFlavorException ufe)
		{
			ufe.printStackTrace();
			dropTargetDropEvent.rejectDrop();
		}
	}

	public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

}