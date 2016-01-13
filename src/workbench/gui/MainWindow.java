/*
 * MainWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Moveable;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.gui.actions.AboutAction;
import workbench.gui.actions.AddMacroAction;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.AssignWorkspaceAction;
import workbench.gui.actions.BookmarksAction;
import workbench.gui.actions.CloseWorkspaceAction;
import workbench.gui.actions.ConfigureShortcutsAction;
import workbench.gui.actions.CreateNewConnection;
import workbench.gui.actions.DataPumperAction;
import workbench.gui.actions.DisconnectTabAction;
import workbench.gui.actions.FileCloseAction;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileDisconnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.FileNewWindowAction;
import workbench.gui.actions.FileReconnectAction;
import workbench.gui.actions.FileSaveProfiles;
import workbench.gui.actions.HelpConnectionInfoAction;
import workbench.gui.actions.HelpContactAction;
import workbench.gui.actions.InsertTabAction;
import workbench.gui.actions.LoadMacrosAction;
import workbench.gui.actions.LoadWorkspaceAction;
import workbench.gui.actions.ManageDriversAction;
import workbench.gui.actions.ManageMacroAction;
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.NewDbExplorerWindowAction;
import workbench.gui.actions.NextTabAction;
import workbench.gui.actions.ObjectSearchAction;
import workbench.gui.actions.OpenFileAction;
import workbench.gui.actions.OptionsDialogAction;
import workbench.gui.actions.PrevTabAction;
import workbench.gui.actions.ReloadProfileWkspAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.SaveAsNewWorkspaceAction;
import workbench.gui.actions.SaveMacrosAction;
import workbench.gui.actions.SaveWorkspaceAction;
import workbench.gui.actions.SearchAllEditorsAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.ShowDbExplorerAction;
import workbench.gui.actions.ShowDbTreeAction;
import workbench.gui.actions.ShowDbmsManualAction;
import workbench.gui.actions.ShowHelpAction;
import workbench.gui.actions.ShowMacroPopupAction;
import workbench.gui.actions.ShowManualAction;
import workbench.gui.actions.VersionCheckAction;
import workbench.gui.actions.ViewLineNumbers;
import workbench.gui.actions.ViewLogfileAction;
import workbench.gui.actions.ViewToolbarAction;
import workbench.gui.actions.WbAction;
import workbench.gui.actions.WhatsNewAction;
import workbench.gui.bookmarks.BookmarkManager;
import workbench.gui.bookmarks.NamedScriptLocation;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.MenuScroller;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.TabCloser;
import workbench.gui.components.TabbedPaneHistory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.dbobjects.objecttree.DbTreePanel;
import workbench.gui.dbobjects.objecttree.DbTreeSettings;
import workbench.gui.dbobjects.objecttree.TreePosition;
import workbench.gui.fontzoom.DecreaseFontSize;
import workbench.gui.fontzoom.FontZoomer;
import workbench.gui.fontzoom.IncreaseFontSize;
import workbench.gui.fontzoom.ResetFontSize;
import workbench.gui.macros.MacroMenuBuilder;
import workbench.gui.menu.RecentFileManager;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.PanelType;
import workbench.gui.sql.RenameableTab;
import workbench.gui.sql.SqlPanel;

import workbench.sql.macros.MacroManager;

import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.FileUtil;
import workbench.util.FileVersioner;
import workbench.util.HtmlUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;
import workbench.util.WbFile;
import workbench.util.WbProperties;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * The main window for SQL Workbench.
 *
 * It will display several {@link workbench.gui.sql.SqlPanel}s in
 * a tabbed pane. Additionally one or more {@link workbench.gui.dbobjects.DbExplorerPanel}
 * might also be displayed inside the JTabbedPane
 *
 * @author  Thomas Kellerer
 */
