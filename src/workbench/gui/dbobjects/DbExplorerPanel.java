/*
 * DbExplorerPanel.java
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.CatalogChanger;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.bookmarks.NamedScriptLocation;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.FlatButton;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.dbobjects.objecttree.ObjectFinder;
import workbench.gui.sql.PanelTitleSetter;
import workbench.gui.sql.PanelType;

import workbench.util.ExceptionUtil;
import workbench.util.HtmlUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbProperties;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;


/**
 * The main container panel for the DbExplorer.
 *
 * This panel incorporates the panels to
 * <ul>
 *	<li>Display a list of tables, views, etc {@link workbench.gui.dbobjects.TableListPanel}</li>
 *  <li>Display a list of procedures: {@link workbench.gui.dbobjects.ProcedureListPanel}</li>
 *	<li>Allow search across several table and columns: {@link workbench.gui.dbobjects.TableSearchPanel}</li>
 * </ul>
 * This panel can either be displayed inside the MainWindow as a tab, or as
 * a separate Window (@link workbench.gui.dbobjects.DbExplorerWindow}
 * @author  Thomas Kellerer
 */
public class DbExplorerPanel
	extends JPanel
	implements ActionListener, MainPanel, DbExecutionListener, PropertyChangeListener, Reloadable
{
  private JTabbedPane tabPane;
  protected TableListPanel tables;
  protected TableSearchPanel searchPanel;
  protected ProcedureListPanel procs;
  protected TriggerListPanel triggers;
  protected JComboBox schemaSelector;
  private JComboBox catalogSelector;
  private JLabel schemaLabel;
  private JLabel catalogLabel;
  private JPanel selectorPanel;
  private WbConnection dbConnection;
  private DbExplorerWindow window;
  private WbToolbar toolbar;
  private ConnectionInfo connectionInfo;
  protected boolean retrievePending;
  private boolean schemaRetrievePending = true;
  private boolean connectionInitPending = true;
  private int internalId = 0;
  private ConnectionSelector connectionSelector;
  private JButton selectConnectionButton;
  private String tabTitle;
  private static int instanceCount = 0;
  private MainWindow mainWindow;
  private boolean busy;
  private String schemaFromWorkspace;
  private String catalogFromWorkspace;
  private boolean switchCatalog;
  private JComponent currentFocus;
  private ReloadAction reloadSchemasAction;
  private JButton reloadButton;
  private boolean locked;
  protected String tabName;

  public DbExplorerPanel()
  {
    this(null);
  }

	public DbExplorerPanel(MainWindow aParent)
	{
		super();
		this.internalId = ++instanceCount;
		this.mainWindow = aParent;
		setName("dbexplorer");
		try
		{
			tables = new TableListPanel(aParent);
			tables.setName("tablelistpanel");
			procs = new ProcedureListPanel(aParent);
			this.searchPanel = new TableSearchPanel(tables);
			tabPane = new WbTabbedPane(JTabbedPane.TOP);
			tabPane.add(ResourceMgr.getString("TxtDbExplorerTables"), tables);
			tabPane.setToolTipTextAt(0, ResourceMgr.getDescription("TxtDbExplorerTables"));
			setDbExecutionListener(aParent);

			String tabLocation = Settings.getInstance().getProperty("workbench.gui.dbobjects.maintabs", "top");
			int location = JTabbedPane.TOP;
			if (tabLocation.equalsIgnoreCase("bottom"))
			{
				location = JTabbedPane.BOTTOM;
			}
			else if (tabLocation.equalsIgnoreCase("left"))
			{
				location = JTabbedPane.LEFT;
			}
			else if (tabLocation.equalsIgnoreCase("right"))
			{
				location = JTabbedPane.RIGHT;
			}
			tabPane.setTabPlacement(location);

			tabPane.add(ResourceMgr.getString("TxtDbExplorerProcs"), procs);
			tabPane.setToolTipTextAt(1, ResourceMgr.getDescription("TxtDbExplorerProcs"));

			if (DbExplorerSettings.getShowTriggerPanel())
			{
				triggers = new TriggerListPanel(aParent);
				tabPane.add(ResourceMgr.getString("TxtDbExplorerTriggers"), triggers);
				tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, ResourceMgr.getDescription("TxtDbExplorerTriggers"));
			}
			tabPane.add(ResourceMgr.getString("TxtSearchTables"), this.searchPanel);
			tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, ResourceMgr.getDescription("TxtSearchTables"));
			tabPane.setFocusable(false);

			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			this.setLayout(new BorderLayout());

			reloadSchemasAction = new ReloadAction(this);
			reloadSchemasAction.setEnabled(false);
			reloadSchemasAction.setTooltip(ResourceMgr.getString("TxtReload"));
      reloadSchemasAction.setUseLabelIconSize(true);

			schemaLabel = new JLabel(ResourceMgr.getString("LblSchema"));

			this.schemaSelector = new JComboBox();
      this.schemaSelector.setMaximumRowCount(15);

			this.catalogSelector = new JComboBox();
			this.catalogSelector.setMaximumRowCount(15);
			this.catalogLabel = new JLabel("Catalog");
			this.catalogSelector.setVisible(false);
			this.catalogSelector.setEnabled(false);
			this.catalogLabel.setVisible(false);

			reloadButton = new FlatButton(reloadSchemasAction);
			reloadButton.setText(null);
			reloadButton.setMargin(WbToolbarButton.MARGIN);

			selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
			selectorPanel.add(schemaLabel);
			selectorPanel.add(schemaSelector);
			selectorPanel.add(catalogLabel);
			selectorPanel.add(catalogSelector);
			selectorPanel.add(reloadButton);

			this.add(selectorPanel, BorderLayout.NORTH);
			this.add(tabPane, BorderLayout.CENTER);

			this.toolbar = new WbToolbar();
			this.toolbar.addDefaultBorder();
      this.toolbar.setLayout(new BorderLayout());
			this.connectionInfo = new ConnectionInfo(this.toolbar.getBackground());
			this.toolbar.add(this.connectionInfo, BorderLayout.CENTER);

      // this dummy button is used to calculate the height of the regular toolbar
      // to avoid the UI from "jumping" when switchting between a SQL tab and the DbExplorer
      WbToolbarButton button = new WbToolbarButton(IconMgr.getInstance().getToolbarIcon("save"));
      Dimension d = button.getPreferredSize();

      Dimension cd = connectionInfo.getPreferredSize();
      cd.height = d.height;
      connectionInfo.setMinimumSize(cd);
      connectionInfo.setPreferredSize(cd);

      reloadButton.setPreferredSize(d);
      reloadButton.setMaximumSize(d);

			if (mainWindow != null) mainWindow.addExecutionListener(this);

			KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			focusManager.addPropertyChangeListener("focusOwner", this);
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not initialize DbExplorerPanel", e);
		}
	}

  @Override
  public void reload()
  {
    if (!WbSwingUtilities.isConnectionIdle(DbExplorerPanel.this, dbConnection)) return;

    if (schemaSelector.isVisible())
    {
      startRetrieveSchemas(false);
    }
    else if (catalogSelector.isVisible())
    {
      readCatalogs();
    }
  }

  @Override
  public void registerObjectFinder(ObjectFinder finder)
  {
  }

  @Override
  public boolean isModifiedAfter(long time)
  {
    return false;
  }

  @Override
  public boolean supportsBookmarks()
  {
    return false;
  }

  @Override
  public List<NamedScriptLocation> getBookmarks()
  {
    return null;
  }

  @Override
  public void jumpToBookmark(NamedScriptLocation bookmark)
  {
  }

  @Override
  public void setLocked(boolean flag)
  {
    this.locked = flag;
    updateTabTitle();
  }

  @Override
  public boolean isLocked()
  {
    return locked;
  }

	@Override
	public boolean isCancelling()
	{
		return false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (!this.isVisible()) return;
		String prop = evt.getPropertyName();

		Object o = evt.getNewValue();
		if (o instanceof JComponent && "focusOwner".equals(prop))
		{
			currentFocus = (JComponent)evt.getNewValue();
		}
	}

	@Override
	public void setConnectionClient(Connectable client)
	{
		// not used
	}

	public final void setDbExecutionListener(DbExecutionListener l)
	{
		if (this.tables != null)
		{
			tables.setDbExecutionListener(l);
		}
		if (this.searchPanel != null)
		{
			this.searchPanel.addDbExecutionListener(l);
		}
	}

	public void setSwitchCatalog(boolean flag)
	{
		this.switchCatalog = flag && canSwitchCatalog();
	}

	private boolean canSwitchCatalog()
	{
		if (this.dbConnection == null)
		{
			return DbExplorerSettings.getSwitchCatalogInExplorer();
		}
		return dbConnection.getDbSettings().getSwitchCatalogInExplorer();
	}

	public void showConnectButton(ConnectionSelector selector)
	{
		this.connectionSelector = selector;
		this.selectConnectionButton = new JButton(ResourceMgr.getString("LblSelectConnection"));
		Border b = new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(1, 10, 1, 10));
		this.selectConnectionButton.setBorder(b);
		this.selectConnectionButton.addActionListener(this);
		this.selectorPanel.add(Box.createHorizontalStrut(15));
		this.selectorPanel.add(this.selectConnectionButton);
	}

	private final Object busyLock = new Object();

	private void setBusy(boolean flag)
	{
		synchronized (busyLock)
		{
			busy = flag;
			schemaSelector.setEnabled(!flag);
			catalogSelector.setEnabled(!flag);
			reloadButton.setEnabled(!flag);
		}
	}

	@Override
	public boolean isBusy()
	{
		synchronized (busyLock)
		{
			return this.busy;
		}
	}

	@Override
	public String getId()
	{
		return "WbExp-" + NumberStringCache.getNumberString(this.internalId);
	}

  private void startRetrieveSchemas(boolean checkWorkspace)
  {
    if (this.isBusy() || isConnectionBusy() || this.dbConnection == null || this.dbConnection.getMetadata() == null)
    {
      this.schemaRetrievePending = true;
      return;
    }

    if (this.connectionInitPending)
    {
      this.initConnection();
    }

    WbThread schemaRetrieval = new WbThread("DbExplorer schema retrieval")
    {
      @Override
      public void run()
      {
        retrieveAndShowSchemas(checkWorkspace);
      }
    };
    schemaRetrieval.start();
  }

	private void retrieveAndShowSchemas(final boolean checkWorkspace)
  {
    final List<String> schemas = this.dbConnection.getMetadata().getSchemas(dbConnection.getSchemaFilter());

    WbSwingUtilities.invoke(() ->
    {
      _showSchemas(schemas, checkWorkspace);
    });
  }

	private void _showSchemas(List<String> schemas, boolean checkWorkspace)
	{
		String schemaToSelect = null;
		try
		{
			this.schemaSelector.removeActionListener(this);

			setBusy(true);

			String currentSchema = null;
			boolean workspaceSchema = false;
			if (checkWorkspace && this.dbConnection.getProfile().getStoreExplorerSchema())
			{
				currentSchema = this.schemaFromWorkspace;
				workspaceSchema = true;
			}

			if (currentSchema == null) currentSchema = this.dbConnection.getCurrentSchema();
			if (currentSchema == null) currentSchema = this.dbConnection.getCurrentUser();

			if (schemas.size() > 0)
			{
				Object selected = schemaSelector.getSelectedItem();
				if (!workspaceSchema && selected != null)
				{
					currentSchema = selected.toString();
				}
				this.schemaSelector.setEnabled(true);
				this.schemaSelector.setVisible(true);
				this.schemaLabel.setVisible(true);

				this.schemaSelector.removeAllItems();
				this.schemaSelector.addItem("*");
				if ("*".equals(currentSchema))
				{
					schemaToSelect = "*";
				}

				for (String schema : schemas)
				{
					this.schemaSelector.addItem(schema.trim());
					if (schema.equalsIgnoreCase(currentSchema)) schemaToSelect = schema;
				}

				if (workspaceSchema && schemaToSelect == null)
				{
					// when using the workspace for multiple connections
					// it can happen that the stored schema does not exist
					// for the current connection, in this case we revert
					// to "current" schema
					schemaToSelect = this.dbConnection.getMetadata().getCurrentSchema();
				}

				if (schemaToSelect != null)
				{
					schemaSelector.setSelectedItem(schemaToSelect);
				}
				else
				{
					schemaSelector.setSelectedIndex(0);
				}
				currentSchema = (String)schemaSelector.getSelectedItem();
			}
			else
			{
				this.schemaSelector.setEnabled(false);
				this.schemaSelector.setVisible(false);
				this.schemaLabel.setVisible(false);
				currentSchema = null;
			}

			try
			{
				if (this.dbConnection.getDbSettings().supportsCatalogs())
				{
					readCatalogs();
				}
			}
			catch (Exception ex)
			{
				LogMgr.logError("DbExplorerPanel.readSchemas()", "Could not read catalogs", ex);
			}

			String catalog = getSelectedCatalog();
			tables.setCatalogAndSchema(catalog, currentSchema, false);
			procs.setCatalogAndSchema(catalog, currentSchema, false);
		}
		catch (Throwable e)
		{
			LogMgr.logError("DbExplorer.readSchemas()", "Could not retrieve list of schemas", e);
		}
		finally
		{
			this.schemaRetrievePending = false;
			setBusy(false);
		}
		if (schemaSelector.isVisible() || catalogSelector.isVisible())
		{
			this.reloadButton.setVisible(true);
			this.reloadSchemasAction.setEnabled(true);
		}
		else
		{
			this.reloadButton.setVisible(false);
			this.reloadSchemasAction.setEnabled(false);
		}

		this.schemaSelector.addActionListener(this);
	}

	@Override
	public boolean isConnected()
	{
		return (this.dbConnection != null);
	}

	private void doConnect(ConnectionProfile profile)
	{
		ConnectionMgr mgr = ConnectionMgr.getInstance();
		WbConnection conn = null;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			conn = mgr.getConnection(profile, this.getId());
			setConnection(conn);
		}
		catch (Exception e)
		{
			LogMgr.logError("MainWindow.showDbExplorer()", "Error getting new connection for DbExplorer tab.", e);
			String error = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.showErrorMessage(this, error);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	public void connect(final ConnectionProfile profile)
	{
		// connecting can be pretty time consuming on a slow system
		// so move it into its own thread...
		if (!this.isConnected())
		{
			Thread t = new WbThread("DbExplorer connection")
			{
				@Override
				public void run()
				{
					doConnect(profile);
				}
			};
			t.start();
		}
	}

	private boolean isConnectionBusy()
	{
		if (this.dbConnection == null) return false;
		if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab()) return this.isBusy();
		return dbConnection.isBusy();
	}

	private void initConnection()
	{
		if (this.dbConnection == null) return;
		try
		{
			this.tables.setConnection(this.dbConnection);
			this.procs.setConnection(dbConnection);
			if (this.triggers != null) this.triggers.setConnection(dbConnection);
			if (this.searchPanel != null) this.searchPanel.setConnection(dbConnection);
			readSchemaLabel();
			this.connectionInitPending = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("DbExplorerPanel.initConnection()", "Error during init",e);
		}
	}

	private void readSchemaLabel()
	{
		String schemaTerm = dbConnection.getMetadata().getSchemaTerm();
		schemaLabel.setText(StringUtil.capitalize(schemaTerm));
    reloadSchemasAction.setTooltip(ResourceMgr.getFormattedString("TxtReloadSchemas", schemaTerm));
	}

	@Override
	public void setConnection(WbConnection aConnection)
	{
		if (this.isBusy()) return;

		this.dbConnection = aConnection;
		setSwitchCatalog(false);

		reloadSchemasAction.setEnabled((dbConnection != null));

		if (aConnection == null)
		{
			this.reset();
			this.connectionInitPending = false;
			return;
		}

		try
		{
			if (this.connectionSelector != null)
			{
				// always switch database/catalog if in stand-alone mode
				setSwitchCatalog(true);
			}
			else if (aConnection.getProfile() != null)
			{
				boolean separateConnection = aConnection.getProfile().getUseSeparateConnectionPerTab();
				setSwitchCatalog(separateConnection);
				if (separateConnection)
				{
          JdbcUtils.initDbExplorerConnection(dbConnection);
				}
			}

			this.connectionInitPending = true;
			this.schemaRetrievePending = true;
			this.retrievePending = DbExplorerSettings.getRetrieveDbExplorer();

			if (this.window != null)
			{
				this.window.setProfile(aConnection.getProfile());
			}

			this.connectionInfo.setConnection(aConnection);

			// Avoid concurrent execution on the same connection object
			if (!isConnectionBusy())
			{
        initConnection();
        if (isVisible() && retrievePending)
        {
          // if we are visible start the retrieve immediately in a background thread
          retrieve();
        }
			}
		}
		catch (Throwable th)
		{
			this.retrievePending = true;
			this.schemaRetrievePending = true;
			LogMgr.logError("DbExplorerPanel.setConnection()", "Error during connection init", th);
		}
	}

	private void readCatalogs()
	{
		List<String> catalogs = this.dbConnection.getMetadata().getCatalogInformation(dbConnection.getCatalogFilter());
		this.catalogSelector.removeActionListener(this);
		if (catalogs.isEmpty())
		{
			this.catalogSelector.setVisible(false);
			this.catalogSelector.setEnabled(false);
			this.catalogLabel.setVisible(false);
			this.catalogFromWorkspace = null;
		}
		else
		{
			String catalogTerm = StringUtil.capitalize(this.dbConnection.getMetadata().getCatalogTerm());
			String catalogToSelect = null;

			if (this.dbConnection.getProfile().getStoreExplorerSchema() && this.catalogFromWorkspace != null)
			{
				catalogToSelect = catalogFromWorkspace;
				catalogFromWorkspace = null;
			}
			else if (catalogSelector.getItemCount() > 0)
			{
				// if there are entries in the dropdown, make sure the currently selected
				// catalog is restored when re-loading the databases.
				Object o = catalogSelector.getSelectedItem();
				catalogToSelect = o == null ? null : o.toString();
			}
			else
			{
				catalogToSelect = this.dbConnection.getMetadata().getCurrentCatalog();
			}

			this.catalogSelector.removeAllItems();
			this.catalogLabel.setText(catalogTerm);

			int index = 0;
			int indexToSelect = 0;
			for (String db : catalogs)
			{
				// only select the catalog if it's actually present in the newly retrieved list
				if (db.equalsIgnoreCase(catalogToSelect)) indexToSelect = index;
				catalogSelector.addItem(db);
				index ++;
			}

			this.catalogSelector.setSelectedIndex(indexToSelect);

			this.catalogSelector.addActionListener(this);
			this.catalogSelector.setVisible(true);
			this.catalogSelector.setEnabled(true);
			this.catalogLabel.setVisible(true);
		}
		this.selectorPanel.validate();
	}

	@Override
	public void setVisible(boolean flag)
	{
		boolean wasVisible = this.isVisible();
		super.setVisible(flag);
		if (!wasVisible && flag)
		{
      if (retrievePending)
      {
        // retrievePending will be true, if the connection has
        // been set already, the DbExplorer should be retrieved automatically
        // and the panel was not visible when the connection was provided
        // retrieve() will automatically retrieve the schemas if necessary
        EventQueue.invokeLater(this::retrieve);
      }
      else if (schemaRetrievePending)
			{
        startRetrieveSchemas(true);
			}

			if (currentFocus != null)
			{
				WbSwingUtilities.requestFocus(currentFocus);
			}
		}
	}

	@Override
	public void panelSelected()
	{
		Component panel = this.tabPane.getSelectedComponent();
		if (panel == null) return;
		if (panel == this.tables)
		{
			this.tables.panelSelected();
		}
		else if (panel == this.procs)
		{
			this.procs.panelSelected();
		}
		else if (panel == this.triggers)
		{
			this.triggers.panelSelected();
		}
	}

	@Override
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	@Override
	public void reset()
	{
		this.setLocked(false);
		if (tables != null) this.tables.reset();
		if (procs != null) this.procs.reset();
		if (searchPanel != null) this.searchPanel.reset();
		if (tabPane != null && this.tabPane.getTabCount() > 0) this.tabPane.setSelectedIndex(0);
	}

	@Override
	public void disconnect()
	{
		if (tables != null) this.tables.disconnect();
		if (procs != null) this.procs.disconnect();
		if (this.triggers != null) this.triggers.disconnect();
		this.searchPanel.disconnect();
		this.dbConnection = null;
		this.reset();
	}

	public void saveSettings()
	{
		if (tables != null) this.tables.saveSettings();
		if (procs != null) this.procs.saveSettings();
		if (triggers != null) this.triggers.saveSettings();
		if (searchPanel != null) this.searchPanel.saveSettings();
	}

	public void restoreSettings()
	{
		if (tables != null) tables.restoreSettings();
		if (procs != null) procs.restoreSettings();
		if (triggers != null) triggers.restoreSettings();
		if (searchPanel != null) searchPanel.restoreSettings();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.schemaSelector)
		{
			retrieve();
		}
		else if (e.getSource() == this.selectConnectionButton)
		{
			this.connectionSelector.selectConnection();
		}
		else if (e.getSource() == this.catalogSelector)
		{
      String catalog = getSelectedCatalog();
			if (switchCatalog && !"*".equals(catalog))
			{
				try
				{
					CatalogChanger changer = new CatalogChanger();
					changer.setCurrentCatalog(dbConnection, catalog);
				}
				catch (SQLException ex)
				{
					LogMgr.logError("DbExplorerPanel.actionPerformed()", "Could not switch catalog", ex);
					WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(ex));
				}
			}
			retrieve();
		}
	}

	protected String getSelectedCatalog()
	{
		if (this.catalogSelector == null) return null;
		return (String)catalogSelector.getSelectedItem();
	}

	protected void retrieve()
	{
		if (this.dbConnection == null) return;

		if (this.isBusy() || isConnectionBusy())
		{
			this.retrievePending = true;
			return;
		}

		if (this.connectionInitPending)
		{
			this.initConnection();
		}

    final Component c = this;

    WbSwingUtilities.showWaitCursorOnWindow(this);

		Thread t = new WbThread("SchemaChange")
		{
			@Override
			public void run()
			{
				try
				{
          setBusy(true);

          if (schemaRetrievePending)
          {
            retrieveAndShowSchemas(true);
          }

          String schema = (String)schemaSelector.getSelectedItem();

					WbSwingUtilities.showWaitCursorOnWindow(c);
					tables.setCatalogAndSchema(getSelectedCatalog(), schema, true);
					procs.setCatalogAndSchema(getSelectedCatalog(), schema, true);
					if (triggers != null) triggers.setCatalogAndSchema(getSelectedCatalog(), schema, true);
				}
				catch (Exception ex)
				{
					LogMgr.logError(DbExplorerPanel.this, "Could not set schema", ex);
				}
				finally
				{
					retrievePending = false;
					setBusy(false);
					WbSwingUtilities.showDefaultCursorOnWindow(c);
				}
			}
		};
		t.start();
	}

	@Override
	public void setTabName(String name)
	{
		this.tabTitle = name;
	}

	public String getRealTabTitle()
	{
		if (getParent() instanceof JTabbedPane)
		{
			JTabbedPane p = (JTabbedPane)getParent();
			int index = p.indexOfComponent(this);
			if (index > -1)
			{
				String t = p.getTitleAt(index);
				return t;
			}
		}
		return getTabTitle();
	}

	protected void updateTabTitle()
	{
		PanelTitleSetter.updateTitle(this);
	}

	@Override
	public String getTabTitle()
	{
		return (tabTitle == null ? ResourceMgr.getString("LblDbExplorer") : tabTitle);
	}

	@Override
	public void setTabTitle(JTabbedPane tab, int index)
	{
		String plainTitle = getTabTitle();
		PanelTitleSetter.setTabTitle(tab, this, index, plainTitle);
	}

	public DbExplorerWindow openWindow(ConnectionProfile profile)
	{
		if (this.window == null)
		{
			window = new DbExplorerWindow(this, profile);
		}
		window.setVisible(true);
		return window;
	}

	@Override
	public List getMenuItems()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	public WbToolbar getToolbar(List<WbAction> globalActions, boolean createNew)
	{
		return this.toolbar;
	}

	@Override
	public void showLogMessage(String aMsg)
	{
	}

	@Override
	public void clearLog()
	{
	}

	@Override
	public void appendToLog(String msg)
	{
	}

	@Override
	public void showLogPanel()
	{
	}

	@Override
	public void showResultPanel()
	{
	}

	void explorerWindowClosed()
	{
		if (this.dbConnection != null)
		{
			if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab() || DbExplorerSettings.getAlwaysUseSeparateConnForDbExpWindow())
			{
				try { this.dbConnection.disconnect(); } catch (Throwable th) {}
			}
		}
		this.dispose();
		this.disconnect();

		if (mainWindow != null)
		{
			mainWindow.explorerWindowClosed(this.window);
		}
		window = null;
	}

	@Override
	public void dispose()
	{
		this.reset();
		if (tables != null)
		{
			this.tables.dispose();
			tables = null;
		}
		if (procs != null)
		{
			this.procs.dispose();
			this.procs = null;
		}
		if (this.triggers != null)
		{
			this.triggers.dispose();
			this.triggers = null;
		}

		if (mainWindow != null)
		{
			mainWindow.removeExecutionListener(this);
		}
	}

	@Override
	public void saveToWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		Object s = this.schemaSelector.getSelectedItem();
		WbProperties p = w.getSettings();
		String key = "dbexplorer" + index + ".currentschema";
		if (s != null)
		{
			p.setProperty(key, s.toString());
		}
		else if (this.schemaFromWorkspace != null)
		{
			// if the DbExplorer was never "really" displayed we have to
			// save the schema that we retrieved initially from the workspace
			p.setProperty(key, this.schemaFromWorkspace);
		}

		key = "dbexplorer" + index + ".currentcatalog";
		s = this.getSelectedCatalog();
		if (s != null)
		{
			p.setProperty(key, s.toString());
		}
		else if (this.catalogFromWorkspace != null)
		{
			p.setProperty(key, this.catalogFromWorkspace);
		}
		p.setProperty("dbexplorer" + index + ".locked", this.locked);
		p.setProperty("tab" + index + ".type", PanelType.dbExplorer.toString());
		p.setProperty("tab" + index + ".title", getTabTitle());
		tables.saveToWorkspace(w, index);
		searchPanel.saveToWorkspace(w, index);
		procs.saveToWorkspace(w, index);
		if (triggers != null) triggers.saveToWorkspace(w, index);
	}


	@Override
	public boolean isModified()
	{
    if (tables == null) return false;
		return tables.isModified();
	}

	@Override
	public boolean canClosePanel(boolean checkTransactions)
	{
		if (!GuiSettings.getConfirmDiscardResultSetChanges()) return true;
		if (!isModified()) return true;

		boolean canClose = WbSwingUtilities.getProceedCancel(this, "MsgDiscardTabChanges", HtmlUtil.cleanHTML(getRealTabTitle()));
		return canClose;
	}

	@Override
	public void readFromWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		this.schemaFromWorkspace = null;
		this.catalogFromWorkspace = null;
		this.reset();
		try
		{
			WbProperties p = w.getSettings();
			this.schemaFromWorkspace = p.getProperty("dbexplorer" + index + ".currentschema", null);
			this.catalogFromWorkspace = p.getProperty("dbexplorer" + index + ".currentcatalog", null);
			if (tables != null) tables.readFromWorkspace(w, index);
			if (searchPanel != null) searchPanel.readFromWorkspace(w, index);
			if (procs != null) procs.readFromWorkspace(w, index);
			if (triggers != null) triggers.readFromWorkspace(w, index);
			setTabName(p.getProperty("tab" + index + ".title"));
			this.locked = p.getBoolProperty("dbexplorer" + index + ".locked", false);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbExplorerPanel.readFromWorkspace()", "Error loading workspace", e);
		}

		EventQueue.invokeLater(this::updateTabTitle);
	}

	@Override
	public void executionStart(WbConnection conn, Object source)
	{
	}

	/*
	 *	Fired by the SqlPanel if DB access finished
	 */
	@Override
	public void executionEnd(WbConnection conn, Object source)
	{
		if (!this.isVisible()) return;

    if (this.retrievePending)
		{
			retrieve();
		}
    else if (this.schemaRetrievePending)
    {
      startRetrieveSchemas(true);
    }

	}

	@Override
	public String toString()
	{
		return getId();
	}

}
