/*
 * TableListPanel.java
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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.TriggerReader;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CreateDropScriptAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.actions.ToggleTableSourceAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.ShareableDisplay;
import workbench.interfaces.Exporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.util.ExceptionUtil;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.SequenceReader;
import workbench.db.SynonymDDLHandler;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.TriggerReaderFactory;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.CreateDummySqlAction;
import workbench.gui.actions.DeleteTablesAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.AlterObjectAction;
import workbench.gui.actions.SchemaReportAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.settings.PlacementChooser;
import workbench.gui.sql.PanelContentSender;
import workbench.interfaces.CriteriaPanel;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.ListSelectionControl;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.resource.GuiSettings;
import workbench.util.LowMemoryException;
import workbench.util.WbWorkspace;
import workbench.util.WbProperties;


/**
 * A panel that displays a list of tables, views and other database objects.
 * Essentially everything returned by DbMetadata.getObjects()
 *
 * @author Thomas Kellerer
 * @see workbench.db.DbMetadata#getObjects(java.lang.String, java.lang.String, java.lang.String[])
 */
public class TableListPanel
	extends JPanel
	implements ActionListener, ChangeListener, ListSelectionListener, MouseListener,
						 ShareableDisplay, Exporter, PropertyChangeListener,
						 TableModelListener, DbObjectList, ListSelectionControl, TableLister
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	protected WbConnection dbConnection;
	protected JPanel listPanel;
	protected CriteriaPanel findPanel;
	protected DbObjectTable tableList;
	protected TableDefinitionPanel tableDefinition;
	protected WbTable indexes;
	protected FkDisplayPanel importedKeys;
	protected FkDisplayPanel exportedKeys;
	protected ReloadAction reloadAction;

	protected TableDataPanel tableData;

	private JPanel indexPanel;
	private TriggerDisplayPanel triggers;
	protected DbObjectSourcePanel tableSource;
	private JTabbedPane displayTab;
	private WbSplitPane splitPane;

	private JComboBox tableTypes = new JComboBox();
	private String currentSchema;
	private String currentCatalog;
	private SpoolDataAction spoolData;

	private CompileDbObjectAction compileAction;
	private AlterObjectAction renameAction;

	private MainWindow parentWindow;

	private TableIdentifier selectedTable;

	// For synonym resolution
	private TableIdentifier realTable;

	private boolean shiftDown;
	protected boolean shouldRetrieve;

	protected boolean shouldRetrieveTable;
	protected boolean shouldRetrieveTableSource;
	protected boolean shouldRetrieveTriggers;
	protected boolean shouldRetrieveIndexes;
	protected boolean shouldRetrieveExportedKeys;
	protected boolean shouldRetrieveImportedKeys;
	protected boolean shouldRetrieveTableData;

	protected boolean busy;
	protected boolean ignoreStateChanged;

	private EditorTabSelectMenu showDataMenu;

	private ToggleTableSourceAction toggleTableSource;

	// holds a reference to other WbTables which
	// need to display the same table list
	// e.g. the table search panel
	private List<JTable> tableListClients;

	protected JDialog infoWindow;
	private JLabel infoLabel;
	private JLabel summaryStatusBarLabel;
	private String tableTypeToSelect;

	private final Object connectionLock = new Object();
	private final Object msgLock = new Object();

	private TableChangeValidator validator = new TableChangeValidator();
	// </editor-fold>

	public TableListPanel(MainWindow aParent)
		throws Exception
	{
		super();
		this.parentWindow = aParent;
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		int location = PlacementChooser.getPlacementLocation();
		displayTab = new WbTabbedPane(location);
		displayTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		displayTab.setName("displaytab");

		this.tableDefinition = new TableDefinitionPanel();
		this.tableDefinition.setName("tabledefinition");
		this.tableDefinition.addPropertyChangeListener(TableDefinitionPanel.INDEX_PROP, this);
		this.tableDefinition.addPropertyChangeListener(TableDefinitionPanel.DEFINITION_PROP, this);

		Reloadable indexReload = new Reloadable()
		{
			@Override
			public void reload()
			{
				shouldRetrieveIndexes = true;
				if (dbConnection.isBusy()) return;
				try
				{
					dbConnection.setBusy(true);
					retrieveIndexes();
				}
				catch (SQLException e)
				{
					LogMgr.logError("TableListPanel.indexReloader", "Error retrieving indexes", e);
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};

		this.indexes = new WbTable();
		this.indexes.setName("indexlist");
		this.indexes.setAdjustToColumnLabel(false);
		this.indexes.setSelectOnRightButtonClick(true);
		this.indexPanel = new TableIndexPanel(this.indexes, indexReload);

		Reloadable sourceReload = new Reloadable()
		{
			@Override
			public void reload()
			{
				shouldRetrieveTable = true;
				shouldRetrieveIndexes = true;
				if (dbConnection.isBusy()) return;

				try
				{
					dbConnection.setBusy(true);
					retrieveTableSource();
				}
				finally
				{
					dbConnection.setBusy(false);
				}
			}
		};

		this.tableSource = new DbObjectSourcePanel(aParent, sourceReload);

		this.tableData = new TableDataPanel();

		this.importedKeys = new FkDisplayPanel(this, true);
		this.exportedKeys = new FkDisplayPanel(this, false);

		this.triggers = new TriggerDisplayPanel();

		this.listPanel = new JPanel();
		this.tableList = new DbObjectTable();

		this.tableList.setName("dbtablelist");
		this.tableList.setSelectOnRightButtonClick(true);
		this.tableList.getSelectionModel().addListSelectionListener(this);
		this.tableList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.tableList.setAdjustToColumnLabel(true);
		this.tableList.addTableModelListener(this);

		this.spoolData = new SpoolDataAction(this);
		this.tableList.addPopupAction(spoolData, true);

		renameAction = new AlterObjectAction(tableList);
		renameAction.setReloader(this);

		this.extendPopupMenu();

		this.findPanel = new QuickFilterPanel(this.tableList, false, "tablelist");
		((QuickFilterPanel)findPanel).setFilterOnType(Settings.getInstance().getDbExpFilterDuringTyping());

		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_DBEXP_INSTANT_FILTER);

		reloadAction = new ReloadAction(this);
		reloadAction.getToolbarButton().setToolTipText(ResourceMgr.getString("TxtRefreshTableList"));
		reloadAction.addToInputMap(tableList);

		this.findPanel.addToToolbar(reloadAction, true, false);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.gridx = 0;
		constr.fill = GridBagConstraints.HORIZONTAL;

		Dimension d = new Dimension(150, 50);
		tableTypes.setMaximumSize(d);
		topPanel.add(this.tableTypes, constr);

		constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1.0;
		topPanel.add((JPanel)this.findPanel, constr);

		this.listPanel.setLayout(new BorderLayout());
		this.listPanel.add(topPanel, BorderLayout.NORTH);

		this.summaryStatusBarLabel = new SummaryLabel("");
		this.listPanel.add(summaryStatusBarLabel, BorderLayout.SOUTH);

		this.splitPane = new WbSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		WbScrollPane scroll = new WbScrollPane(this.tableList);

		this.listPanel.add(scroll, BorderLayout.CENTER);
		this.listPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.splitPane.setLeftComponent(this.listPanel);
		this.splitPane.setRightComponent(displayTab);
		this.splitPane.setDividerSize(6);
		this.splitPane.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setContinuousLayout(true);

		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent((JPanel)findPanel);
		pol.addComponent((JPanel)findPanel);
		pol.addComponent(tableList);
		pol.addComponent(tableDefinition);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
		this.displayTab.addMouseListener(this);
		this.tableList.addMouseListener(this);

		initIndexDropper(indexReload);

		this.toggleTableSource = new ToggleTableSourceAction(this, "MnuTxtToggleDbExpSplit");
		this.splitPane.setOneTouchTooltip(toggleTableSource.getTooltipTextWithKeys());
		setupActionMap();

		if (Settings.getInstance().showFocusInDbExplorer())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					indexes.showFocusBorder();
					tableList.showFocusBorder();
				}
			});
		}

		tableList.setRememberColumnOrder(Settings.getInstance().getRememberMetaColumnOrder("tablelist"));
		tableList.setListSelectionControl(this);
		tableList.setReadOnly(!GuiSettings.allowAlterInDbExplorer());
		showObjectDefinitionPanels(false);
	}

	private void initIndexDropper(Reloadable indexReload)
	{
		DbObjectList indexList = new DbObjectList()
		{
			@Override
			public void reload()
			{
				reload();
			}

			@Override
			public Component getComponent()
			{
				return TableListPanel.this;
			}

			@Override
			public WbConnection getConnection()
			{
				return dbConnection;
			}

			@Override
			public TableIdentifier getObjectTable()
			{
				return TableListPanel.this.getObjectTable();
			}

			@Override
			public List<DbObject> getSelectedObjects()
			{
				int[] rows = indexes.getSelectedRows();
				if (rows == null) return null;

				ArrayList<DbObject> objects = new ArrayList<DbObject>(rows.length);

				TableIdentifier tbl = getObjectTable();

				for (int i = 0; i < rows.length; i++)
				{
					String name = indexes.getValueAsString(rows[i], IndexReader.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
					IndexDefinition index = new IndexDefinition(tbl, name);
					objects.add(index);
				}
				return objects;
			}
		};

		DropDbObjectAction dropAction = new DropDbObjectAction("MnuTxtDropIndex", indexList, indexes.getSelectionModel(), indexReload);
		this.indexes.addPopupAction(dropAction, true);
	}

	public boolean isModified()
	{
		if (this.tableData == null) return false;
		return tableData.isModified();
	}

	public void dispose()
	{
		reset();
		tableDefinition.removePropertyChangeListener(this);
		tableDefinition.dispose();
		tableData.dispose();
		Settings.getInstance().removePropertyChangeListener(this);
	}

	private void extendPopupMenu()
	{
		if (this.parentWindow != null)
		{
			this.showDataMenu = new EditorTabSelectMenu(this, ResourceMgr.getString("MnuTxtShowTableData"), "LblShowDataInNewTab", "LblShowDataInTab", parentWindow);
			this.showDataMenu.setEnabled(false);
			this.tableList.addPopupMenu(this.showDataMenu, false);
		}

		this.tableList.addPopupAction(CreateDummySqlAction.createDummyInsertAction(this, tableList.getSelectionModel()), true);
		this.tableList.addPopupAction(CreateDummySqlAction.createDummyUpdateAction(this, tableList.getSelectionModel()), false);
		this.tableList.addPopupAction(CreateDummySqlAction.createDummySelectAction(this, tableList.getSelectionModel()), false);

		ScriptDbObjectAction createScript = new ScriptDbObjectAction(this, tableList.getSelectionModel());
		this.tableList.addPopupAction(createScript, false);

		SchemaReportAction action = new SchemaReportAction(this);
		tableList.addPopupMenu(action.getMenuItem(), false);

		compileAction = new CompileDbObjectAction(this, tableList.getSelectionModel());
		tableList.addPopupAction(compileAction, false);

		DropDbObjectAction dropAction = new DropDbObjectAction(this, this.tableList.getSelectionModel(), this);
		tableList.addPopupAction(dropAction, true);

		CreateDropScriptAction dropScript = new CreateDropScriptAction(this, tableList.getSelectionModel());
		this.tableList.addPopupAction(dropScript, false);

		tableList.addPopupAction(new DeleteTablesAction(this, tableList.getSelectionModel(), this.tableData), false);
		tableList.addPopupAction(renameAction, true);
	}

	public void setDbExecutionListener(DbExecutionListener l)
	{
		tableData.addDbExecutionListener(l);
	}

	private void setDirty(boolean flag)
	{
		this.shouldRetrieve = flag;
	}

	private void setupActionMap()
	{
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		this.setActionMap(am);

		this.toggleTableSource.addToInputMap(im, am);
	}

	/**
	 * Displays the tabs necessary for a TABLE like object
	 */
	protected void showTablePanels()
	{
		if (displayTab.getTabCount() > 3) return; // nothing to do

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ignoreStateChanged = true;
					int index = displayTab.getSelectedIndex();
					displayTab.removeAll();
					addBaseObjectPanels();
					addDataPanel();
					displayTab.add(ResourceMgr.getString("TxtDbExplorerIndexes"), indexPanel);
					displayTab.add(ResourceMgr.getString("TxtDbExplorerFkColumns"), importedKeys);
					displayTab.add(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), exportedKeys);
					addTriggerPanel();
					restoreIndex(index);
				}
				finally
				{
					ignoreStateChanged = false;
				}
			}
		});
	}

	private void restoreIndex(int index)
	{
		if (index > 0 && index < displayTab.getTabCount())
		{
			displayTab.setSelectedIndex(index);
		}
		else
		{
			displayTab.setSelectedIndex(0);
		}
	}

	/**
	 * Displays the tabs common to all DB objects
	 * (essentially object definition and source).
	 *
	 * @param includeDataPanel if true, the Data panel will also be displayed
	 */
	private void showObjectDefinitionPanels(final boolean includeDataPanel)
	{
		int count = displayTab.getTabCount();

		if (includeDataPanel && count == 3) return; // nothing to do
		if (!includeDataPanel && count == 2) return; // nothing to do

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int index = displayTab.getSelectedIndex();
					ignoreStateChanged = true;
					displayTab.removeAll();

					addBaseObjectPanels();
					if (includeDataPanel) addDataPanel();
					showTriggerIfSupported();

					exportedKeys.reset();
					indexes.reset();
					triggers.reset();
					importedKeys.reset();

					if (!includeDataPanel) tableData.reset();
					restoreIndex(index);
				}
				finally
				{
					ignoreStateChanged = false;
				}
			}
		});
	}

	private void showTriggerIfSupported()
	{
		TriggerReader reader = TriggerReaderFactory.createReader(dbConnection);

		if (reader == null) return;
		if (!reader.supportsTriggersOnViews()) return;

		TableIdentifier tbl = getObjectTable();
		if (tbl == null) return;
		DbSettings dbs = dbConnection.getDbSettings();
		if (dbs.isViewType(tbl.getType()))
		{
			addTriggerPanel();
		}
	}

	protected void addTriggerPanel()
	{
		displayTab.add(ResourceMgr.getString("TxtDbExplorerTriggers"), triggers);
	}

	protected void addBaseObjectPanels()
	{
		displayTab.add(ResourceMgr.getString("TxtDbExplorerTableDefinition"), tableDefinition);
		displayTab.add(ResourceMgr.getString("TxtDbExplorerSource"), tableSource);
	}

	protected void addDataPanel()
	{
		displayTab.add(ResourceMgr.getString("TxtDbExplorerData"), tableData);
	}

	private boolean sourceExpanded = false;

	public void toggleExpandSource()
	{
		if (sourceExpanded)
		{
			int last = this.splitPane.getLastDividerLocation();
			this.splitPane.setDividerLocation(last);
		}
		else
		{
			int current = this.splitPane.getDividerLocation();
			this.splitPane.setLastDividerLocation(current);
			this.splitPane.setDividerLocation(0);
		}
		sourceExpanded = !sourceExpanded;
	}

	public void setInitialFocus()
	{
		findPanel.setFocusToEntryField();
	}

	public void disconnect()
	{
		try
		{
			tableData.storeColumnOrder();
			// reset() sets and clears the ignoreStateChanged flag as well!
			this.reset();
			this.ignoreStateChanged = true;
			this.dbConnection = null;
			this.tableTypes.removeActionListener(this);
			this.displayTab.removeChangeListener(this);
			this.tableData.setConnection(null);
			this.tableTypes.removeAllItems();
			this.tableDefinition.setConnection(null);
		}
		finally
		{
			this.ignoreStateChanged = false;
		}
	}

	public void reset()
	{
		this.selectedTable = null;
		this.realTable = null;
		this.invalidateData();
		if (this.isBusy())
		{
			return;
		}

		tableList.saveColumnOrder();

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				tableList.cancelEditing();
				if (displayTab.getTabCount() > 0)
				{
					try
					{
						ignoreStateChanged = true;
						displayTab.setSelectedIndex(0);
					}
					finally
					{
						ignoreStateChanged = false;
					}
				}
				tableDefinition.reset();
				importedKeys.reset();
				exportedKeys.reset();
				indexes.reset();
				triggers.reset();
				tableSource.reset();
				tableData.reset();
				tableList.reset();
			}
		});
	}

	protected void resetCurrentPanel()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				Resettable panel = (Resettable)displayTab.getSelectedComponent();
				if (panel != null)
				{
					panel.reset();
				}
			}
		});
	}

	protected void invalidateData()
	{
		shouldRetrieveTable = true;
		shouldRetrieveTableData = true;
		shouldRetrieveTableSource = true;
		shouldRetrieveTriggers = true;
		shouldRetrieveIndexes = true;
		shouldRetrieveExportedKeys = true;
		shouldRetrieveImportedKeys = true;
	}

	public void setConnection(WbConnection connection)
	{
		dbConnection = connection;

		tableTypes.removeActionListener(this);
		displayTab.removeChangeListener(this);

		importedKeys.setConnection(connection);
		exportedKeys.setConnection(connection);
		tableData.setConnection(connection);
		tableDefinition.setConnection(connection);
		triggers.setConnection(connection);
		tableSource.setDatabaseConnection(connection);

		renameAction.setConnection(dbConnection);
		validator.setConnection(dbConnection);

		if (this.dbConnection != null)
		{
			this.findPanel.setColumnList(dbConnection.getMetadata().getTableListColumns());
		}

		reset();

		try
		{
			Collection<String> types = this.dbConnection.getMetadata().getObjectTypes();
			this.tableTypes.removeAllItems();
			this.tableTypes.addItem("*");

			for (String type : types)
			{
				this.tableTypes.addItem(type);
			}

			String tableView = this.dbConnection.getMetadata().getBaseTableTypeName() + "," + this.dbConnection.getMetadata().getViewTypeName();
			String add = Settings.getInstance().getProperty("workbench.dbexplorer.typefilter.additional", tableView);
			List<String> userFilter = StringUtil.stringToList(add, ";", true, true);

			for (String t : userFilter)
			{
				List<String> l = StringUtil.stringToList(t, ",");
				l.retainAll(types);
				String newFilter = StringUtil.listToString(l, ',');
				if (StringUtil.isNonBlank(newFilter) && !types.contains(newFilter) )
				{
					this.tableTypes.addItem(newFilter);
				}
			}

			this.tableTypes.setSelectedIndex(0);
			if (tableTypeToSelect != null)
			{
				this.tableTypes.setSelectedItem(this.tableTypeToSelect);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.setConnection()", "Error when setting up connection", e);
		}

		this.tableTypes.addActionListener(this);
		this.displayTab.addChangeListener(this);
		this.compileAction.setConnection(connection);
	}

	public boolean isReallyVisible()
	{
		if (!this.isVisible()) return false;
		Window w = SwingUtilities.getWindowAncestor(this);
		if (w == null) return false;
		return (w.isVisible());
	}

	public void setCatalogAndSchema(String newCatalog, String newSchema, boolean retrieve)
		throws Exception
	{
		this.currentSchema = newSchema;
		this.currentCatalog = newCatalog;

		invalidateData();

		if (this.isBusy())
		{
			setDirty(retrieve);
			return;
		}

		reset();

		if (!retrieve) return;

		if (this.dbConnection == null) return;

		if (isReallyVisible() || isClientVisible())
		{
			retrieve();
			setFocusToTableList();
		}
		else
		{
			setDirty(true);
		}
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		String info = tableList.getRowCount() + " " + ResourceMgr.getString("TxtTableListObjects");
		this.summaryStatusBarLabel.setText(info);
	}

	protected void setFocusToTableList()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				listPanel.requestFocus();
				tableList.requestFocus();
			}
		});
	}

	public void retrieve()
	{
		if (this.isBusy())
		{
			invalidateData();
			return;
		}

		if (dbConnection == null)
		{
			LogMgr.logDebug("TableListPanel.retrieve()", "Connection object not accessible", new Exception());
			WbSwingUtilities.showErrorMessageKey(this, "ErrConnectionGone");
			return;
		}

		if (dbConnection.getMetadata() == null)
		{
			LogMgr.logDebug("TableListPanel.retrieve()", "Database Metadata object not accessible", new Exception());
			WbSwingUtilities.showErrorMessageKey(this, "ErrConnectionMetaGone");
			return;
		}

		try
		{
			WbSwingUtilities.showWaitCursor(this);
			summaryStatusBarLabel.setText(ResourceMgr.getString("MsgRetrieving"));
			reset();

			// do not call setBusy() before reset() because
			// reset will do nothing if the panel is busy
			setBusy(true);

			String[] types = null;
			String type = (String)tableTypes.getSelectedItem();

			if (!"*".equals(type))
			{
				List<String> typeList = StringUtil.stringToList(type);
				types = new String[typeList.size()];
				for (int i=0; i < typeList.size(); i++)
				{
					types[i] = typeList.get(i);
				}
			}

			DataStore ds = dbConnection.getMetadata().getObjects(currentCatalog, currentSchema, types);
			dbConnection.getObjectCache().addTableList(ds, currentSchema);
			tableList.setOriginalOrder(ds);
			final DataStoreTableModel model = new DataStoreTableModel(ds);

			// Make sure some columns are not modified by the user
			// to avoid the impression that e.g. a table's catalog can be changed
			// by editing this list
			model.setValidator(validator);

			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					tableList.setModel(model, true);
					tableList.getExportAction().setEnabled(true);
					tableList.adjustRowsAndColumns();
					updateDisplayClients();
				}
			});

			setDirty(false);
		}
		catch (OutOfMemoryError mem)
		{
			setBusy(false);
			reset();
			setDirty(true);
			WbManager.getInstance().showOutOfMemoryError();
		}
		catch (LowMemoryException mem)
		{
			setBusy(false);
			reset();
			setDirty(true);
			WbManager.getInstance().showLowMemoryError();
		}
		catch (Throwable e)
		{
			if (e instanceof SQLException)
			{
				LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", (SQLException)e);
			}
			else
			{
				LogMgr.logError("TableListPanel.retrieve()", "Error retrieving table list", e);
			}
			String msg = ExceptionUtil.getDisplay(e);
			invalidateData();
			setDirty(true);
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
			setBusy(false);
			endTransaction();
		}
	}

	/**
	 *	Starts the retrieval of the tables in a background thread
	 */
	protected void startRetrieve(final boolean setFocus)
	{
		if (dbConnection == null)
		{
			LogMgr.logDebug("TableListPanel.startRetrieve()", "startRetrieve() called, but no connection available", new Exception());
			return;
		}

		Thread t = new WbThread("TableListPanel retrieve() thread")
		{
			@Override
			public void run()
			{
				retrieve();
				if (setFocus) setFocusToTableList();
			}
		};
		t.start();
	}

	public void panelSelected()
	{
		if (this.shouldRetrieve) startRetrieve(true);
	}

	@Override
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag && this.shouldRetrieve)
			this.startRetrieve(false);
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".tablelist.";
	}

	/**
	 * Save settings to global settings file
	 */
	public void saveSettings()
	{
		this.triggers.saveSettings();
		this.tableData.saveSettings();
		this.tableDefinition.saveSettings();
		String prefix = this.getClass().getName() + ".";
		storeSettings(Settings.getInstance(), prefix);
		findPanel.saveSettings(Settings.getInstance(), "workbench.quickfilter.");
	}

	/**
	 *	Restore settings from global settings file.
	 */
	public void restoreSettings()
	{
		String prefix = this.getClass().getName() + ".";
		readSettings(Settings.getInstance(), prefix);
		findPanel.restoreSettings(Settings.getInstance(), "workbench.quickfilter.");
		this.triggers.restoreSettings();
		this.tableData.restoreSettings();
		this.tableDefinition.restoreSettings();
	}


	/**
	 * Save settings to a workspace
	 *
	 * @param w the Workspace into which the settings should be saved
	 * @param index the index to be used in the Workspace
	 */
	public void saveToWorkspace(WbWorkspace w, int index)
	{
		tableData.saveToWorkspace(w, index);
		WbProperties props = w.getSettings();
		String prefix = getWorkspacePrefix(index);
		storeSettings(props, prefix);
		this.findPanel.saveSettings(props, "workbench.quickfilter.");
	}

	/**
	 *	Read settings from a workspace
	 *
	 * @param w the Workspace from which to read the settings
	 * @param index the index inside the workspace
	 */
	public void readFromWorkspace(WbWorkspace w, int index)
	{
		// first we read the global settings, then we'll let
		// the settings in the workspace override the global ones
		restoreSettings();
		tableData.readFromWorkspace(w, index);
		WbProperties props = w.getSettings();
		String prefix = getWorkspacePrefix(index);
		readSettings(props, prefix);
		findPanel.restoreSettings(props, "workbench.quickfilter.");
	}

	private void storeSettings(PropertyStorage props, String prefix)
	{
		try
		{
			String type = null;
			if (tableTypes != null && tableTypes.getModel().getSize() > 0)
			{
				type = (String)tableTypes.getSelectedItem();
			}
			else
			{
				// if tableTypes does not contain any items, this panel was never
				// displayed and we should use the value of the tableTypeToSelect
				// variable that was retrieved when the settings were read from
				// the workspace.
				type = tableTypeToSelect;
			}
			if (type != null) props.setProperty(prefix + "objecttype", type);

			props.setProperty(prefix + "divider", Integer.toString(this.splitPane.getDividerLocation()));
			props.setProperty(prefix + "exportedtreedivider", Integer.toString(exportedKeys.getDividerLocation()));
			props.setProperty(prefix + "importedtreedivider", Integer.toString(importedKeys.getDividerLocation()));
			props.setProperty(prefix + "exportedtree.retrieveall", Boolean.toString(exportedKeys.getRetrieveAll()));
			props.setProperty(prefix + "importedtree.retrieveall", Boolean.toString(importedKeys.getRetrieveAll()));
			List<String> objectListColumnOrder = tableList.saveColumnOrder();
			if (objectListColumnOrder != null)
			{
				props.setProperty(prefix + "columnorder", StringUtil.listToString(objectListColumnOrder, ','));
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableListPanel.storeSettings()", "Error storing settings", th);
		}
	}

	private void readSettings(PropertyStorage props, String prefix)
	{
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int maxWidth = (int)(d.getWidth() - 50);

		int loc = props.getIntProperty(prefix + "divider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			this.splitPane.setDividerLocation(loc);
		}

		loc = props.getIntProperty(prefix + "exportedtreedivider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			exportedKeys.setDividerLocation(loc);
		}

		loc = props.getIntProperty(prefix + "importedtreedivider",-1);
		if (loc != -1)
		{
			if (loc == 0 || loc > maxWidth) loc = 200;
			importedKeys.setDividerLocation(loc);
		}

		importedKeys.setRetrieveAll(props.getBoolProperty(prefix + "importedtree.retrieveall", true));
		exportedKeys.setRetrieveAll(props.getBoolProperty(prefix + "exportedtree.retrieveall", true));

		String defType = Settings.getInstance().getDefaultExplorerObjectType();
		if (Settings.getInstance().getStoreExplorerObjectType())
		{
			this.tableTypeToSelect = props.getProperty(prefix + "objecttype", defType);
		}
		else
		{
			this.tableTypeToSelect = defType;
		}
		String colString = props.getProperty(prefix + "columnorder", null);
		if (StringUtil.isNonEmpty(colString))
		{
			tableList.setNewColumnOrder(StringUtil.stringToList(colString, ","));
		}
	}


	/**
	 * Invoked when the selection in the table list has changed
	 */
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;

		if (e.getSource() == this.tableList.getSelectionModel())
		{
			if (this.showDataMenu != null)
			{
				this.showDataMenu.setEnabled(this.tableList.getSelectedRowCount() == 1);
			}
			this.updateDisplay();
		}
	}

	@Override
	public boolean canChangeSelection()
	{
		if (this.tableData == null) return true;
		if (GuiSettings.getConfirmDiscardResultSetChanges() && isModified())
		{
			if (!WbSwingUtilities.getProceedCancel(this, "MsgDiscardDataChanges"))
			{
				return false;
			}
			else
			{
				// for some reason the "valueIsAdjusting" flag is set to true if we wind up here
				// and I have no idea why.
				// But if the change of the selection is allowed, the valueIsAdjusting flag will
				// prevent updateDisplay() from applying the change.
				tableList.getSelectionModel().setValueIsAdjusting(false);
			}
		}
		return true;
	}

	public void updateDisplay()
	{
		int count = this.tableList.getSelectedRowCount();

		this.spoolData.setEnabled(count > 0);

		if (count > 1) return;

		int row = this.tableList.getSelectedRow();
		if (row < 0) return;

		this.selectedTable = createTableIdentifier(row);
		this.realTable = null;

		this.invalidateData();

		boolean isTable = isTable();
		boolean hasData = isTable;
		if (!isTable)
		{
			hasData = canContainData();
		}
		if (isTable)
		{
			showTablePanels();
		}
		else
		{
			this.showObjectDefinitionPanels(hasData);
		}

		this.tableData.reset();
		this.tableData.setReadOnly(dbConnection.getMetadata().isTableType(this.selectedTable.getType()));
		this.tableData.setTable(this.selectedTable);

		this.setShowDataMenuStatus(hasData);

		this.startRetrieveCurrentPanel();
	}

	private void setShowDataMenuStatus(boolean flag)
	{
		if (this.showDataMenu != null) this.showDataMenu.setEnabled(flag);
	}

	private boolean isSynonym(TableIdentifier table)
	{
		if (table == null) return false;
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = this.dbConnection.getDbSettings();
		return (meta.supportsSynonyms() && dbs.isSynonymType(table.getType()));
	}

	private boolean isTable()
	{
		if (this.selectedTable == null) return false;
		if (this.dbConnection == null) return false;
		DbMetadata meta = this.dbConnection.getMetadata();
		if (meta == null) return false;
		DbSettings dbs = this.dbConnection.getDbSettings();
		if (dbs == null) return false;

		String type = selectedTable.getType();
		if (meta.isTableType(type)) return true;
		if (meta.supportsSynonyms() && dbs.isSynonymType(type))
		{
			TableIdentifier rt = getObjectTable();
			if (rt == null) return false;
			return meta.isTableType(realTable.getType());
		}
		return false;
	}

	private boolean canContainData()
	{
		if (selectedTable == null) return false;
		String type = selectedTable.getType();
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = this.dbConnection.getDbSettings();
		if (meta.supportsSynonyms() && dbs.isSynonymType(type))
		{
			TableIdentifier rt = getObjectTable();
			if (rt == null) return false;
			type = rt.getType();
		}
		return meta.objectTypeCanContainData(type);
	}

	protected void retrieveTableSource()
	{
		if (selectedTable == null) return;

		tableSource.setPlainText(ResourceMgr.getString("TxtRetrievingSourceCode"));

		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(this.dbConnection);

		try
		{

			WbSwingUtilities.showWaitCursor(this);
			CharSequence sql = null;

			DbMetadata meta = this.dbConnection.getMetadata();
			DbSettings dbs = this.dbConnection.getDbSettings();

			String type = selectedTable.getType();

			if (meta.isExtendedObject(selectedTable))
			{
				sql = meta.getObjectSource(selectedTable);
			}
			else if (dbs.isViewType(type))
			{
				if (shouldRetrieveTable) retrieveTableDefinition();
				TableDefinition def = new TableDefinition(this.selectedTable, TableColumnsDatastore.createColumnIdentifiers(meta, tableDefinition.getDataStore()));
				sql = meta.getViewReader().getExtendedViewSource(def, true, false);
			}
			else if (dbs.isSynonymType(type))
			{
				SynonymDDLHandler synHandler = new SynonymDDLHandler();
				sql = synHandler.getSynonymSource(this.dbConnection, this.selectedTable, true);
			}
			else if ("sequence".equalsIgnoreCase(type))
			{
				SequenceReader reader = meta.getSequenceReader();
				if (reader != null)
				{
					CharSequence seqSql = reader.getSequenceSource(selectedTable.getCatalog(), this.selectedTable.getSchema(), this.selectedTable.getTableName());
					if (StringUtil.isEmptyString(seqSql))
					{
						sql = ResourceMgr.getString("MsgSequenceSourceNotImplemented") + " " + meta.getProductName();
					}
					else
					{
						sql = seqSql.toString();
					}
				}
			}
			else if (meta.isTableType(type))
			{
				sql = builder.getTableSource(selectedTable, true, true);
			}

			if (sql != null && dbConnection.getDbSettings().ddlNeedsCommit())
			{
				sql = sql.toString() + "\nCOMMIT;\n";
			}

			final String s = (sql == null ? "" : sql.toString());
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					tableSource.setText(s);
					tableSource.setCaretPosition(0, false);
				}
			});
			shouldRetrieveTableSource = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.retrieveTableSource()", "Error retrieving table source", e);
			final String msg = ExceptionUtil.getDisplay(e);
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					tableSource.setText(msg);
				}
			});
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	private void retrieveTableDefinition()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			tableDefinition.retrieve(selectedTable);
			shouldRetrieveTable = false;
		}
		catch (SQLException e)
		{
			shouldRetrieveTable = true;
			throw e;
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void showCancelMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgTryCancelling"));
	}

	protected void showWaitMessage()
	{
		this.showPopupMessagePanel(ResourceMgr.getString("MsgWaitRetrieveEnded"));
	}

	protected void showPopupMessagePanel(final String aMsg)
	{
		synchronized (msgLock)
		{
			if (this.infoWindow != null && infoLabel != null)
			{
				WbSwingUtilities.setLabel(infoLabel, aMsg, null);
				return;
			}

			JPanel p = new JPanel();
			p.setBorder(WbSwingUtilities.getBevelBorderRaised());
			p.setLayout(new BorderLayout());
			this.infoLabel = new JLabel(aMsg);
			this.infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
			p.add(this.infoLabel, BorderLayout.CENTER);
			JFrame f = (JFrame)SwingUtilities.getWindowAncestor(this);
			this.infoWindow = new JDialog(f, true);
			this.infoWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			this.infoWindow.getContentPane().setLayout(new BorderLayout());
			this.infoWindow.getContentPane().add(p, BorderLayout.CENTER);
			this.infoWindow.setUndecorated(true);
			this.infoWindow.setSize(260,50);
			WbSwingUtilities.center(this.infoWindow, f);
		}

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				infoWindow.setVisible(true);
			}
		});
	}

	private void closeInfoWindow()
	{
		if (this.infoWindow != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					infoLabel = null;
					infoWindow.getOwner().setEnabled(true);
					infoWindow.setVisible(false);
					infoWindow.dispose();
					infoWindow = null;
				}
			});
		}
	}

	protected Thread panelRetrieveThread;

	protected void startCancelThread()
	{
		Thread t = new WbThread("TableListPanel Cancel")
		{
			@Override
			public void run()
			{
				try
				{
					if (tableData.isRetrieving())
					{
						showCancelMessage();
						tableData.cancelRetrieve();
					}
					else if (importedKeys.isRetrieving())
					{
						showCancelMessage();
						importedKeys.cancel();
					}
					else if (exportedKeys.isRetrieving())
					{
						showCancelMessage();
						exportedKeys.cancel();
					}
					else
					{
						showWaitMessage();
					}

					if (panelRetrieveThread != null)
					{
						panelRetrieveThread.join();
						panelRetrieveThread = null;
					}
				}
				catch (InterruptedException e)
				{
				}
				setBusy(false);
				invalidateData();
				startRetrieveThread();
			}
		};
		t.start();
	}

	protected void startRetrieveCurrentPanel()
	{
		if (isBusy())
		{
			startCancelThread();
		}
		else
		{
			startRetrieveThread();
		}
	}

	protected void startRetrieveThread()
	{
		panelRetrieveThread = new WbThread("TableListPanel RetrievePanel")
		{
			@Override
			public void run()
			{
				try
				{
					retrieveCurrentPanel();
				}
				finally
				{
					panelRetrieveThread = null;
				}
			}
		};
		panelRetrieveThread.start();
	}

	protected void retrieveCurrentPanel()
	{
		if (this.dbConnection == null) return;

		if (this.isBusy() || this.dbConnection.isBusy())
		{
			this.invalidateData();
			this.resetCurrentPanel();
			return;
		}

		if (this.tableList.getSelectedRowCount() <= 0) return;
		int index = this.displayTab.getSelectedIndex();

		this.setBusy(true);

		try
		{
			synchronized (this.connectionLock)
			{
				switch (index)
				{
					// Index 0 to 2 are always the same, so this can be dealt with here
					case 0:
						if (this.shouldRetrieveTable) this.retrieveTableDefinition();
						break;
					case 1:
						if (this.shouldRetrieveTableSource) this.retrieveTableSource();
						break;
					case 2:
						if (this.shouldRetrieveTableData)
						{
							this.tableData.showData(!this.shiftDown);
							this.shouldRetrieveTableData = false;
						}
						break;
					default:
						retrievePanel();
				}
			}
		}
		catch (Throwable ex)
		{
			LogMgr.logError("TableListPanel.retrieveCurrentPanel()", "Error retrieving panel " + index, ex);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
			this.setBusy(false);
			this.repaint();
			closeInfoWindow();
			endTransaction();
		}
	}

	private void retrievePanel()
		throws SQLException
	{
		Component panel = displayTab.getSelectedComponent();
		if (panel == this.indexPanel && shouldRetrieveIndexes)
		{
			retrieveIndexes();
		}
		else if (panel == importedKeys && shouldRetrieveImportedKeys)
		{
			retrieveImportedTables();
		}
		else if (panel == exportedKeys && shouldRetrieveExportedKeys)
		{
			retrieveExportedTables();
		}
		else if (panel == triggers && shouldRetrieveTriggers)
		{
			retrieveTriggers();
		}
	}

	private void endTransaction()
	{
		if (this.dbConnection.getProfile().getUseSeparateConnectionPerTab())
		{
			if (this.dbConnection.selectStartsTransaction() && !this.dbConnection.getAutoCommit())
			{
				try { this.dbConnection.commit(); } catch (Throwable th) {}
			}
		}
	}

	private final Object busyLock = new Object();

	public boolean isBusy()
	{
		synchronized (busyLock)
		{
			return this.busy;
		}
	}

	protected void setBusy(boolean aFlag)
	{
		synchronized (busyLock)
		{
			this.busy = aFlag;
			this.dbConnection.setBusy(aFlag);
		}
	}

	@Override
	public TableIdentifier getObjectTable()
	{
		if (this.selectedTable == null) return null;
		if (!isSynonym(selectedTable)) return selectedTable;

		if (realTable == null)
		{
			realTable = dbConnection.getMetadata().resolveSynonym(selectedTable);
		}
		return realTable;
	}

	protected void retrieveTriggers()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showDefaultCursor(this);
			triggers.readTriggers(getObjectTable());
			this.shouldRetrieveTriggers = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveTriggers = true;
			LogMgr.logError("TableListPanel.retrieveTriggers()", "Error retrieving triggers", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveIndexes()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			DbMetadata meta = this.dbConnection.getMetadata();
			DataStore ds = meta.getIndexReader().getTableIndexInformation(getObjectTable());
			final DataStoreTableModel model = new DataStoreTableModel(ds);
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					indexes.setModel(model, true);
					indexes.adjustRowsAndColumns();
				}
			});
			this.shouldRetrieveIndexes = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveIndexes = true;
			LogMgr.logError("TableListPanel.retrieveIndexes()", "Error retrieving indexes", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	protected void retrieveExportedTables()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			exportedKeys.retrieve(getObjectTable());
			this.shouldRetrieveExportedKeys = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveExportedKeys = true;
			LogMgr.logError("TableListPanel.retrieveExportedTables()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
	}

	protected void retrieveImportedTables()
		throws SQLException
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			importedKeys.retrieve(getObjectTable());
			this.shouldRetrieveImportedKeys = false;
		}
		catch (Throwable th)
		{
			this.shouldRetrieveImportedKeys = true;
			LogMgr.logError("TableListPanel.retrieveImportedTables()", "Error retrieving table references", th);
			WbSwingUtilities.showErrorMessage(this, ExceptionUtil.getDisplay(th));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}

	@Override
	public void reload()
	{
		if (!WbSwingUtilities.checkConnection(this, dbConnection)) return;
		this.reset();
		this.startRetrieve(false);
	}

	private void showTableData(final int panelIndex, final boolean appendText)
	{
		PanelContentSender sender = new PanelContentSender(this.parentWindow);
		String sql = buildSqlForTable();
		if (sql == null) return;

		sender.sendContent(sql, panelIndex, appendText);
	}

	private String buildSqlForTable()
	{
		if (this.selectedTable == null) return null;

		if (this.shouldRetrieveTable || this.tableDefinition.getRowCount() == 0)
		{
			try
			{
				this.retrieveTableDefinition();
			}
			catch (Exception e)
			{
				LogMgr.logError("TableListPanel.buidlSqlForTable()", "Error retrieving table definition", e);
				String msg = ExceptionUtil.getDisplay(e);
				WbSwingUtilities.showErrorMessage(this, msg);
				return null;
			}
		}

		StringBuilder select = null;
		String sql = tableDefinition.getSelectForTable();
		if (sql == null)
		{
			String msg = ResourceMgr.getString("ErrNoColumnsRetrieved").replace("%table%", this.selectedTable.getTableName());
			WbSwingUtilities.showErrorMessage(this, msg);
			return null;
		}
		select = new StringBuilder(sql.length() + 40);
		select.append("-- @WbResult ");
		select.append(selectedTable.getTableName());
		select.append('\n');
		select.append(sql);
		select.append(';');
		return select.toString();
	}

	/**
	 * Invoked when the type dropdown changes or one of the additional actions
	 * is invoked that are put into the context menu of the table list
	 *
	 * @param e the Event that ocurred
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (ignoreStateChanged) return;

		if (e.getSource() == this.tableTypes)
		{
			try
			{
				this.showObjectDefinitionPanels(false);
				this.startRetrieve(true);
			}
			catch (Exception ex)
			{
				LogMgr.logError("TableListPanel.actionPerformed()", "Error while retrieving", ex);
			}
		}
		else
		{
			String command = e.getActionCommand();

			if (command.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX) && this.parentWindow != null)
			{
				try
				{
					final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));
					final boolean appendText = WbAction.isCtrlPressed(e);
					// Allow the selection change to finish so that
					// we have the correct table name in the instance variables
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							showTableData(panelIndex, appendText);
						}
					});
				}
				catch (Exception ex)
				{
					LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
				}
			}
		}
	}

	private boolean isClientVisible()
	{
		if (this.tableListClients == null) return false;
		for (JTable table : tableListClients)
		{
			if (table.isVisible()) return true;
		}
		return false;
	}

	protected void updateDisplayClients()
	{
		if (this.tableListClients == null) return;

		TableModel model = this.tableList.getModel();
		for (JTable table : tableListClients)
		{
			if (table != null && model != null)
			{
				table.setModel(model);
				if (table instanceof WbTable)
				{
					WbTable t = (WbTable)table;
					t.adjustRowsAndColumns();
				}
				table.repaint();
			}
		}
	}

	@Override
	public void addTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) this.tableListClients = new ArrayList<JTable>();
		if (!this.tableListClients.contains(aClient)) this.tableListClients.add(aClient);
		if (tableList != null && tableList.getRowCount() > 0)
		{
			updateDisplayClients();
		}
	}

	@Override
	public void removeTableListDisplayClient(JTable aClient)
	{
		if (this.tableListClients == null) return;
		this.tableListClients.remove(aClient);
	}

	/**
	 * Return a TableIdentifier for the given row number in the table list.
	 *
	 * @param row the row from the tableList Table
	 * @return a TableIdentifier for that row
	 */
	private TableIdentifier createTableIdentifier(int row)
	{
		String name = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		String catalog = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		String type = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		String comment = this.tableList.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS);
		TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
		tbl.setType(type);
		tbl.setNeverAdjustCase(true);
		tbl.setComment(comment);
		return tbl;
	}

	@Override
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public List<DbObject> getSelectedObjects()
	{
		int[] rows = this.tableList.getSelectedRows();
		int count = rows.length;
		if (count == 0) return null;

		List<DbObject> result = new ArrayList<DbObject>(count);
		for (int i=0; i < count; i++)
		{
			DbObject db = (DbObject)tableList.getDataStore().getRow(rows[i]).getUserObject();
			if (db == null)
			{
				TableIdentifier table = createTableIdentifier(rows[i]);
				table.checkQuotesNeeded(dbConnection);
				result.add(table);
			}
			else
			{
				result.add(db);
			}
		}
		return result;
	}

	@Override
	public void selectTable(TableIdentifier table)
	{
		for (int row = 0; row < this.tableList.getRowCount(); row ++)
		{
			TableIdentifier tbl = createTableIdentifier(row);
			if (tbl.equals(table))
			{
				displayTab.setSelectedIndex(0);
				tableList.scrollToRow(row);
				tableList.setRowSelectionInterval(row, row);
				break;
			}
		}
	}

	@Override
	public void exportData()
	{
		if (!WbSwingUtilities.checkConnection(this, this.dbConnection)) return;
		int rowCount = this.tableList.getSelectedRowCount();
		if (rowCount <= 0) return;

		TableExporter exporter = new TableExporter(this.dbConnection);
		Frame f = parentWindow;
		if (f == null) f = (Frame)SwingUtilities.getWindowAncestor(this);
		exporter.exportTables(getSelectedObjects(), f);
		exporter.startExport(f);
	}

	/** Invoked when the displayed tab has changed.
	 *	Retrieve table detail information here.
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (this.ignoreStateChanged) return;

		if (e.getSource() == this.displayTab)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					startRetrieveCurrentPanel();
				}
			});
		}
	}

	/**
	 * If an index is created in the TableDefinitionPanel it
	 * sends a PropertyChange event. This will invalidate
	 * the currently retrieved index list
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (TableDefinitionPanel.INDEX_PROP.equals(evt.getPropertyName()))
		{
			this.shouldRetrieveIndexes = true;
		}
		else if (TableDefinitionPanel.DEFINITION_PROP.equals(evt.getPropertyName()))
		{
			invalidateData();
			this.shouldRetrieveTable = false;
		}
		else if (Settings.PROPERTY_DBEXP_INSTANT_FILTER.equals(evt.getPropertyName()))
		{
			((QuickFilterPanel)findPanel).setFilterOnType(Settings.getInstance().getDbExpFilterDuringTyping());
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		this.shiftDown = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
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

}