public class MainWindow
	extends JFrame
	implements MouseListener, WindowListener, ChangeListener,
						MacroChangeListener, DbExecutionListener, Connectable, PropertyChangeListener,
						Moveable, RenameableTab, TabCloser, FilenameChangeListener
{
	private static final String DEFAULT_WORKSPACE = "Default.wksp";
	private static final String RECENTMACROS_NAME = "recent-macros";
	private static final String DB_TREE_PROPS = "dbtree";

	private static int instanceCount;
	private final int windowId;

	private boolean exitOnCancel = false;

	private WbConnection currentConnection;
	private ConnectionProfile currentProfile;
	protected ConnectionSelector connectionSelector;

  private HelpConnectionInfoAction connectionInfoAction;
	private ShowDbmsManualAction showDbmsManual;
	private FileDisconnectAction disconnectAction;
	private FileReconnectAction reconnectAction;
	private CreateNewConnection createNewConnection;
	private DisconnectTabAction disconnectTab;
	private ShowDbExplorerAction dbExplorerAction;
	private NewDbExplorerPanelAction newDbExplorerPanel;
	private NewDbExplorerWindowAction newDbExplorerWindow;
  private ShowDbTreeAction showDbTree;
	private final WbTabbedPane sqlTab;
	private final TabbedPaneHistory tabHistory;
	private WbToolbar currentToolbar;
	private final List<JMenuBar> panelMenus = Collections.synchronizedList(new ArrayList<JMenuBar>(15));

	private String currentWorkspaceFile;

	private CloseWorkspaceAction closeWorkspaceAction;
	private SaveWorkspaceAction saveWorkspaceAction;
	private SaveAsNewWorkspaceAction saveAsWorkspaceAction;
	private LoadWorkspaceAction loadWorkspaceAction;
	private AssignWorkspaceAction assignWorkspaceAction;
	private ReloadProfileWkspAction reloadWorkspace;
	private final NextTabAction nextTab;
	private final PrevTabAction prevTab;

	private boolean resultForWorkspaceClose;

	private boolean ignoreTabChange;

	// will indicate a connect or disconnect in progress
	// connecting and disconnecting is done in a separate thread
	// so that slow connections do not block the GUI
	private boolean connectInProgress;

	private AddMacroAction createMacro;
	private ManageMacroAction manageMacros;
	private LoadMacrosAction loadMacros;
	private SaveMacrosAction saveMacros;
	private ShowMacroPopupAction showMacroPopup;

	private final List<ToolWindow> explorerWindows = new ArrayList<>();

	private RunningJobIndicator jobIndicator;
	protected WbThread connectThread;
	private DropHandler dropHandler;
  private DbTreePanel treePanel;
  private boolean shouldShowTree;

	/**
	 * Stores additional properties that should be saved into the Worskpace from objects that are not constantly visible.
	 * e.g. the Macro Popup window
	 */
	private final Map<String, WbProperties> toolProperties = new HashMap<>();

	public MainWindow()
	{
		super(ResourceMgr.TXT_PRODUCT_NAME);

		// Control the brushed metal look for MacOS, this must be set as soon as possible on the
		// root pane in order to have an effect
		getRootPane().putClientProperty("apple.awt.brushMetalLook", GuiSettings.getUseBrushedMetal());

		this.windowId = ++instanceCount;

		sqlTab = new WbTabbedPane();
		tabHistory = new TabbedPaneHistory(sqlTab);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// There is no need to register the actions with the ActionMap
		// as they will be handed over to the FocusManager in windowActivated()
		nextTab = new NextTabAction(sqlTab);
		prevTab = new PrevTabAction(sqlTab);

		initMenu();

		ResourceMgr.setWindowIcons(this, "workbench");

		getContentPane().add(this.sqlTab, BorderLayout.CENTER);

		restoreSettings();

		updateTabPolicy();

		sqlTab.addChangeListener(this);
		sqlTab.addMouseListener(this);
		sqlTab.hideDisabledButtons(false);
		if (GuiSettings.getShowSqlTabCloseButton())
		{
			sqlTab.showCloseButton(this);
		}

		addWindowListener(this);

		MacroManager.getInstance().getMacros(getMacroClientId()).addChangeListener(this);

		dropHandler = new DropHandler(this, sqlTab);
		sqlTab.enableDragDropReordering(this);

		Settings.getInstance().addPropertyChangeListener(this,
			Settings.PROPERTY_SHOW_TOOLBAR,
			Settings.PROPERTY_SHOW_TAB_INDEX,
			GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON,
			Settings.PROPERTY_TAB_POLICY,
			GuiSettings.PROP_TITLE_APP_AT_END,
			GuiSettings.PROP_TITLE_SHOW_WKSP,
			GuiSettings.PROP_TITLE_SHOW_URL,
			GuiSettings.PROP_TITLE_SHOW_PROF_GROUP,
			GuiSettings.PROP_TITLE_SHOW_EDITOR_FILE,
			GuiSettings.PROP_TITLE_GROUP_SEP,
			GuiSettings.PROP_TITLE_GROUP_BRACKET,
			GuiSettings.PROP_TITLE_SHOW_URL_USER
		);
		ShortcutManager.getInstance().addChangeListener(this);
	}

	protected final void updateTabPolicy()
	{
		final JComponent content = (JComponent)this.getContentPane();
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				int tabPolicy = Settings.getInstance().getIntProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.WRAP_TAB_LAYOUT);
				sqlTab.setTabLayoutPolicy(tabPolicy);
				sqlTab.invalidate();
				content.revalidate();
			}
		});
		WbSwingUtilities.repaintLater(this);
	}

	public void display()
	{
		this.restoreState();
		this.setVisible(true);
		this.addTab();
		this.updateWindowTitle();

		boolean macroVisible = Settings.getInstance().getBoolProperty(this.getClass().getName() + ".macropopup.visible", false);
		if (macroVisible)
		{
			EventQueue.invokeLater(showMacroPopup::showPopup);
		}
	}

  public boolean isDbTreeVisible()
  {
    return (treePanel != null && treePanel.isVisible());
  }

  public void showDbTree()
  {
    showDbTree(true);
  }

  public void showDbTree(boolean requestFocus)
  {
    if (treePanel == null)
    {
      treePanel = new DbTreePanel();

      getContentPane().remove(sqlTab);

      WbSplitPane split = new WbSplitPane();
      split.setOneTouchExpandable(true);

      TreePosition position = DbTreeSettings.getDbTreePosition();
      if (position == TreePosition.left)
      {
        split.setLeftComponent(treePanel);
        split.setRightComponent(sqlTab);
      }
      else
      {
        split.setLeftComponent(sqlTab);
        split.setRightComponent(treePanel);
      }

      treePanel.restoreSettings(getToolProperties(DB_TREE_PROPS));

      getContentPane().add(split, BorderLayout.CENTER);

      invalidate();
      sqlTab.invalidate();

      EventQueue.invokeLater(this::validate);
    }

    if (treePanel.getConnection() == null)
    {
      treePanel.connect(currentProfile);
    }

    if (requestFocus)
    {
      treePanel.requestFocusInWindow();
    }

    int count = getTabCount();
    for (int i=0; i < count - 1; i++)
    {
      MainPanel sqlPanel = getSqlPanel(i);
      sqlPanel.registerObjectFinder(treePanel);
    }
  }

  public void hideDbTree()
  {
    if (treePanel == null) return;
    if (!treePanel.isVisible()) return;

    treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));
    treePanel.setVisible(false);
  }

  public void restoreDbTree()
  {
    if (treePanel == null) return;
    if (treePanel.isVisible()) return;
    treePanel.restoreSettings(getToolProperties(DB_TREE_PROPS));
    treePanel.setVisible(true);
  }

  public DbTreePanel getDbTree()
  {
    return treePanel;
  }

  public void closeDbTree()
  {
    if (treePanel != null)
    {
      treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));

      JSplitPane split = (JSplitPane)treePanel.getParent();
      split.remove(sqlTab);
      getContentPane().remove(split);
      getContentPane().add(sqlTab, BorderLayout.CENTER);
      sqlTab.invalidate();
      invalidate();

      treePanel.disconnectInBackground();
      treePanel = null;

      int count = getTabCount();
      for (int i=0; i < count - 1; i++)
      {
        MainPanel sqlPanel = getSqlPanel(i);
        sqlPanel.registerObjectFinder(null);
      }
      EventQueue.invokeLater(this::validate);
    }
  }

	@Override
	public void fileNameChanged(Object sender, String newFilename)
	{
		if (ignoreTabChange) return;

		updateWindowTitle();
		if (sender instanceof SqlPanel)
		{
			SqlPanel panel = (SqlPanel)sender;
			BookmarkManager.getInstance().updateInBackground(this, panel, true);
		}
	}

	/**
	 * The listener will be notified when the name of a tab changes.
	 * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
	 * to display the available panels in the context menu
	 * @see workbench.gui.dbobjects.EditorTabSelectMenu#fileNameChanged(Object, String)
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

	/**
	 * Remove the file name change listener.
	 * @see #addFilenameChangeListener(FilenameChangeListener )
	 */
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
	@Override
	public void addTabChangeListener(ChangeListener aListener)
	{
		this.sqlTab.addChangeListener(aListener);
	}

	public void removeIndexChangeListener(ChangeListener aListener)
	{
		this.sqlTab.removeChangeListener(aListener);
	}

	public void addExecutionListener(DbExecutionListener l)
	{
		int count = this.sqlTab.getTabCount();
		for (int i = 0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				((SqlPanel)p).addDbExecutionListener(l);
			}
		}
	}

	public void removeExecutionListener(DbExecutionListener l)
	{
		int count = this.sqlTab.getTabCount();
		for (int i = 0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				((SqlPanel)p).removeDbExecutionListener(l);
			}
		}
	}

  public boolean hasProfileWorkspace()
  {
    return currentProfile != null && StringUtil.isNonEmpty(currentProfile.getWorkspaceFile());
  }

	protected void checkWorkspaceActions()
	{
		this.saveWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
		this.assignWorkspaceAction.setEnabled(this.currentWorkspaceFile != null && this.currentProfile != null);
		this.closeWorkspaceAction.setEnabled(this.currentWorkspaceFile != null);
		checkReloadWkspAction();
	}

	private void initMenu()
	{
		this.disconnectAction = new FileDisconnectAction(this);
		this.reconnectAction = new FileReconnectAction(this);
		this.assignWorkspaceAction = new AssignWorkspaceAction(this);
		this.reloadWorkspace = new ReloadProfileWkspAction(this);
		this.closeWorkspaceAction = new CloseWorkspaceAction(this);
		this.saveAsWorkspaceAction = new SaveAsNewWorkspaceAction(this);

		this.createNewConnection = new CreateNewConnection(this);
		this.disconnectTab = new DisconnectTabAction(this);

		this.loadWorkspaceAction = new LoadWorkspaceAction(this);
		this.saveWorkspaceAction = new SaveWorkspaceAction(this);

		this.createMacro = new AddMacroAction(getMacroClientId());
		this.manageMacros = new ManageMacroAction(this);
		this.loadMacros = new LoadMacrosAction(getMacroClientId());
		this.saveMacros = new SaveMacrosAction(getMacroClientId());
		showMacroPopup = new ShowMacroPopupAction(this);

		this.dbExplorerAction = new ShowDbExplorerAction(this);
		this.newDbExplorerPanel = new NewDbExplorerPanelAction(this);
		this.newDbExplorerWindow = new NewDbExplorerWindowAction(this);
    this.showDbTree = new ShowDbTreeAction(this);
		int tabCount = this.sqlTab.getTabCount();
		for (int tab=0; tab < tabCount; tab ++)
		{
			MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(tab);
			JMenuBar menuBar = this.createMenuForPanel(sql);
			this.panelMenus.add(menuBar);
		}
	}

	private void adjustMenuHeight(JMenuBar bar)
	{
		if (!GuiSettings.limitMenuLength()) return;
		int maxItems = Math.min(WbSwingUtilities.calculateMaxMenuItems(this) - 4, GuiSettings.maxMenuItems());
		int count = bar.getMenuCount();
		for (int i=0; i < count; i++)
		{
			JMenu menu = bar.getMenu(i);
      if (menu == null) continue;
			int items = menu.getItemCount();
			if (items > maxItems)
			{
				MenuScroller.setScrollerFor(menu, maxItems - 4);
			}
		}
	}

	private JMenuBar createMenuForPanel(MainPanel panel)
	{
		HashMap<String, JMenu> menus = new HashMap<>(10);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorderPainted(false);
		menuBar.putClientProperty("jgoodies.headerStyle", "Single");

		// Create the file menu for all tabs
		JMenu menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_FILE));
		menu.setName(ResourceMgr.MNU_TXT_FILE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_FILE, menu);

		WbAction action;

		action = new FileConnectAction(this);
		action.addToMenu(menu);
		this.disconnectAction.addToMenu(menu);
		this.reconnectAction.addToMenu(menu);
		FileCloseAction close = new FileCloseAction(this);
		close.addToMenu(menu);
		menu.addSeparator();
		this.createNewConnection.addToMenu(menu);
		this.disconnectTab.addToMenu(menu);
		menu.addSeparator();

		action = new FileSaveProfiles();
		action.addToMenu(menu);

		action = new FileNewWindowAction();
		action.addToMenu(menu);

		OpenFileAction open = new OpenFileAction(this);
		menu.addSeparator();
		open.addToMenu(menu);

		// now create the menus for the current tab
		List menuItems = panel.getMenuItems();

		// Create the menus in the correct order
		if (panel instanceof SqlPanel)
		{
			menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_EDIT));
			menu.setName(ResourceMgr.MNU_TXT_EDIT);
			menu.setVisible(false);
			menuBar.add(menu);
			menus.put(ResourceMgr.MNU_TXT_EDIT, menu);
		}

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_VIEW));
		menu.setName(ResourceMgr.MNU_TXT_VIEW);
		menu.setVisible(true);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_VIEW, menu);

		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i ++)
		{
			action = new SelectTabAction(this.sqlTab, i);
			menu.add(action);
		}
		menu.addSeparator();
		menu.add(nextTab.getMenuItem());
		menu.add(prevTab.getMenuItem());

		if (panel instanceof SqlPanel)
		{
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

			final WbMenu macroMenu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_MACRO));
			macroMenu.setName(ResourceMgr.MNU_TXT_MACRO);
			macroMenu.setVisible(true);
			menuBar.add(macroMenu);
			menus.put(ResourceMgr.MNU_TXT_MACRO, macroMenu);
			buildMacroMenu(macroMenu);
		}

		menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_WORKSPACE));
		menu.setName(ResourceMgr.MNU_TXT_WORKSPACE);
		menuBar.add(menu);
		menus.put(ResourceMgr.MNU_TXT_WORKSPACE, menu);
		menu.add(this.saveWorkspaceAction);
		menu.add(this.saveAsWorkspaceAction);
		menu.add(this.loadWorkspaceAction);
		menu.add(this.reloadWorkspace);
		menu.addSeparator();
		menu.add(this.closeWorkspaceAction);
		menu.add(this.assignWorkspaceAction);
		menu.addSeparator();
		JMenu recentWorkspace = new JMenu(ResourceMgr.getString("MnuTxtRecentWorkspace"));
		recentWorkspace.setName("recent-workspace");
		RecentFileManager.getInstance().populateRecentWorkspaceMenu(recentWorkspace, this);
		menu.add(recentWorkspace);


		for (Object entry : menuItems)
		{
			WbMenu subMenu = null;
			String menuName = null;
			WbAction menuAction = null;

			boolean menuSep = false;
			if (entry instanceof WbAction)
			{
				menuAction = (WbAction)entry;
				menuName = menuAction.getMenuItemName();
				menuSep = menuAction.getCreateMenuSeparator();
			}
			else if (entry instanceof WbMenu)
			{
				subMenu = (WbMenu)entry;
				menuName = subMenu.getParentMenuId();
				menuSep = subMenu.getCreateMenuSeparator();
			}

			if (menuName == null)	continue;

			menu = menus.get(menuName);

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

			if (menuAction != null)
			{
				menuAction.addToMenu(menu);
			}
			else if (subMenu != null)
			{
				menu.add(subMenu);
				subMenu.setVisible(true);
			}
			menu.setVisible(true);
		}

		final JMenu filemenu = menus.get(ResourceMgr.MNU_TXT_FILE);
		filemenu.addSeparator();
		filemenu.add(new ManageDriversAction());
		filemenu.addSeparator();

		action = new FileExitAction();
		filemenu.add(action);

		final JMenu viewMenu = menus.get(workbench.resource.ResourceMgr.MNU_TXT_VIEW);
		AddTabAction add = new AddTabAction(this);
		viewMenu.addSeparator();
		viewMenu.add(add);
		InsertTabAction insert = new InsertTabAction(this);
		viewMenu.add(insert);

		RemoveTabAction rem = new RemoveTabAction(this);
		viewMenu.add(rem);
		viewMenu.add(new RenameTabAction(this));
		viewMenu.addSeparator();
		ViewLineNumbers v = new ViewLineNumbers();
		v.addToMenu(viewMenu);

		WbAction vTb = new ViewToolbarAction();
		vTb.addToMenu(viewMenu);

		if (panel instanceof SqlPanel)
		{
			JMenu zoom = new JMenu(ResourceMgr.getString("TxtZoom"));
			SqlPanel sqlpanel = (SqlPanel)panel;
			EditorPanel editor = sqlpanel.getEditor();
			FontZoomer zoomer = editor.getFontZoomer();

			IncreaseFontSize inc = new IncreaseFontSize(zoomer);
			DecreaseFontSize dec = new DecreaseFontSize(zoomer);
			ResetFontSize reset = new ResetFontSize(zoomer);

			zoom.add(new JMenuItem(inc));
			zoom.add(new JMenuItem(dec));
			zoom.addSeparator();
			zoom.add(new JMenuItem(reset));
			viewMenu.add(zoom);
		}

		menuBar.add(this.buildToolsMenu());
		menuBar.add(this.buildHelpMenu());

		panel.addToToolbar(dbExplorerAction, false);
    panel.addToToolbar(showDbTree, true);
		adjustMenuHeight(menuBar);
		return menuBar;
	}

	/**
	 * Removes or makes the toolbar visible depending on
	 * {@link GuiSettings#getShowToolbar}.
	 *
	 * This method will <i>validate</i> this' {@link #getContentPane content pane}
	 * in case a change on the toolbar's visibility is performed.
	 *
	 * This method should be called on the EDT.
	 */
	private void updateToolbarVisibility()
	{
		final JComponent content = (JComponent)this.getContentPane();

		if (this.currentToolbar != null)
		{
			content.remove(this.currentToolbar);
			this.currentToolbar = null;
		}

		if (GuiSettings.getShowToolbar())
		{
			final MainPanel curPanel = this.getCurrentPanel();
			if (curPanel != null)
			{
				this.currentToolbar = curPanel.getToolbar();
				content.add(currentToolbar, BorderLayout.NORTH);
			}
		}
		content.revalidate();
	}

	public void forceRedraw()
	{
		WbSwingUtilities.invoke(() ->
    {
      JComponent content = (JComponent)getContentPane();
      sqlTab.validate();
      content.validate();
    });
		WbSwingUtilities.repaintLater(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_SHOW_TOOLBAR.equals(evt.getPropertyName()))
		{
			updateToolbarVisibility();
		}
		else if (Settings.PROPERTY_SHOW_TAB_INDEX.equals(evt.getPropertyName()))
		{
			this.renumberTabs();
		}
		else if (GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON.equals(evt.getPropertyName()))
		{
			if (GuiSettings.getShowSqlTabCloseButton())
			{
				sqlTab.showCloseButton(this);
			}
			else
			{
				sqlTab.showCloseButton(null);
			}
		}
		else if (Settings.PROPERTY_TAB_POLICY.equals(evt.getPropertyName()))
		{
			updateTabPolicy();
		}
		else if (GuiSettings.WINDOW_TITLE_PROPS.contains(evt.getPropertyName()))
		{
			updateWindowTitle();
		}
	}

	private void checkMacroMenuForPanel(int index)
	{
		MainPanel p = this.getSqlPanel(index);
		try
		{
			JMenu macro = this.getMacroMenu(index);
			setMacroMenuItemStates(macro, p.isConnected());
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkMacroMenuForPanel()", "Error during macro update", e);
		}
	}

	private void setMacroMenuEnabled(boolean enabled)
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macro = this.getMacroMenu(i);
			setMacroMenuItemStates(macro, enabled);
		}
	}

	private void setMacroMenuItemStates(JMenu menu, boolean enabled)
	{
		if (menu != null)
		{
			int itemCount = menu.getItemCount();

			int startIndex = -1;
			for (int i=0; i < itemCount; i++)
			{
				JMenuItem item = menu.getItem(i);
				if (item == null) continue;
				if (item.getName() == null) continue;
				if (item.getName().equals(RECENTMACROS_NAME))
				{
					startIndex = i + 1;
				}
			}

			if (startIndex == -1)
			{
				LogMgr.logWarning("MainWindow.setMacroMenuItemStates()", "Start of macro menu items not found!");
				return;
			}

			for (int in=startIndex; in < itemCount; in++)
			{
				JMenuItem item = menu.getItem(in);
				if (item != null) item.setEnabled(enabled);
			}
		}
	}

	@Override
	public void macroListChanged()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			JMenu macros = this.getMacroMenu(i);
			if (macros != null)
			{
				this.buildMacroMenu(macros);
				MainPanel p = this.getSqlPanel(i);
				this.setMacroMenuItemStates(macros, p.isConnected());
			}
		}
	}

	private void buildMacroMenu(JMenu macroMenu)
	{
		macroMenu.removeAll();
		createMacro.addToMenu(macroMenu);
		manageMacros.addToMenu(macroMenu);
		showMacroPopup.addToMenu(macroMenu);

		macroMenu.addSeparator();
		loadMacros.addToMenu(macroMenu);
		saveMacros.addToMenu(macroMenu);

		JMenu recentWorkspace = new JMenu(ResourceMgr.getString("MnuTxtRecentMacros"));
		recentWorkspace.setName(RECENTMACROS_NAME);
		RecentFileManager.getInstance().populateRecentMacrosMenu(getMacroClientId(), recentWorkspace);
		macroMenu.add(recentWorkspace);

		MacroMenuBuilder builder = new MacroMenuBuilder();
		builder.buildMacroMenu(this, macroMenu);
	}

	public int getCurrentPanelIndex()
	{
		return this.sqlTab.getSelectedIndex();
	}

	public int getIndexForPanel(MainPanel panel)
	{
		if (panel == null) return -1;
		return getIndexForPanel(panel.getId());
	}

	public int getIndexForPanel(String tabId)
	{
		if (tabId == null) return -1;
		int tabCount = this.sqlTab.getTabCount();
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.getId().equals(tabId)) return i;
		}
		return -1;
	}

	/**
	 * Return properties for a specific tool.
	 *
	 * @param toolKey a (unique) key for the tool. It must not contain spaces or special characters
	 * @return the properties, never null
	 */
	public WbProperties getToolProperties(String toolKey)
	{
		synchronized (toolProperties)
		{
			WbProperties props = toolProperties.get(toolKey);
			if (props == null)
			{
				props = new WbProperties(1);
				toolProperties.put(toolKey, props);
			}
			return props;
		}
	}

	/**
	 * Return a list of titles for all sql panels.
	 * For indexes where a DbExplorer is open a NULL string will be returned
	 * at that index position in the list.
	 */
	public List<String> getPanelLabels()
	{
		int tabCount = this.sqlTab.getTabCount();

		List<String> result = new ArrayList<>(tabCount);
		for (int i=0; i < tabCount; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p instanceof SqlPanel)
			{
				result.add(p.getTabTitle());
			}
			else
			{
				result.add(null);
			}
		}
		return result;
	}

	public MainPanel getCurrentPanel()
	{
		int index = this.sqlTab.getSelectedIndex();
		if (index >-1) return this.getSqlPanel(index);
		else return null;
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

	public int getTabCount()
	{
		return this.sqlTab.getTabCount();
	}

	public MainPanel getSqlPanel(int index)
	{
		if (index < 0 || index >= sqlTab.getTabCount()) return null;
		try
		{
			return (MainPanel)this.sqlTab.getComponentAt(index);
		}
		catch (Exception e)
		{
			LogMgr.logDebug("MainWindow.getSqlPanel()", "Invalid index [" + index + "] specified!", e);
			return null;
		}
	}

	public void selectTab(int anIndex)
	{
		this.sqlTab.setSelectedIndex(anIndex);
	}

	private boolean isConnectInProgress()
	{
		return this.connectInProgress;
	}

	private void clearConnectIsInProgress()
	{
		this.connectInProgress = false;
	}

	private void setConnectIsInProgress()
	{
		this.connectInProgress = true;
	}

	public void checkConnectionForPanel(final MainPanel panel)
	{
		if (this.isConnectInProgress()) return;
		if (panel == null) return;
		if (panel.isConnected()) return;

		try
		{
			if (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab())
			{
				createNewConnectionForPanel(panel);
			}
			else if (this.currentConnection != null)
			{
				panel.setConnection(this.currentConnection);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.checkConnectionForPanel()", "Error when checking connection", e);
		}
	}

	public void disconnectCurrentPanel()
	{
		if (this.currentProfile == null) return;
		if (this.currentProfile.getUseSeparateConnectionPerTab()) return;

		final MainPanel p = this.getCurrentPanel();
		WbConnection con = p.getConnection();
		if (con == this.currentConnection) return;

		Thread t = new WbThread("Disconnect panel " + p.getId())
		{
			@Override
			public void run()
			{
				disconnectPanel(p);
			}
		};
		t.start();
	}

	protected void disconnectPanel(final MainPanel panel)
	{
		if (this.isConnectInProgress()) return;
		boolean inProgress = isConnectInProgress();
		if (!inProgress) setConnectIsInProgress();

		showDisconnectInfo();
		showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
		try
		{
			WbConnection old = panel.getConnection();
			panel.disconnect();

			// use WbConnection.disconnect() rather than ConnectionMgr.getInstance().disconnect()
			// to make sure the connection state listeners are notified
			old.disconnect();

			panel.setConnection(currentConnection);
			int index = this.getIndexForPanel(panel);
			sqlTab.setForegroundAt(index, null);
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.connectPanel()", "Error when disconnecting panel " + panel.getId(), e);
			String error = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.showErrorMessage(this, error);
		}
		finally
		{
			showStatusMessage("");
			closeConnectingInfo();
			if (!inProgress) clearConnectIsInProgress();
		}

		EventQueue.invokeLater(() ->
    {
      createNewConnection.checkState();
      disconnectTab.checkState();
    });
	}

	public boolean canUseSeparateConnection()
	{
		if (this.currentProfile == null) return false;
		return !this.currentProfile.getUseSeparateConnectionPerTab();
	}

	public boolean usesSeparateConnection()
	{
		if (!canUseSeparateConnection()) return false;
		final MainPanel current = this.getCurrentPanel();
		WbConnection conn = current.getConnection() ;

		return (currentConnection != null && conn != this.currentConnection);
	}

	public void createNewConnectionForCurrentPanel()
	{
		final MainPanel panel = getCurrentPanel();
		createNewConnectionForPanel(panel);
		EventQueue.invokeLater(() ->
    {
      int index = getIndexForPanel(panel);
      sqlTab.setForegroundAt(index, Color.BLUE);
    });
	}

	protected void createNewConnectionForPanel(final MainPanel aPanel)
	{
		if (this.isConnectInProgress()) return;
		if (this.connectThread != null) return;

		this.showConnectingInfo();

		this.connectThread = new WbThread("Panel Connect " + aPanel.getId())
		{
			@Override
			public void run()
			{
				connectPanel(aPanel);
			}
		};
		this.connectThread.start();
	}

	/**
	 * Connect the given panel to the database. This will always
	 * create a new physical connection to the database.
	 */
	protected void connectPanel(final MainPanel aPanel)
	{
		if (this.isConnectInProgress()) return;
		this.setConnectIsInProgress();

		try
		{
			// prevent a manual tab change while connecting
			sqlTab.setEnabled(false);

			WbConnection conn = this.getConnectionForTab(aPanel, true);
			int index = this.getIndexForPanel(aPanel);
			this.tabConnected(aPanel, conn, index);
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.connectPanel()", "Error when connecting panel " + aPanel.getId(), e);
			showStatusMessage("");
			String error = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), error);
		}
		finally
		{
			sqlTab.setEnabled(true);
			closeConnectingInfo();
			clearConnectIsInProgress();
			this.connectThread = null;
		}
	}

	public void waitForConnection()
	{
		if (this.connectThread != null)
		{
			try
			{
				this.connectThread.join();
			}
			catch (Exception e)
			{
				LogMgr.logError("MainWindow.waitForConnection()", "Error joining connection thread", e);
			}
		}
	}

	private void tabConnected(final MainPanel panel, WbConnection conn, final int anIndex)
	{
		this.closeConnectingInfo();
		panel.setConnection(conn);

		WbSwingUtilities.invoke(() ->
    {
      updateGuiForTab(anIndex);
    });
	}

	private void updateGuiForTab(final int index)
	{
		if (index < 0) return;
		if (index > this.sqlTab.getTabCount() - 1) return;

		final MainPanel current = this.getSqlPanel(index);
		if (current == null) return;

		JMenuBar menu = null;
		if (index > -1 && index < panelMenus.size())
		{
			menu = this.panelMenus.get(index);
		}

		// this can happen if a tab selected event occurs during initialization of a new tab
		if (menu == null)
		{
			return;
		}

		setJMenuBar(menu);
		updateToolbarVisibility();
		createNewConnection.checkState();
		disconnectTab.checkState();
		checkMacroMenuForPanel(index);
		forceRedraw();

		SwingUtilities.invokeLater(current::panelSelected);
	}

	public void currentTabChanged()
	{
		int index = getCurrentPanelIndex();
		tabSelected(index);
	}

	protected void tabSelected(final int index)
	{
		if (index < 0) return;
		if (index >= sqlTab.getTabCount()) return;

		// Make sure this is executed on the EDT
		WbSwingUtilities.invoke(() ->
    {
      updateCurrentTab(index);
    });

		int lastIndex = sqlTab.getPreviousTabIndex();
		if (lastIndex > -1 && lastIndex < sqlTab.getTabCount())
		{
			BookmarkManager.getInstance().updateInBackground(MainWindow.this, getSqlPanel(lastIndex), false);
		}

    if (getCurrentPanel() instanceof DbExplorerPanel)
    {
      hideDbTree();
    }
    else
    {
      MainPanel lastPanel = getSqlPanel(lastIndex);
      if (lastPanel instanceof DbExplorerPanel)
      {
        if (shouldShowTree)
        {
          showDbTree(false);
          shouldShowTree = false;
        }
        else
        {
          restoreDbTree();
        }
      }
    }

	}

	private void updateCurrentTab(int index)
	{
		MainPanel current = getSqlPanel(index);
		if (current == null) return;
		checkConnectionForPanel(current);
		updateAddMacroAction();
		updateGuiForTab(index);
		updateWindowTitle();
	}

	protected void updateAddMacroAction()
	{
		SqlPanel sql = this.getCurrentSqlPanel();
		if (sql != null)
		{
			createMacro.setClient(sql.getEditor());
		}
	}

	public void restoreState()
	{
		String state = Settings.getInstance().getProperty(this.getClass().getName() + ".state", "0");
		int i = StringUtil.getIntValue(state, NORMAL);
		if (i == MAXIMIZED_BOTH)
		{
			setExtendedState(i);
		}
	}

	public final void restoreSettings()
	{
		Settings s = Settings.getInstance();

		if (!s.restoreWindowSize(this))
		{
      Dimension screenSize = WbSwingUtilities.getScreenSize();
      int w = (int)(screenSize.width * 0.75);
      int h = (int)(w * 0.75);
			this.setSize(w, h);
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
		boolean macroVisible = (showMacroPopup != null && showMacroPopup.isPopupVisible());
		sett.setProperty(this.getClass().getName() + ".macropopup.visible", macroVisible);
	}

	@Override
	public void windowOpened(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowClosing(WindowEvent windowEvent)
	{
		WbManager.getInstance().closeMainWindow(this);
	}

	@Override
	public void windowDeactivated(WindowEvent windowEvent)
	{
    if (GuiSettings.installFocusManager())
    {
      WbKeyDispatcher.getInstance().grabActions(null, null);
    }
	}

	@Override
	public void windowActivated(WindowEvent windowEvent)
	{
    if (GuiSettings.installFocusManager())
    {
      WbKeyDispatcher.getInstance().grabActions(nextTab, prevTab);
    }
  }

	@Override
	public void windowIconified(WindowEvent windowEvent)
	{
	}

	/**
	 *	Display a message in the status bar
	 */
	public void showStatusMessage(final String aMsg)
	{
		MainPanel current = this.getCurrentPanel();
		if (!(current instanceof StatusBar)) return;

		final StatusBar status = (StatusBar)current;

		WbSwingUtilities.invoke(() ->
    {
      if (StringUtil.isEmptyString(aMsg))
      {
        status.clearStatusMessage();
      }
      else
      {
        status.setStatusMessage(aMsg);
      }
    });
	}

	public void showLogMessage(String aMsg)
	{
		MainPanel current = this.getCurrentPanel();
		if (current != null) current.showLogMessage(aMsg);
	}

	@Override
	public boolean connectBegin(final ConnectionProfile aProfile, final StatusBar info, final boolean loadWorkspace)
	{
		if (this.isBusy() || this.isCancelling())
		{
			WbSwingUtilities.showErrorMessageKey(this, "MsgDisconnectBusy");
			return false;
		}

		if (this.currentWorkspaceFile != null && WbManager.getInstance().getSettingsShouldBeSaved())
		{
			if (!this.saveWorkspace(this.currentWorkspaceFile, true))
			{
				return false;
			}
		}

		if (this.isConnected())
		{
			showDisconnectInfo();
		}
		disconnect(false, false, false);

		// it is important to set the connectInProgress flag,
		// otherwise loading the workspace will already trigger a
		// panel switch which might cause a connect
		// to the current profile before the ConnectionSelector
		// has actually finished.
		// this has to be set AFTER calling disconnect(), because
		// disconnect respects this flag and does nothing...
		this.setConnectIsInProgress();

		this.currentProfile = aProfile;

		showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
		if (info != null)
		{
			info.setStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
		}

		if (loadWorkspace)
		{
			loadCurrentProfileWorkspace();
		}
		Settings.getInstance().setLastConnection(currentProfile);
		showStatusMessage(ResourceMgr.getString("MsgConnecting"));
		return true;
	}

	public int getMacroClientId()
	{
		return windowId;
	}

	public String getWindowId()
	{
		return NumberStringCache.getNumberString(windowId);
	}

	private String getConnectionIdForPanel(MainPanel p)
	{
		if (p == null)
		{
			LogMgr.logError("MainWindow.getConnectionIdForPanel()", "Requested connection ID for NULL panel!", new Exception());
			return "Wb" + getWindowId();
		}
		return "Wb" + getWindowId() + "-" + p.getId();
	}

	@Override
	public String getDefaultIconName()
	{
		return "workbench";
	}

	/**
	 * Return the internal ID that should be used when connecting
	 * to the given connection profile
	 * @return an id specific for the current tab or a "global" id the connection
	 *         is shared between all tabs of this window
	 */
	@Override
	public String getConnectionId(ConnectionProfile aProfile)
	{
		if (aProfile != null && aProfile.getUseSeparateConnectionPerTab())
		{
			return getConnectionIdForPanel(this.getCurrentPanel());
		}
		else
		{
			return "WbWin-" + getWindowId();
		}
	}

	private ConnectionSelector getSelector()
	{
		if (connectionSelector == null)
		{
			connectionSelector = new ConnectionSelector(this, this);
		}
		return connectionSelector;
	}

	public void connectTo(ConnectionProfile profile, boolean showDialog, boolean loadWorkspace)
	{
		if (!ProfileSelectionDialog.doPrompt(this, profile))
		{
      LogMgr.logWarning("MainWindow.connectTo()", "Can't directly connect to a profile that requires prompting for a password or username");
			return;
		}
		getSelector().connectTo(profile, showDialog, loadWorkspace);
	}


	private void loadMacrosForProfile()
	{
		if (currentProfile == null) return;
		WbFile macroFile = currentProfile.getMacroFile();

		MacroManager.getInstance().getMacros(getMacroClientId()).removeChangeListener(this);
		if (macroFile != null && macroFile.exists())
		{
			MacroManager.getInstance().loadMacros(getMacroClientId(), macroFile);
		}
		else
		{
			MacroManager.getInstance().loadDefaultMacros(getMacroClientId());
		}
		macroListChanged();
		MacroManager.getInstance().getMacros(getMacroClientId()).addChangeListener(this);
	}

	/**
	 *	Call-back function which gets executed on the AWT thread after
	 *  the initial connection has been completed
	 */
	@Override
	public void connected(WbConnection conn)
	{
		MainPanel panel = this.getCurrentPanel();
		if (panel == null)
		{
			Thread.yield();
			panel = this.getCurrentPanel();
			LogMgr.logError("MainWindow.connected()", "Connection established but no current panel!", new NullPointerException("Backtrace"));
		}

		if (this.currentProfile.getUseSeparateConnectionPerTab())
		{
			if (panel != null) panel.setConnection(conn);
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
		if (showDbTree != null) showDbTree.setEnabled(true);

		this.disconnectAction.setEnabled(true);
		this.reconnectAction.setEnabled(true);
		this.createNewConnection.checkState();
		this.disconnectTab.checkState();
		this.showMacroPopup.workspaceChanged();

		if (panel != null)
		{
			panel.clearLog();
			panel.showResultPanel();
		}
		VersionNumber version = conn.getDatabaseVersion();
		showDbmsManual.setDbms(conn.getDbId(), version);
    connectionInfoAction.setEnabled(true);
		showConnectionWarnings(conn, panel);

    if (isDbTreeVisible())
    {
      treePanel.connect(currentProfile);
    }

		selectCurrentEditor();
	}

	@Override
	public void connectFailed(String error)
	{
		disconnected(true);
		tabSelected(0);

		if (error == null) return;
		WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), error.trim());
	}

	@Override
	public void connectCancelled()
	{
		if (this.exitOnCancel)
		{
			WbManager.getInstance().closeMainWindow(this);
		}
	}

	@Override
	public void connectEnded()
	{
		for (int i=0; i < sqlTab.getTabCount(); i++)
		{
			MainPanel sql = getSqlPanel(i);
			if (sql instanceof StatusBar)
			{
				((StatusBar)sql).clearStatusMessage();
			}
		}
		this.clearConnectIsInProgress();
	}

	private static final int CREATE_WORKSPACE = 0;
	private static final int LOAD_OTHER_WORKSPACE = 1;
	private static final int IGNORE_MISSING_WORKSPACE = 2;

	private int checkNonExistingWorkspace()
	{
		String[] options = new String[] { ResourceMgr.getString("LblCreateWorkspace"), ResourceMgr.getString("LblLoadWorkspace"), ResourceMgr.getString("LblIgnore")};
		JOptionPane ignorePane = new JOptionPane(ResourceMgr.getString("MsgProfileWorkspaceNotFound"), JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(this, ResourceMgr.TXT_PRODUCT_NAME);
		try
		{
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setResizable(true);
			dialog.pack();
			dialog.setVisible(true);
		}
		finally
		{
			dialog.dispose();
		}
		Object result = ignorePane.getValue();
		if (result == null) return CREATE_WORKSPACE;
		else if (result.equals(options[0])) return CREATE_WORKSPACE;
		else if (result.equals(options[1])) return LOAD_OTHER_WORKSPACE;
		else return IGNORE_MISSING_WORKSPACE;
	}

	private void handleWorkspaceLoadError(Throwable e, String realFilename)
	{
		String error = ExceptionUtil.getDisplay(e);
		String msg = StringUtil.replace(ResourceMgr.getString("ErrLoadingWorkspace"), "%error%", error);
		if (e instanceof OutOfMemoryError)
		{
			msg = ResourceMgr.getString("MsgOutOfMemoryError");
		}
		boolean create = WbSwingUtilities.getYesNo(this, msg);
		if (create)
		{
			this.currentWorkspaceFile = realFilename;
		}
		else
		{
			this.currentWorkspaceFile = null;
		}
	}

	private void resetWorkspace()
	{
		this.closeWorkspace(false);
	}

	private String getRealWorkspaceFilename(String filename)
	{
		if (filename == null) return filename;
		filename = FileDialogUtil.replaceConfigDir(filename);

		WbFile wfile = new WbFile(filename);
		if (!wfile.isAbsolute())
		{
			wfile = new WbFile(Settings.getInstance().getConfigDir(), filename);
			filename = wfile.getFullPath();
		}
		return filename;
	}

	public boolean loadWorkspace(String filename, boolean updateRecent)
	{
		if (this.isBusy())
		{
			WbSwingUtilities.showMessageKey(this, "ErrLoadWkspBusy");
			return false;
		}

		if (filename == null) return false;
		final String realFilename = getRealWorkspaceFilename(filename);

		WbFile f = new WbFile(realFilename);

	 	if (!f.exists())
		{
			// if the file does not exist, set all variables as if it did
			// thus the file will be created automatically.
			this.resetWorkspace();
			this.currentWorkspaceFile = realFilename;
			this.updateWindowTitle();
			this.checkWorkspaceActions();
			return true;
		}

		this.currentWorkspaceFile = null;
		this.resultForWorkspaceClose = false;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				WbWorkspace w = null;
				try
				{
					removeAllPanels(false);

					// Ignore all stateChanged() events from the SQL Tab during loading
					setIgnoreTabChange(true);

					w = new WbWorkspace(realFilename, false);
					final int entryCount = w.getEntryCount();

					for (int i = 0; i < entryCount; i++)
					{
						if (w.getPanelType(i) == PanelType.dbExplorer)
						{
							newDbExplorerPanel(false);
						}
						else
						{
							addTabAtIndex(false, false, false, -1);
						}
						MainPanel p = getSqlPanel(i);
            ((JComponent)p).validate();
						p.readFromWorkspace(w, i);
					}

					if (entryCount == 0)
					{
						LogMgr.logWarning("MainWindow.loadWorkspace()", "No panels stored in the workspace: " + realFilename);
						addTabAtIndex(false, false, false, -1);
					}

					currentWorkspaceFile = realFilename;
					resultForWorkspaceClose = true;

					renumberTabs();
					updateWindowTitle();
					checkWorkspaceActions();
					updateAddMacroAction();
					toolProperties.clear();
					toolProperties.putAll(w.getToolProperties());

					setIgnoreTabChange(false);

					int newIndex = entryCount > 0 ? w.getSelectedTab() : 0;
					if (newIndex < sqlTab.getTabCount())
					{
						sqlTab.setSelectedIndex(newIndex);
					}

					MainPanel p = getCurrentPanel();
					checkConnectionForPanel(p);
					setMacroMenuEnabled(true);
				}
				catch (Throwable e)
				{
					LogMgr.logWarning("MainWindow.loadWorkspace()", "Error loading workspace  " + realFilename, e);
					handleWorkspaceLoadError(e, realFilename);
					resultForWorkspaceClose = false;
				}
				finally
				{
					checkReloadWkspAction();
					setIgnoreTabChange(false);
					FileUtil.closeQuietely(w);
					updateGuiForTab(sqlTab.getSelectedIndex());
				}
			}
		});

		if (updateRecent)
		{
			RecentFileManager.getInstance().workspaceLoaded(f);
			EventQueue.invokeLater(this::updateRecentWorkspaces);
		}

    shouldShowTree = getToolProperties(DB_TREE_PROPS).getBoolProperty(DbTreePanel.PROP_VISIBLE, false);

    if (shouldShowTree && getCurrentSqlPanel() != null)
    {
      EventQueue.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          showDbTree(false);
          shouldShowTree = false;
        }
      });
    }

		BookmarkManager.getInstance().updateInBackground(this);

		return resultForWorkspaceClose;
	}

	private void checkReloadWkspAction()
	{
		String profileWkspName = currentProfile != null ? currentProfile.getWorkspaceFile() : null;

		if (StringUtil.isNonEmpty(profileWkspName))
		{
			boolean isProfileWorkspace = false;
			WbFile profileWksp = new WbFile(getRealWorkspaceFilename(profileWkspName));
			if (this.currentWorkspaceFile != null)
			{
				WbFile current = new WbFile(currentWorkspaceFile);
				isProfileWorkspace = current.equals(profileWksp);
			}
			this.reloadWorkspace.setEnabled(!isProfileWorkspace);
		}
		else
		{
			this.reloadWorkspace.setEnabled(false);
		}
	}

	public void loadCurrentProfileWorkspace()
	{
		if (this.currentProfile == null)
		{
			LogMgr.logError("MainWindow.loadCurrentProfileWorkspace()", "No current profile defined!", new IllegalStateException("No current profile"));
			return;
		}

		loadMacrosForProfile();

		String realFilename = null;
		try
		{
			boolean useDefault = false;
			String workspaceFilename = currentProfile.getWorkspaceFile();
			if (StringUtil.isBlank(workspaceFilename))
			{
				workspaceFilename = DEFAULT_WORKSPACE;
				useDefault = true;
			}

			realFilename = getRealWorkspaceFilename(workspaceFilename);

			WbFile f = new WbFile(realFilename);

			if (realFilename.length() > 0 && !f.exists())
			{
				int action = useDefault ? CREATE_WORKSPACE : this.checkNonExistingWorkspace();
				if (action == LOAD_OTHER_WORKSPACE)
				{
					FileDialogUtil util = new FileDialogUtil();
					workspaceFilename = util.getWorkspaceFilename(this, false, true);
					currentProfile.setWorkspaceFile(workspaceFilename);
				}
				else if (action == IGNORE_MISSING_WORKSPACE)
				{
					workspaceFilename = null;
					currentProfile.setWorkspaceFile(null);
				}
				else
				{
					// start with an empty workspace
					// and create a new workspace file.
					resetWorkspace();
				}
			}

			if (StringUtil.isNonBlank(workspaceFilename))
			{
				// loadWorkspace will replace the %ConfigDir% placeholder,
				// so we need to pass the original filename
				this.loadWorkspace(workspaceFilename, false);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.loadWorkspaceForProfile()", "Error reading workspace " + realFilename, e);
			this.handleWorkspaceLoadError(e, realFilename);
		}
	}

	public void forceDisconnect()
	{
		if (this.isConnectInProgress()) return;

		saveWorkspace(false);

		setConnectIsInProgress();
		showDisconnectInfo();
		try
		{
			final List<WbConnection> toAbort = new ArrayList<>();

			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					((SqlPanel)sql).forceAbort();
				}
				sql.disconnect();
				WbConnection con = sql.getConnection();
				if (con != null)
				{
					toAbort.add(con);
				}
				for (ToolWindow w : explorerWindows)
				{
					WbConnection conn = w.getConnection();
					if (conn != this.currentConnection && conn != null)
					{
						toAbort.add(conn);
					}
				}
			}
			closeExplorerWindows(false);
			WbThread abort = new WbThread("Abort connections")
			{
				@Override
				public void run()
				{
					ConnectionMgr.getInstance().abortAll(toAbort);
				}
			};
			abort.start();
		}
		finally
		{
			closeConnectingInfo();
			// this must be called on the AWT thread
			// and it must be called synchronously!
			WbSwingUtilities.invoke(() ->
      {
        disconnected(true);
      });
		}
	}

	@Override
	public void dispose()
	{
    if (treePanel != null)
    {
      treePanel.dispose();
    }
		sqlTab.removeAll();
		WbAction.dispose(
			this.assignWorkspaceAction, this.closeWorkspaceAction, this.reloadWorkspace, this.loadWorkspaceAction, this.saveAsWorkspaceAction, this.saveWorkspaceAction,
			this.dbExplorerAction, this.disconnectAction, this.reconnectAction, this.disconnectTab, this.createNewConnection,
			this.newDbExplorerPanel, this.newDbExplorerWindow, this.showDbTree, this.nextTab, this.prevTab, this.showDbmsManual,
      this.manageMacros, this.showMacroPopup, this.createMacro, this.loadMacros, this.saveMacros
		);
		for (JMenuBar bar : panelMenus)
		{
			disposeMenu(bar);
		}
		this.panelMenus.clear();
		this.explorerWindows.clear();
		JMenuBar bar = getJMenuBar();
		disposeMenu(bar);
		if (this.dropHandler != null)
		{
			this.dropHandler.dispose();
		}
		ShortcutManager.getInstance().removeChangeListener(this);
		super.dispose();
	}

	public void disconnect(final boolean background, final boolean closeWorkspace, final boolean saveWorkspace)
	{
		if (this.isConnectInProgress())
    {
      LogMgr.logWarning("MainWindow.disconnect()", "Cannot disconnect because a disconnect is already in progress");
      return;
    }

		setConnectIsInProgress();

		if (saveWorkspace) saveWorkspace(false);
		if (background) showDisconnectInfo();

		Runnable run = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					doDisconnect();
					if (closeWorkspace) closeWorkspace(background);
				}
				finally
				{
					clearConnectIsInProgress();
					if (background) closeConnectingInfo();
				}
			}
		};

		if (background)
		{
			Thread t = new WbThread(run, "MainWindow Disconnect");
			t.start();
		}
		else
		{
			run.run();
		}
	}

  private void saveCache()
  {
    MainPanel panel = getCurrentPanel();
    if (panel == null) return;
    WbConnection conn = panel.getConnection();
    if (conn == null) return;
    DbObjectCacheFactory.getInstance().saveCache(conn);
  }

	/**
	 *	This does the real disconnect action.
	 */
	protected void doDisconnect()
	{
    saveCache();

		try
		{
      if (treePanel != null)
      {
        treePanel.disconnect();
      }

			WbConnection conn = null;

			for (int i = 0; i < this.sqlTab.getTabCount(); i++)
			{
				final MainPanel panel = (MainPanel) this.sqlTab.getComponentAt(i);
				if (panel instanceof SqlPanel)
				{
					((SqlPanel) panel).abortExecution();
				}
				conn = panel.getConnection();
				panel.disconnect();
				if (conn != null && !conn.isClosed())
				{
					showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
					conn.disconnect();
				}
			}
			closeExplorerWindows(true);
		}
		finally
		{
			// this must be called on the AWT thread
			// and it must be called synchronously!
			WbSwingUtilities.invoke(() ->
      {
        disconnected(false);
      });
		}
	}

	protected void disconnected(boolean closeWorkspace)
	{
		this.currentProfile = null;
		this.currentConnection = null;
		if (closeWorkspace)
		{
			this.closeWorkspace(false);
		}
		this.setMacroMenuEnabled(false);
		getJobIndicator().allJobsEnded();
		this.updateWindowTitle();
		this.disconnectAction.setEnabled(false);
		this.reconnectAction.setEnabled(false);
		showDbmsManual.setDbms(null, -1, -1);
    connectionInfoAction.setEnabled(false);
		this.createNewConnection.checkState();
		this.disconnectTab.checkState();
		this.dbExplorerAction.setEnabled(false);
		this.newDbExplorerPanel.setEnabled(false);
		this.newDbExplorerWindow.setEnabled(false);
		if (showDbTree != null) showDbTree.setEnabled(false);
		this.showStatusMessage("");
		for (int i=0; i < sqlTab.getTabCount(); i++)
		{
			sqlTab.setForegroundAt(i, null);
		}
	}

	public void abortAll()
	{
		try
		{
			for (int i=0; i < this.sqlTab.getTabCount(); i++)
			{
				MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
				if (sql instanceof SqlPanel)
				{
					SqlPanel sp = (SqlPanel)sql;
					sp.forceAbort();
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MainWindow.abortAll()", "Error stopping execution",e);
		}
	}

	public void requestEditorFocus()
	{
		SqlPanel sql = this.getCurrentSqlPanel();
		if (sql != null)
		{
			sql.requestEditorFocus();
		}
	}

	private void selectCurrentEditor()
	{
		SqlPanel sql = this.getCurrentSqlPanel();
		if (sql != null)
		{
			sql.selectEditorLater();
		}
	}

	protected String getCurrentEditorFile()
	{
		String filename = null;

		SqlPanel sql = this.getCurrentSqlPanel();
		if (sql != null)
		{
			filename = sql.getCurrentFileName();
		}
		return filename;
	}

	protected synchronized RunningJobIndicator getJobIndicator()
	{
		if (this.jobIndicator == null)
		{
			this.jobIndicator = new RunningJobIndicator(this);
		}

		return this.jobIndicator;
	}

	protected void updateWindowTitle()
	{
		EventQueue.invokeLater(() ->
    {
      WindowTitleBuilder titleBuilder = new WindowTitleBuilder();
      String title1 = titleBuilder.getWindowTitle(currentProfile, currentWorkspaceFile, getCurrentEditorFile());
      setTitle(title1);
      getJobIndicator().baseTitleChanged();
    });
	}

	protected void closeConnectingInfo()
	{
		getSelector().closeConnectingInfo();
	}

	protected void showDisconnectInfo()
	{
		getSelector().showDisconnectInfo();
	}

	/**
	 * Display a little PopupWindow to tell the user that the
	 * workbench is currently connecting to the DB
	 */
	protected void showConnectingInfo()
	{
		getSelector().showConnectingInfo();
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
		selectConnection(false);
	}

	public void selectConnection(boolean exit)
	{
		exitOnCancel = exit;
		getSelector().selectConnection();
	}

	public JMenu getRecentWorkspaceMenu(int panelIndex)
	{
		JMenu main = this.getMenu(ResourceMgr.MNU_TXT_WORKSPACE, panelIndex);
		if (main == null) return null;
		int count = main.getItemCount();
		for (int i=0; i < count; i ++)
		{
			JMenuItem item = main.getItem(i);
			if (item == null) continue;
			if ("recent-workspace".equals(item.getName()))
			{
				return (JMenu)item;
			}
		}
		return null;
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
		if (aName == null) return null;
		JMenuBar menubar = this.panelMenus.get(panelIndex);
		int count = menubar.getMenuCount();
		for (int k=0; k < count; k++)
		{
			JMenu item = menubar.getMenu(k);
			if (item == null) continue;
			if (aName.equals(item.getName())) return item;
		}
		return null;
	}

	protected void updateRecentWorkspaces()
	{
		for (int i=0; i < getTabCount(); i++)
		{
			JMenu menu = getRecentWorkspaceMenu(i);
			RecentFileManager.getInstance().populateRecentWorkspaceMenu(menu, this);
		}
	}

	protected void updateViewMenu(int sqlTabIndex, String aName)
	{
		int panelCount = this.panelMenus.size();
		if (aName == null) aName = ResourceMgr.getDefaultTabLabel();
		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);
      if (view == null) continue;

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
						a.setMenuText(aName);
						break;
					}
				}
			}
			WbSwingUtilities.repaintNow(view);
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
					view.insert(anAction, k);
					inserted = k;
					break;
				}
			}

			if (inserted == -1)
			{
				if (lastActionIndex == -1)
				{
					// no index found which is greater than or equal to the new one
					// so add it to the end
					if (!(view.getItem(count - 1).getAction() instanceof SelectTabAction))
					{
						view.addSeparator();
					}
					view.add(anAction);
				}
				else if (lastAction != null && lastAction.getIndex() != anAction.getIndex())
				{
					// we found at least one SelectTabAction, so we'll
					// insert the new one right behind the last one.
					// (there might be other items in the view menu!)

					view.insert(anAction, lastActionIndex + 1);
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

	/**
	 * Tell all SelectTabAction that the shortcuts have changed, so that they can update accordingly.
	 *
	 * @see SelectTabAction#setNewIndex(int)
	 */
	private void updateViewShortcuts()
	{
		int panelCount = sqlTab.getTabCount();
		for (int i=0; i < panelCount; i++)
		{
			JMenu view = this.getViewMenu(i);
			for (int k=0; k < panelCount; k++)
			{
				JMenuItem item = view.getItem(k);
				Action a = item.getAction();
				if (a instanceof SelectTabAction)
				{
					SelectTabAction tab = (SelectTabAction)a;
					tab.setNewIndex(k);
				}
			}
		}
	}

	private WbConnection getConnectionForTab(MainPanel aPanel, boolean returnNew)
		throws Exception
	{
		if (this.currentConnection != null && !returnNew) return this.currentConnection;
		String id = this.getConnectionIdForPanel(aPanel);

		StatusBar status = null;
		if (aPanel instanceof StatusBar)
		{
			status = (StatusBar)aPanel;
			status.setStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + this.currentProfile.getName() + " ...");
		}

		ConnectionMgr mgr = ConnectionMgr.getInstance();
		WbConnection conn = null;
		try
		{
			conn = mgr.getConnection(this.currentProfile, id);
		}
		finally
		{
			if (status != null) status.clearStatusMessage();
		}
		showConnectionWarnings(conn, aPanel);
		return conn;
	}

	private void showConnectionWarnings(WbConnection conn, MainPanel panel)
	{
		if (panel == null) return;
		String warn = (conn != null ? conn.getWarnings() : null);
		if (warn != null && panel != null)
		{
			panel.showResultPanel();
			panel.showLogMessage(ResourceMgr.getString("MsgConnectMsg") + "\n");
			panel.appendToLog(warn);
		}
	}

	public void addDbExplorerTab(DbExplorerPanel explorer)
	{
		JMenuBar dbmenu = this.createMenuForPanel(explorer);

		this.sqlTab.add(explorer);

		explorer.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);

		SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);
		action.setMenuText(explorer.getTabTitle());
		this.panelMenus.add(dbmenu);
		this.addToViewMenu(action);
	}

	public List<ToolWindow> getExplorerWindows()
	{
		return Collections.unmodifiableList(explorerWindows);
	}

	public void closeExplorerWindows(boolean doDisconnect)
	{
		try
		{
			List<ToolWindow> copy = new ArrayList<>(explorerWindows);
			for (ToolWindow w : copy)
			{
				WbConnection conn = w.getConnection();
				if (conn != null && doDisconnect && conn != this.currentConnection)
				{
					conn.disconnect();
				}
				w.closeWindow();
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.closeExplorerWindows()", "Error when closing explorer windows", th);
		}
	}

	public void closeOtherPanels(MainPanel toKeep)
	{
		if (GuiSettings.getConfirmTabClose())
		{
			boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseOtherTabs"));
			if (!doClose) return;
		}

		boolean inProgress = connectInProgress;
		if (!inProgress) this.setConnectIsInProgress();

		try
		{
			setIgnoreTabChange(true);
			int index = 0;
			while (index < sqlTab.getTabCount())
			{
				MainPanel p = getSqlPanel(index);

				if (p != toKeep && !p.isLocked())
				{
					if (p.isModified())
					{
						// if the panel is modified the user will be asked
						// if the panel should really be closed, in that
						// case I think it makes sense to make that panel the current panel
						selectTab(index);
						// tabSelected will not be run because tabRemovalInProgress == true
						tabSelected(index);
					}
					if (p.canClosePanel(true))
					{
						removeTab(index, false);
					}
					else
					{
						// if canCloseTab() returned false, then the user
						// selected "Cancel" which means stop closing panels
						// if the user selected "No" canCloseTab() will return "true"
						// to indicate whatever is in progress can go on.
						break;
					}
				}
				else
				{
					index ++;
				}
			}
			renumberTabs();
			// make sure the toolbar and menus are updated correctly
			updateCurrentTab(getCurrentPanelIndex());
			tabHistory.clear();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.removeAllPanels()", "Error when removing all panels", e);
		}
		finally
		{
			setIgnoreTabChange(false);
			if (!inProgress) clearConnectIsInProgress();
		}
	}

	protected void removeAllPanels(boolean keepOne)
	{
		boolean inProgress = connectInProgress;
		if (!inProgress) this.setConnectIsInProgress();

		BookmarkManager.getInstance().clearBookmarksForWindow(getWindowId());

		try
		{
			setIgnoreTabChange(true);
      closeDbTree();
			int keep = (keepOne ? 1 : 0);
			while (sqlTab.getTabCount() > keep)
			{
				// I'm not using removeCurrentTab() as that will also
				// update the GUI and immediately check for a new
				// connection which is not necessary when removing all tabs.
				removeTab(keep, false);
			}

			// Reset the first panel, now we have a "clean" workspace
			if (keepOne)
			{
				MainPanel p = getSqlPanel(0);
				if (p == null)
				{
					addTabAtIndex(false, false, false, -1);
					p = getSqlPanel(0);
				}
        else
        {
          p.reset();
        }
				resetTabTitles();

				// make sure the toolbar and menus are updated correctly
				updateCurrentTab(0);
			}
			tabHistory.clear();
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.removeAllPanels()", "Error when removing all panels", e);
		}
		finally
		{
			setIgnoreTabChange(false);
			if (!inProgress) clearConnectIsInProgress();
		}
	}

	/**
	 *	Returns the index of the first explorer tab
	 */
	public int findFirstExplorerTab()
	{
		int count = this.sqlTab.getTabCount();
		if (count <= 0) return -1;

		for (int i=0; i < count; i++)
		{
			Component c = this.sqlTab.getComponentAt(i);
			if (c instanceof DbExplorerPanel) return i;
		}
		return -1;
	}

	public void newDbExplorerWindow()
	{
		DbExplorerPanel explorer = new DbExplorerPanel(this);
		explorer.restoreSettings();
		DbExplorerWindow w = explorer.openWindow(this.currentProfile);

		boolean useNewConnection = DbExplorerSettings.getAlwaysUseSeparateConnForDbExpWindow()
			      || currentProfile.getUseSeparateConnectionPerTab()
						|| currentConnection == null;

		if (useNewConnection)
		{
			explorer.connect(this.currentProfile);
		}
		else
		{
			LogMgr.logDebug("MainWindow.newDbExplorerWindow()", "Re-using current connection for DbExplorer Window");
			explorer.setConnection(this.currentConnection);
		}
		explorerWindows.add(w);
	}

	public void explorerWindowClosed(DbExplorerWindow w)
	{
		explorerWindows.remove(w);
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

	public ConnectionProfile getCurrentProfile()
	{
		return this.currentProfile;
	}

	public JMenu buildHelpMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_HELP));
		result.setName(ResourceMgr.MNU_TXT_HELP);
		new ShowHelpAction().addToMenu(result);
		new ShowManualAction().addToMenu(result);
		if (showDbmsManual == null)
		{
			showDbmsManual = new ShowDbmsManualAction();
		}
    if (connectionInfoAction == null)
    {
      connectionInfoAction = new HelpConnectionInfoAction(this);
      connectionInfoAction.setEnabled(false);
    }
		result.add(showDbmsManual);
    result.add(connectionInfoAction);
    result.addSeparator();
		result.add(new HelpContactAction(this));
		result.add(WhatsNewAction.getInstance());
		result.addSeparator();

		result.add(ViewLogfileAction.getInstance());
		result.add(new VersionCheckAction());
		result.add(new AboutAction(this));

		return result;
	}

	/**
	 * Create the tools menu for a panel menu. This will be called
	 * for each panel that gets added to the main window.
	 * Actions that are singletons (like the db explorer stuff)
	 * should not be created here
	 */
	public JMenu buildToolsMenu()
	{
		JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TOOLS));
		result.setName(ResourceMgr.MNU_TXT_TOOLS);

		result.add(this.dbExplorerAction);
		result.add(this.newDbExplorerPanel);
		result.add(this.newDbExplorerWindow);
    if (showDbTree != null)
    {
      result.add(this.showDbTree);
    }
		result.addSeparator();

		result.add(new DataPumperAction(this));
		result.add(new ObjectSearchAction(this));

		result.addSeparator();
		result.add(new BookmarksAction(this));
    result.add(new SearchAllEditorsAction(this));

		result.addSeparator();
		new OptionsDialogAction().addToMenu(result);
		new ConfigureShortcutsAction().addToMenu(result);

		return result;
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
				sql.closeFile(true, false);
				this.setTabTitle(i, defaultTitle);
			}
		}
	}

	public boolean isCancelling()
	{
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isCancelling()) return true;
		}
		return false;

	}

	public boolean isConnected()
	{
		if (this.currentConnection != null)
		{
			return true;
		}
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = this.getSqlPanel(i);
			if (p.isConnected()) return true;
		}
		return false;
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
		this.saveWorkspace();
		FileDialogUtil dialog = new FileDialogUtil();
		String filename = dialog.getWorkspaceFilename(this, false, true);
		if (filename == null) return;
		boolean loaded = this.loadWorkspace(filename, true);
		if (loaded && Settings.getInstance().getBoolProperty("workbench.gui.workspace.load.askassign", true))
		{
			checkMakeProfileWorkspace();
		}
		WbSwingUtilities.repaintLater(this);
	}

	/**
	 *	Closes the current workspace.
	 *  The tab count is dispose to 1, the SQL history for the tab will be emptied
  and the workspace filename will be "forgotten".
	 */
	public void closeWorkspace(boolean checkUnsaved)
	{
		this.currentWorkspaceFile = null;

		if (checkUnsaved)
		{
			int count = this.sqlTab.getTabCount();
			boolean first = true;
			for (int i=0; i < count; i++)
			{
				MainPanel p = getSqlPanel(i);
				if (currentConnection != null)
				{
					first = i == 0;
				}
				if (!p.canClosePanel(first)) return;
			}
		}

		WbSwingUtilities.invoke(() ->
    {
      try
      {
        removeAllPanels(true);
      }
      catch (Exception e)
      {
        LogMgr.logError("MainWindow.closeWorkspace()", "Error when resetting workspace", e);
      }
      updateWindowTitle();
      checkWorkspaceActions();
    });
	}

	/**
	 *	This will assign the current workspace name to the current profile.
	 */
	public void assignWorkspace()
	{
		if (this.currentWorkspaceFile == null) return;
		if (this.currentProfile == null) return;
		FileDialogUtil util = new FileDialogUtil();
		String filename = util.removeConfigDir(this.currentWorkspaceFile);
		this.currentProfile.setWorkspaceFile(filename);

		// The MainWindow gets a copy of the profile managed by the ConnectionMgr
		// so we need to update that one as well.
		ConnectionProfile realProfile = ConnectionMgr.getInstance().getProfile(currentProfile.getKey());
		if (realProfile != null)
		{
			realProfile.setWorkspaceFile(filename);
		}
		this.updateWindowTitle();
	}

	/**
	 *	Save the currently loaded workspace.
	 */
	public boolean saveWorkspace()
	{
		return saveWorkspace(true);
	}

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
	 *
	 *  If the workspace is saved with a new name (filename == null) the user
	 *  will be asked if the workspace should be assigned to the current profile
	 */
	public boolean saveWorkspace(String filename, boolean checkUnsaved)
	{
		if (!WbManager.getInstance().getSettingsShouldBeSaved()) return true;
		WbWorkspace w = null;
		boolean interactive = false;

		if (filename == null)
		{
			interactive = true;
			FileDialogUtil util = new FileDialogUtil();
			filename = util.getWorkspaceFilename(this, true, true);
			if (filename == null) return true;
		}

		String realFilename = getRealWorkspaceFilename(filename);
		WbFile f = new WbFile(realFilename);

		if (Settings.getInstance().getCreateWorkspaceBackup())
		{
			int maxVersions = Settings.getInstance().getMaxBackupFiles();
			String dir = Settings.getInstance().getBackupDir();
			String sep = Settings.getInstance().getFileVersionDelimiter();
			FileVersioner version = new FileVersioner(maxVersions, dir, sep);
			try
			{
				version.createBackup(f);
			}
			catch (IOException e)
			{
				LogMgr.logWarning("MainWindow.saveWorkspace()", "Error when creating backup file!", e);
			}
		}
		else if (WbManager.getInstance().outOfMemoryOcurred())
		{
			// sometimes when an OoM occurred, saving of the workspace
			// succeeds but the ZIP file is not written correctly.
			// This tries to prevent the old file from beeing overwritten, just in case...
			f.makeBackup();
		}

		this.showMacroPopup.saveWorkspaceSettings();

    getToolProperties(DB_TREE_PROPS).setProperty(DbTreePanel.PROP_VISIBLE, isDbTreeVisible());

    if (treePanel != null)
    {
      treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));
    }

		try
		{
			int count = this.sqlTab.getTabCount();

			if (checkUnsaved)
			{
				boolean first = true;
				for (int i=0; i < count; i++)
				{
					MainPanel p = (MainPanel)this.sqlTab.getComponentAt(i);
					if (currentConnection != null)
					{
						// for a global connection only the first tab needs to check open transactions
						first = (i == 0);
					}
					if (!p.canClosePanel(first)) return false;
				}
			}
			w = new WbWorkspace(realFilename, true);
			w.setToolProperties(this.toolProperties);
			int selected = this.sqlTab.getSelectedIndex();
			w.setSelectedTab(selected);
			w.setEntryCount(count);
			for (int i=0; i < count; i++)
			{
				MainPanel p = getSqlPanel(i);
				p.saveToWorkspace(w,i);
			}
			LogMgr.logDebug("MainWindow.saveWorkspace()", "Workspace " + filename + " saved");
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindow.saveWorkspace()", "Error saving workspace: " + filename, e);
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrSavingWorkspace") + "\n" + ExceptionUtil.getDisplay(e));
		}
		finally
		{
			FileUtil.closeQuietely(w);
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
	 *	Invoked when the a different SQL panel has been selected.
	 *
	 *  This fires the tabSelected() method but only if ignoreTabChange is not set true.
	 *
	 *  @param e  a ChangeEvent object
	 *
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.sqlTab)
		{
			if (this.ignoreTabChange)	return;
			int index = this.sqlTab.getSelectedIndex();
			this.tabSelected(index);
		}
		else if (e.getSource() == ShortcutManager.getInstance())
		{
			updateViewShortcuts();
		}
	}

	public MainPanel insertTab()
	{
		return addTab(true, true, false, true);
	}

	public MainPanel addTab()
	{
		return this.addTab(true, true, true, true);
	}

	/**
	 * Add a SqlPanel to this window.
	 *
	 * @param checkConnection if true, the panel will automatically be connected
	 *                        this is important if a Profile is used where each panel gets its own
	 *                        connection
	 * @param append          if true, the tab will be appended at the end (after all other tabs), if false will be
	 *                        inserted before the current tab.
	 * @param renumber        should the tabs be renumbered after adding the new tab. If several tabs are added
	 *                        in a loop renumber is only necessary at the end
	 *
	 * @see #renumberTabs()
	 * @see #checkConnectionForPanel(workbench.interfaces.MainPanel)
	 */
	public MainPanel addTab(boolean selectNew, boolean checkConnection, boolean append, boolean renumber)
	{
		int index = -1;
		if (append)
		{
			index = findFirstExplorerTab();
			if (index < sqlTab.getTabCount() - 1)
			{
				index = -1;
			}
		}
		else
		{
			index = this.sqlTab.getSelectedIndex() + 1;
		}

		try
		{
			setIgnoreTabChange(true);
			MainPanel p = addTabAtIndex(selectNew, checkConnection, renumber, index);
			if (selectNew)
			{
				currentTabChanged();
			}
			return p;
		}
		finally
		{
			setIgnoreTabChange(false);
		}
	}

	private MainPanel addTabAtIndex(boolean selectNew, boolean checkConnection, boolean renumber, int index)
	{
		final SqlPanel sql = new SqlPanel(getMacroClientId());
    sql.registerObjectFinder(treePanel);
		addTabAtIndex(sql, selectNew, checkConnection, renumber, index);
		return sql;
	}

	private void addTabAtIndex(SqlPanel sql, boolean selectNew, boolean checkConnection, boolean renumber, int index)
	{
		if (index == -1) index = sqlTab.getTabCount();

		sql.setConnectionClient(this);
		sql.addDbExecutionListener(this);
		sql.addFilenameChangeListener(this);
		this.sqlTab.add(sql, index);
		sql.setTabTitle(sqlTab, index);

		JMenuBar menuBar = this.createMenuForPanel(sql);
		this.panelMenus.add(index, menuBar);

		if (checkConnection) this.checkConnectionForPanel(sql);

		this.setMacroMenuEnabled(sql.isConnected());

		if (renumber) this.renumberTabs();
		sql.initDivider(sqlTab.getHeight() - sqlTab.getTabHeight());

		if (selectNew)
		{
			// if no connection was created initially the switch to a new
			// panel will initiate the connection.
			this.sqlTab.setSelectedIndex(index);
		}

		if (sqlTab.getTabCount() > 0)
		{
			sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
		}
	}

	public void jumpToBookmark(final NamedScriptLocation bookmark)
	{
		if (bookmark == null) return;
		final	int index = getTabIndexById(bookmark.getTabId());
		if (index < 0)
		{
			LogMgr.logWarning("MainWindow.jumpToBookmark()", "Tab with ID=" + bookmark.getTabId() + " not found!");
			return;
		}
		final MainPanel p = getSqlPanel(index);
		final boolean selectTab = index != sqlTab.getSelectedIndex();
		EventQueue.invokeLater(() ->
    {
      if (selectTab)
      {
        selectTab(index);
        invalidate();
      }
      p.jumpToBookmark(bookmark);
    });
	}

	/**
	 * Returns the real title of a tab (without the index number or any formatting)
	 *
	 * @see MainPanel#getTabTitle()
	 */
	public String getTabTitle(int index)
	{
		MainPanel panel = getSqlPanel(index);
		return panel.getTabTitle();
	}

	private int getTabIndexById(String tabId)
	{
		int count = sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			MainPanel p = getSqlPanel(i);
			if (p.getId().equals(tabId)) return i;
		}
		return -1;
	}

  /**
   * Returns the tab title as displayed to the user (including the index number).
   *
   * @param tabId  the ID of the panel
   * @return the title as shown to the user
   */
	public String getTabTitleById(String tabId)
	{
		int index = getTabIndexById(tabId);
    if (index > -1)
    {
      return HtmlUtil.cleanHTML(sqlTab.getTitleAt(index));
    }
		return null;
	}

	/**
	 * Returns the title of the currently selected tab.
	 *
	 * @see #getTabTitle(int)
	 * @see MainPanel#getTabTitle()
	 */
	@Override
	public String getCurrentTabTitle()
	{
		int index = this.sqlTab.getSelectedIndex();
		return this.getTabTitle(index);
	}

	@Override
	public void setCurrentTabTitle(String newName)
	{
		int index = this.sqlTab.getSelectedIndex();

		if (newName != null)
		{
			this.setTabTitle(index, newName);
		}
	}
	/**
	 *	Sets the title of a tab and appends the index number to
	 *  the title, so that a shortcut Ctrl-n can be defined
	 */
	public void setTabTitle(int anIndex, String aName)
	{
		MainPanel p = this.getSqlPanel(anIndex);
		p.setTabName(aName);
		p.setTabTitle(this.sqlTab, anIndex);
		updateViewMenu(anIndex, p.getTabTitle());
	}

	public void removeLastTab(boolean includeExplorer)
	{
		int index = this.sqlTab.getTabCount() - 1;
		if (!includeExplorer)
		{
			while (this.getSqlPanel(index) instanceof DbExplorerPanel)
			{
				index --;
			}
		}
		this.tabCloseButtonClicked(index);
	}

	/**
	 * Checks if the current tab is locked, or if it is the
	 * last tab that is open.
	 * <br/>
	 * This does not check if the user actually wants to close
	 * the tab!
	 *
	 * @return boolean if the current tab could be closed
	 */
	public boolean canCloseTab()
	{
		int index = sqlTab.getSelectedIndex();
		return canCloseTab(index);
	}

	@Override
	public boolean canCloseTab(int index)
	{
		if (index < 0) return false;
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return false;
		if (panel.isLocked()) return false;

		int numTabs = sqlTab.getTabCount();
		return numTabs > 1;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public boolean canRenameTab()
	{
		return (this.currentWorkspaceFile != null);
	}

	/**
	 * Closes the currently active tab.
	 *
	 * @see #tabCloseButtonClicked(int)
	 */
	public void removeCurrentTab()
	{
		int index = this.sqlTab.getSelectedIndex();
		this.tabCloseButtonClicked(index);
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
			item.removeAll();
			menu.remove(0);
			item = menu.getItem(0);
		}
		int count = this.sqlTab.getTabCount();
		for (int i=0; i < count; i++)
		{
			SelectTabAction a = new SelectTabAction(sqlTab, i);
			a.setMenuText(getTabTitle(i));
			menu.insert(a, i);
		}
		if (this.sqlTab.getSelectedIndex() == panel)
		{
			WbSwingUtilities.repaintNow(menu);
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
		moveTab(index, index - 1);
	}

	/**
	 * Moves the current sql tab to the right (i.e. index := index + 1)
	 * If oldIndex denotes the last SQL Tab, nothing happens
	 */
	public void moveTabRight()
	{
		int index = this.getCurrentPanelIndex();
		int lastIndex = sqlTab.getTabCount();
		if (index >= lastIndex) return;
		moveTab(index, index + 1);
	}

	@Override
	public void moveCancelled()
	{
	}

	@Override
	public void endMove(int finalIndex)
	{
		tabSelected(finalIndex);
	}

	@Override
	public boolean startMove(int index)
	{
		return true;
	}

	private void setIgnoreTabChange(boolean flag)
	{
		this.ignoreTabChange = flag;
	}

	@Override
	public boolean moveTab(int oldIndex, int newIndex)
	{
		MainPanel panel = this.getSqlPanel(oldIndex);

		JMenuBar oldMenu = this.panelMenus.get(oldIndex);
		this.sqlTab.remove(oldIndex);
		this.panelMenus.remove(oldIndex);
		this.panelMenus.add(newIndex, oldMenu);

		this.sqlTab.add((JComponent)panel, newIndex);
		this.sqlTab.setSelectedIndex(newIndex);

		renumberTabs();
		this.validate();
		return true;
	}

	/**
	 * Removes the tab at the given location. If the current profile
	 * uses a separate connection per tab, then a disconnect will be
	 * triggered as well. This disconnect will be started in a
	 * background thread.
	 * <br/>
	 * The user will not be
	 */
	@Override
	public void tabCloseButtonClicked(int index)
	{
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return;
		if (!panel.canClosePanel(true)) return;

		if (GuiSettings.getConfirmTabClose())
		{
			boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseTab"));
			if (!doClose) return;
		}

		if (GuiSettings.getUseLRUForTabs())
		{
			tabHistory.restoreLastTab();
		}
		removeTab(index, true);
	}

	/**
	 * Removes the indicated tab without checking for modified file etc.
	 * If the tab has a separate connection, the connection is closed (disconnected)
	 * as well.
	 * If a single connection for all tabs is used, the connection is <b>not</b> closed.
	 */
	protected void removeTab(int index, boolean updateGUI)
	{
		MainPanel panel = this.getSqlPanel(index);
		if (panel == null) return;

		int newTab = -1;

		boolean inProgress = this.isConnectInProgress();
		if (!inProgress) this.setConnectIsInProgress();

		try
		{
			setIgnoreTabChange(true);

			WbConnection conn = panel.getConnection();

			// this does not really close the connection
			// it simply tells the panel that it should
			// release anything attached to the connection!
			// the actual disconnect from the DB is done afterwards
			// through the ConnectionMgr
			panel.disconnect();

			panel.dispose();

			BookmarkManager.getInstance().clearBookmarksForPanel(getWindowId(), panel.getId());

			boolean doDisconnect = conn != null && this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab();

			if (doDisconnect)
			{
				showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
				conn.disconnect();
				showStatusMessage("");
			}
			disposeMenu(panelMenus.get(index));
			this.panelMenus.remove(index);
			this.sqlTab.removeTabAt(index);

			if (updateGUI)
			{
				this.renumberTabs();
				newTab = this.sqlTab.getSelectedIndex();
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MainWindows.removeTab()", "Error removing tab index=" + index,e);
		}
		finally
		{
			setIgnoreTabChange(false);
			if (!inProgress) this.clearConnectIsInProgress();
		}
		if (newTab >= 0 && updateGUI)
		{
			this.tabSelected(newTab);
		}

		if (sqlTab.getTabCount() > 0)
		{
			sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
		}
	}

	private void disposeMenu(JMenuBar menuBar)
	{
		if (menuBar == null) return;
		int count = menuBar.getMenuCount();
		for (int i=0; i < count; i++)
		{
			JMenu menu = menuBar.getMenu(i);
			menu.removeAll();
		}
		menuBar.removeAll();
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == this.sqlTab)
		{
			Point p = e.getPoint();
			int index = sqlTab.indexAtLocation(p.x, p.y);

			if (e.getButton() == MouseEvent.BUTTON2)
			{
				if (this.canCloseTab())
				{
					this.removeCurrentTab();
				}
			}

			if (e.getButton() == MouseEvent.BUTTON3)
			{
				SqlTabPopup pop = new SqlTabPopup(this);
				pop.show(this.sqlTab,e.getX(),e.getY());
			}
			else if (index == -1 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
			{
				this.addTab();
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		getJobIndicator().jobEnded();
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
		if (Settings.getInstance().getAutoSaveWorkspace())
		{
			this.saveWorkspace(false);
		}
		getJobIndicator().jobStarted();
	}
}
