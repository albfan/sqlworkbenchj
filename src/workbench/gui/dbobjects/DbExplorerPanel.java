/*
 * DbExplorerPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.db.CatalogChanger;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.db.WbConnection;
import workbench.gui.components.FlatButton;
import workbench.interfaces.DbExecutionListener;
import workbench.util.ExceptionUtil;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.sql.PanelTitleSetter;
import workbench.gui.sql.PanelType;
import workbench.interfaces.Connectable;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
	implements ActionListener, MainPanel, ChangeListener, DbExecutionListener, PropertyChangeListener
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
	boolean connected;
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
	private Reloadable schemaReloader;
	private FlatButton reloadButton;
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

			int index = 1;

			tabPane.add(ResourceMgr.getString("TxtDbExplorerProcs"), procs);
			tabPane.setToolTipTextAt(index, ResourceMgr.getDescription("TxtDbExplorerProcs"));

			if (Settings.getInstance().getShowTriggerPanel())
			{
				triggers = new TriggerListPanel(aParent);
				tabPane.add(ResourceMgr.getString("TxtDbExplorerTriggers"), triggers);
				tabPane.setToolTipTextAt(index ++, ResourceMgr.getDescription("TxtDbExplorerTriggers"));
			}
			tabPane.add(ResourceMgr.getString("TxtSearchTables"), this.searchPanel);
			tabPane.setToolTipTextAt(index ++, ResourceMgr.getDescription("TxtSearchTables"));
			tabPane.setFocusable(false);

			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
			this.setLayout(new BorderLayout());

			this.selectorPanel = new JPanel();
			schemaReloader = new Reloadable()
			{
				@Override
				public void reload()
				{
					if (!WbSwingUtilities.checkConnection(DbExplorerPanel.this, dbConnection)) return;

					if (schemaSelector.isVisible())
					{
						readSchemas(false);
					}
					else if (catalogSelector.isVisible())
					{
						readCatalogs();
					}
				}
			};
			reloadSchemasAction =new ReloadAction(schemaReloader);
			reloadSchemasAction.setEnabled(false);
			reloadSchemasAction.setTooltip(ResourceMgr.getString("TxtReload"));

			schemaLabel = new JLabel(ResourceMgr.getString("LblSchema"));

			this.schemaSelector = new JComboBox();
			Dimension d = new Dimension(80, 20);
			Dimension max = new Dimension(150,40);
			this.schemaSelector.setMinimumSize(d);
			this.schemaSelector.setMaximumSize(max);

			this.catalogSelector  = new JComboBox();
			this.catalogLabel = new JLabel("Catalog");
			this.catalogSelector.setVisible(false);
			this.catalogSelector.setEnabled(false);
			this.catalogSelector.setMaximumSize(max);
			this.catalogLabel.setVisible(false);
			reloadButton = new FlatButton(reloadSchemasAction);
			reloadButton.setText(null);
			reloadButton.setMargin(WbToolbarButton.MARGIN);

			this.selectorPanel.setLayout(new GridBagLayout());
			GridBagConstraints gc = new GridBagConstraints();
			gc.gridx = 0;
			gc.gridy = 0;
			gc.anchor = GridBagConstraints.WEST;
			gc.insets = new Insets(0, 5, 0, 0);
			gc.fill = GridBagConstraints.NONE;
			this.selectorPanel.add(schemaLabel, gc);

			gc.gridx ++;
			gc.insets = new Insets(0, 8, 0, 0);
			this.selectorPanel.add(schemaSelector, gc);

			gc.gridx ++;
			this.selectorPanel.add(catalogLabel, gc);

			gc.gridx ++;
			this.selectorPanel.add(catalogSelector, gc);

			gc.gridx ++;
			gc.weightx = 1.0;
			this.selectorPanel.add(reloadButton, gc);

			this.add(this.selectorPanel, BorderLayout.NORTH);
			this.add(tabPane, BorderLayout.CENTER);

			this.toolbar = new WbToolbar();
			this.toolbar.addDefaultBorder();
			d = new Dimension(100, 30);
			this.toolbar.setMinimumSize(d);
			this.toolbar.setPreferredSize(d);
			this.connectionInfo = new ConnectionInfo(this.toolbar.getBackground());
			this.connectionInfo.setMinimumSize(d);
			this.toolbar.add(this.connectionInfo);
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
		this.switchCatalog = flag && Settings.getInstance().getSwitchCatalogInExplorer();
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

	private void readSchemas(boolean checkWorkspace)
	{
		if (this.isBusy() || isConnectionBusy() || this.dbConnection == null || this.dbConnection.getMetadata() == null)
		{
			this.schemaRetrievePending = true;
			return;
		}

		String schemaToSelect = null;
		try
		{
			this.schemaSelector.removeActionListener(this);

			setBusy(true);

			List<String> schemas = this.dbConnection.getMetadata().getSchemas(dbConnection.getSchemaFilter());
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
			readCatalogs();

			tables.setCatalogAndSchema(getSelectedCatalog(), currentSchema, false);
			procs.setCatalogAndSchema(getSelectedCatalog(), currentSchema, false);
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
		String schemaName = this.dbConnection.getMetadata().getSchemaTerm();
		this.schemaLabel.setText(StringUtil.capitalize(schemaName));
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

		WbSwingUtilities.showWaitCursorOnWindow(this);
		reloadSchemasAction.setTooltip(ResourceMgr.getFormattedString("TxtReloadSchemas", dbConnection.getMetadata().getSchemaTerm()));

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
				// when dealing with tables that have LONG or LONG RAW columns
				// and DBMS_OUTPUT was enabled, then retrieval of those columns
				// does not work. If we have separate connections for each tab
				// we can safely disable the DBMS_OUTPUT on this connection
				// as there won't be a way to view the output anyway
				if (separateConnection) aConnection.getMetadata().disableOutput();
			}

			this.connectionInitPending = true;
			this.schemaRetrievePending = true;
			this.retrievePending = Settings.getInstance().getRetrieveDbExplorer();

			if (this.window != null)
			{
				String name = null;
				ConnectionProfile prof = aConnection.getProfile();
				if (prof != null) name = prof.getName();
				if (name != null) this.window.setProfileName(name);
			}

			this.connectionInfo.setConnection(aConnection);

			// Try to avoid concurrent execution on the
			// same connection object
			if (!isConnectionBusy())
			{
				WbThread t = new WbThread("DbExplorerInit")
				{
					@Override
					public void run()
					{
						initConnection();

						if (isVisible())
						{
							readSchemas(true);
							if (retrievePending)
							{
								// if we are visible start the retrieve immediately
								retrieve();
							}
						}
					}
				};
				t.start();
			}
		}
		catch (Throwable th)
		{
			this.retrievePending = true;
			this.schemaRetrievePending = true;
			LogMgr.logError("DbExplorerPanel.setConnection()", "Error during connection init", th);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursorOnWindow(this);
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
			boolean selectLastCatalog = false;

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
			if (schemaRetrievePending)
			{
				this.readSchemas(true);
			}
			if (retrievePending)
			{
				// retrievePending will be true, if the connection has
				// been set already, the DbExplorer should be retrieved automatically
				// and the panel was not visible when the connection was provided
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						retrieve();
					}
				});
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
		if (this.tables != null) this.tables.reset();
		if (this.procs != null) this.procs.reset();
		if (this.searchPanel != null) this.searchPanel.reset();
		if (this.tabPane != null && this.tabPane.getTabCount() > 0) this.tabPane.setSelectedIndex(0);
	}

	@Override
	public void disconnect()
	{
		this.tables.disconnect();
		this.procs.disconnect();
		if (this.triggers != null) this.triggers.disconnect();
		this.searchPanel.disconnect();
		this.dbConnection = null;
		this.reset();
	}

	public void saveSettings()
	{
		this.tables.saveSettings();
		this.procs.saveSettings();
		if (this.triggers != null) this.triggers.saveSettings();
		if (this.searchPanel != null) this.searchPanel.saveSettings();
	}

	public void restoreSettings()
	{
		if (tables != null) tables.restoreSettings();
		if (procs != null) procs.restoreSettings();
		if (this.triggers != null) triggers.restoreSettings();
		if (this.searchPanel != null) searchPanel.restoreSettings();
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
			if (switchCatalog)
			{
				try
				{
					CatalogChanger changer = new CatalogChanger();
					changer.setCurrentCatalog(dbConnection, getSelectedCatalog());
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

		if (this.schemaRetrievePending)
		{
			this.readSchemas(true);
		}

		final String schema = (String)schemaSelector.getSelectedItem();
		final Component c = this;

		Thread t = new WbThread("SchemaChange")
		{
			@Override
			public void run()
			{
				try
				{
					setBusy(true);
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

	public DbExplorerWindow openWindow(String aProfileName)
	{
		if (this.window == null)
		{
			window = new DbExplorerWindow(this, aProfileName);
		}
		window.setVisible(true);
		return window;
	}

	@Override
	public List getActions()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	public WbToolbar getToolbar()
	{
		return this.toolbar;
	}

	@Override
	public void showLogMessage(String aMsg)
	{
	}

	@Override
	public void clearStatusMessage()
	{
	}

	@Override
	public void showStatusMessage(String aMsg)
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

	@Override
	public void addToToolbar(WbAction anAction, boolean aFlag)
	{
	}

	void explorerWindowClosed()
	{
		if (this.dbConnection != null)
		{
			if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab())
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
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == this.tabPane)
		{
			if (this.tabPane.getSelectedIndex() == 1)
			{
				this.procs.retrieveIfNeeded();
			}
		}
	}

	@Override
	public void dispose()
	{
		this.reset();
		this.tables.dispose();
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
		return tables.isModified();
	}

	@Override
	public boolean canClosePanel()
	{
		if (!GuiSettings.getConfirmDiscardResultSetChanges()) return true;
		if (!isModified()) return true;

		boolean canClose = WbSwingUtilities.getProceedCancel(this, "MsgDiscardTabChanges", getRealTabTitle());
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
			tables.readFromWorkspace(w, index);
			searchPanel.readFromWorkspace(w, index);
			procs.readFromWorkspace(w, index);
			if (triggers != null) triggers.readFromWorkspace(w, index);
			setTabName(p.getProperty("tab" + index + ".title"));
			this.locked = p.getBoolProperty("dbexplorer" + index + ".locked", false);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbExplorerPanel.readFromWorkspace()", "Error loading workspace", e);
		}
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				updateTabTitle();
			}
		});
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
		if (this.connectionInitPending)
		{
			this.initConnection();
		}
		if (this.isVisible() && this.schemaRetrievePending)
		{
			this.readSchemas(true);
		}
		if (this.isVisible() && this.retrievePending)
		{
			retrieve();
		}
	}

	@Override
	public String toString()
	{
		return getId();
	}

}
