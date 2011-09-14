/*
 * TableDefinitionPanel.java
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.db.*;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CreateDummySqlAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.ColumnAlterAction;
import workbench.gui.actions.CreatePKAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.DropPKAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.EmptyTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererFactory;
import workbench.interfaces.DbData;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A panel to display the table definition information inside the DbExplorer.
 *
 * @see workbench.db.DbMetadata#getTableDefinition(TableIdentifier)
 *
 * @author Thomas Kellerer
 */
public class TableDefinitionPanel
	extends JPanel
	implements ActionListener, PropertyChangeListener, ListSelectionListener, Resettable, DbObjectList
{
	public static final String INDEX_PROP = "index";
	public static final String DEFINITION_PROP = "tableDefinition";

	private final Object connectionLock = new Object();

	private WbTable tableDefinition;
	private JLabel tableNameLabel;
	private QuickFilterPanel columnFilter;
	private WbAction createIndexAction;
	private CreatePKAction createPKAction;
	private DropPKAction dropPKAction;
	private ColumnAlterAction alterColumnsAction;
	private TableIdentifier currentTable;
	private InsertRowAction addColumn;
	private DeleteRowAction deleteColumn;

	private WbConnection dbConnection;
	private WbAction reloadAction;
	private DropDbObjectAction dropColumnsAction;
	private JPanel toolbar;
	private boolean busy;
	private FlatButton alterButton;
	private ColumnChangeValidator validator = new ColumnChangeValidator();
	private boolean doRestore;
	private boolean initialized;

	public TableDefinitionPanel()
	{
		super();
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_initGui();
			}
		});
	}

	private void _initGui()
	{
		if (initialized) return;

		this.tableDefinition = new WbTable(true, false, false);
		this.tableDefinition.setAdjustToColumnLabel(false);
		this.tableDefinition.setSelectOnRightButtonClick(true);
		this.tableDefinition.getExportAction().setEnabled(true);

		tableDefinition.setReadOnly(!GuiSettings.allowAlterInDbExplorer());
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_ALLOW_ALTER_TABLE);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setEnabled(false);
		this.reloadAction.addToInputMap(this.tableDefinition);

		toolbar = new JPanel(new GridBagLayout());

		alterColumnsAction = new ColumnAlterAction(tableDefinition);
		alterColumnsAction.setReloader(this);

		columnFilter  = new QuickFilterPanel(this.tableDefinition, true, "columnlist");
		// Setting the column list now, ensures that the dropdown will be displayed
		// properly in the QuickFilterPanel, although it wouldn't be necessary
		// as the column list will be updated automatically when the model of the table changes
		columnFilter.setColumnList(TableColumnsDatastore.TABLE_DEFINITION_COLS);

		DbData db = new DbData()
		{
			@Override
			public long addRow()
			{
				return tableDefinition.addRow();
			}

			@Override
			public void deleteRow()
			{
				tableDefinition.deleteRow();
			}

			@Override
			public void deleteRowWithDependencies()
			{
			}

			@Override
			public boolean startEdit()
			{
				return true;
			}

			@Override
			public int duplicateRow()
			{
				return -1;
			}

			@Override
			public void endEdit()
			{
			}
		};
		addColumn = new InsertRowAction(db);
		addColumn.initMenuDefinition("MnuTxtAddCol");

		deleteColumn = new DeleteRowAction(db);
		deleteColumn.initMenuDefinition("MnuTxtDropColumn");

		columnFilter.addToToolbar(addColumn, true, true);
		columnFilter.addToToolbar(deleteColumn, 1);
		columnFilter.addToToolbar(reloadAction, 0);

		GridBagConstraints cc = new GridBagConstraints();

		cc.anchor = GridBagConstraints.WEST;
		cc.fill = GridBagConstraints.NONE;
		cc.gridx = 0;
		cc.weightx = 0.0;
		cc.ipadx = 0;
		cc.ipady = 0;
		cc.insets = new Insets(0, 0, 0, 5);
		toolbar.add(columnFilter, cc);

		JLabel l = new JLabel(ResourceMgr.getString("LblTable") + ":");
		cc.fill = GridBagConstraints.NONE;
		cc.gridx ++;
		cc.weightx = 0.0;
		cc.insets = new Insets(0, 5, 0, 5);
		toolbar.add(l, cc);

		tableNameLabel = new JLabel();
		cc.fill = GridBagConstraints.HORIZONTAL;
		cc.gridx ++;
		cc.weightx = 1.0;
		cc.fill = GridBagConstraints.NONE;
		cc.insets = new Insets(0, 0, 0, 0);
		toolbar.add(tableNameLabel, cc);

		cc.fill = GridBagConstraints.HORIZONTAL;
		cc.gridx ++;
		cc.weightx = 0;
		cc.fill = GridBagConstraints.NONE;
		cc.anchor = GridBagConstraints.EAST;
		cc.insets = new Insets(0, 15, 0, 0);
		alterButton = new FlatButton(alterColumnsAction);
		alterButton.setIcon(null);
		toolbar.add(alterButton, cc);

		WbScrollPane scroll = new WbScrollPane(this.tableDefinition);
		this.setLayout(new BorderLayout());
		this.add(toolbar, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);

		this.createIndexAction = new WbAction(this, "create-index");
		this.createIndexAction.setEnabled(false);
		this.createIndexAction.initMenuDefinition("MnuTxtCreateIndex");

		createPKAction = new CreatePKAction(this);
		dropPKAction = new DropPKAction(this);

		tableDefinition.addPopupAction(CreateDummySqlAction.createDummyInsertAction(this, tableDefinition.getSelectionModel()), true);
		tableDefinition.addPopupAction(CreateDummySqlAction.createDummySelectAction(this, tableDefinition.getSelectionModel()), false);

		tableDefinition.getSelectionModel().addListSelectionListener(this);
		tableDefinition.addPopupAction(this.createPKAction, true);
		tableDefinition.addPopupAction(this.dropPKAction, false);
		tableDefinition.addPopupAction(this.createIndexAction, false);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(tableDefinition);
		policy.setDefaultComponent(tableDefinition);
		setFocusCycleRoot(false);
		setFocusTraversalPolicy(policy);

		if (Settings.getInstance().showFocusInDbExplorer())
		{
			tableDefinition.showFocusBorder();
		}

		if (doRestore)
		{
			restoreSettings();
		}
		initialized = true;
	}

	protected void fireTableDefinitionChanged()
	{
		firePropertyChange(DEFINITION_PROP, null, this.currentTable.getTableName());
	}

	protected void fireIndexChanged(String indexName)
	{
		firePropertyChange(INDEX_PROP, null, indexName);
	}

	private final Object busyLock = new Object();
	public boolean isBusy()
	{
		synchronized (this.busyLock)
		{
			return busy;
		}
	}

	private void setBusy(boolean flag)
	{
		synchronized (this.busyLock)
		{
			busy = flag;
		}
	}

	public void dispose()
	{
		if (tableDefinition != null) tableDefinition.dispose();
		Settings.getInstance().removePropertyChangeListener(this);
	}

	/**
	 * Retrieve the definition of the given table.
	 */
	public void retrieve(TableIdentifier table)
		throws SQLException
	{
		this.currentTable = table;
		initGui();
		retrieveTableDefinition();
	}

	protected void retrieveTableDefinition()
		throws SQLException
	{
		if (currentTable == null) return;

		if (this.isBusy()) return;

		synchronized (connectionLock)
		{
			try
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						tableDefinition.reset();
						reloadAction.setEnabled(false);
						String msg = "<html>" + ResourceMgr.getString("TxtRetrieveTableDef") + " <b>" + currentTable.getTableName() + "</b></html>";
						tableNameLabel.setText(msg);
					}
				});
				DbMetadata meta = this.dbConnection.getMetadata();
				DataStore def = meta.getObjectDetails(currentTable);
				final TableModel model = def == null ? EmptyTableModel.EMPTY_MODEL : new DataStoreTableModel(def) ;

				if (def instanceof TableColumnsDatastore)
				{
					DataStoreTableModel dsModel = (DataStoreTableModel)model;
					// Make sure some columns are not modified by the user
					// to avoid the impression that e.g. the column's position
					// can be changed by editing that column
					dsModel.setValidator(validator);

					int typeIndex = dsModel.findColumn(TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME);
					int posIndex = dsModel.findColumn("POSITION");
					int pkIndex = dsModel.findColumn("PK");
					dsModel.setNonEditableColums(typeIndex, posIndex, pkIndex);

					if (meta.isTableType(currentTable.getType()) || meta.getViewTypeName().equals(currentTable.getType()))
					{
						List<ColumnIdentifier> cols = TableColumnsDatastore.createColumnIdentifiers(meta, def);
						TableDefinition tbl = new TableDefinition(currentTable, cols);
						dbConnection.getObjectCache().addTable(tbl);
					}
				}

				alterButton.setVisible(dbConnection.getDbSettings().columnCommentAllowed(currentTable.getType()));

				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						applyTableModel(model);
					}
				});
				alterColumnsAction.setSourceTable(dbConnection, currentTable);
				alterColumnsAction.setEnabled(false);
				boolean canAddColumn = dbConnection.getDbSettings().getAddColumnSql() != null && GuiSettings.allowAlterInDbExplorer();
				addColumn.setEnabled(canAddColumn && isTable());
			}
			catch (SQLException e)
			{
				tableNameLabel.setText(ExceptionUtil.getDisplay(e));
				throw e;
			}
			finally
			{
				reloadAction.setEnabled(true);
				setBusy(false);
			}
		}
	}

	protected void applyTableModel(TableModel model)
	{
		tableDefinition.setPrintHeader(this.currentTable.getTableName());
		tableDefinition.setAutoCreateColumnsFromModel(true);
		tableDefinition.setModel(model, true);
		if (model instanceof EmptyTableModel)
		{
			tableNameLabel.setText("");
		}
		else
		{
			tableNameLabel.setText("<html><b>" + currentTable.getTableName() + "</b></html>");
		}

		TableColumnModel colmod = tableDefinition.getColumnModel();

		// Assign the correct renderer to display java.sql.Types values
		// should only appear for table definitions
		try
		{
			int typeIndex = colmod.getColumnIndex(TableColumnsDatastore.JAVA_SQL_TYPE_COL_NAME);
			TableColumn col = colmod.getColumn(typeIndex);
			col.setCellRenderer(RendererFactory.getSqlTypeRenderer());
		}
		catch (IllegalArgumentException e)
		{
			// The IllegalArgumentException will be thrown by getColumnIndex()
			// rather than returning a -1 as other methods do.

			// If the Types column is not present, we can simply return as the
			// other columns will then not be there as well.
			return;
		}

		// hide the the columns "SCALE/SIZE", "PRECISION"
		// they don't need to be displayed as this is "included" in the
		// displayed (DBMS) data type already

		// Columns may not be removed from the underlying DataStore because
		// that is also used to retrieve the table source and DbMetadata
		// relies on all columns being present when that datastore is passed
		// to getTableSource()

		// So we need to remove those columns from the view
		String[] columns = new String[] { "SCALE/SIZE", "PRECISION" };
		for (String name : columns)
		{
			try
			{
				int index = colmod.getColumnIndex(name);
				TableColumn col = colmod.getColumn(index);
				colmod.removeColumn(col);
			}
			catch (IllegalArgumentException e)
			{
				// ignore, this is expected for some table types
			}
		}
	}

	@Override
	public void reset()
	{
		if (!initialized) return;

		currentTable = null;
		tableDefinition.reset();
		reloadAction.setEnabled(false);
	}

	private DropDbObjectAction getDropColumnAction()
	{
		if (this.dropColumnsAction == null)
		{
			dropColumnsAction = new DropDbObjectAction("MnuTxtDropColumn", this, tableDefinition.getSelectionModel(), this);
			dropColumnsAction.setDropper(new ColumnDropper());
		}
		return dropColumnsAction;
	}

	public void setConnection(WbConnection conn)
	{
		initGui();
		this.dbConnection = conn;
		this.createIndexAction.setEnabled(this.dbConnection != null);
		this.reloadAction.setEnabled(this.dbConnection != null);

		validator.setConnection(dbConnection);

		if (dbConnection != null && dbConnection.getDbSettings().canDropType("column"))
		{
			DropDbObjectAction action = getDropColumnAction();
			action.setAvailable(true);
			this.tableDefinition.addPopupAction(action, false);
		}
		else if (this.dropColumnsAction != null)
		{
			dropColumnsAction.setAvailable(false);
		}

		addColumn.setEnabled(false);
	}

	/**
	 * Implement the Reloadable interface for the reload action.
	 * This method should not be called directly, use {@link #retrieve(workbench.db.TableIdentifier) }
	 * instead.
	 */
	@Override
	public void reload()
	{
		if (this.currentTable == null) return;
		if (this.dbConnection == null) return;

		initGui();

		WbThread t = new WbThread("TableDefinition Retrieve")
		{
			@Override
			public void run()
			{
				try
				{
					retrieveTableDefinition();
					fireTableDefinitionChanged();
				}
				catch (SQLException ex)
				{
					LogMgr.logError("TableDefinitionPanel.reload()", "Error loading table definition", ex);
				}
			}
		};
		t.start();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.createIndexAction)
		{
			createIndex();
		}
	}

	@Override
	public List<DbObject> getSelectedObjects()
	{
		if (this.tableDefinition.getSelectedRowCount() <= 0) return null;
		int[] rows = this.tableDefinition.getSelectedRows();

		List<DbObject> columns = new ArrayList<DbObject>(rows.length);

		for (int i=0; i < rows.length; i++)
		{
			String column = this.tableDefinition.getValueAsString(rows[i], TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			columns.add(new ColumnIdentifier(column));
		}
		return columns;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

	@Override
	public TableIdentifier getObjectTable()
	{
		return this.currentTable;
	}

	protected void createIndex()
	{
		if (this.tableDefinition.getSelectedRowCount() <= 0) return;
		int[] rows = this.tableDefinition.getSelectedRows();
		int count = rows.length;
		List<IndexColumn> columns = new ArrayList<IndexColumn>(count);
		String indexName = ResourceMgr.getString("TxtNewIndexName");
		//String indexName = WbSwingUtilities.getUserInput(this, msg, defaultName);
		if (StringUtil.isBlank(indexName)) return;

		for (int i=0; i < count; i++)
		{
			String colName = this.tableDefinition.getValueAsString(rows[i], TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME).toLowerCase();
			IndexColumn col = new IndexColumn(colName, null);
			columns.add(col);
		}

		IndexReader reader = this.dbConnection.getMetadata().getIndexReader();
		String sql = reader.buildCreateIndexSql(this.currentTable, indexName, false, columns);
		if (!sql.trim().endsWith(";"))
		{
			sql += ";\n";
		}

		String title = ResourceMgr.getString("TxtWindowTitleCreateIndex");

		if (dbConnection.shouldCommitDDL())
		{
			sql += "\nCOMMIT;\n";
		}
		RunScriptPanel panel = new RunScriptPanel(dbConnection, sql);
		panel.openWindow(this, title, indexName);

		if (panel.wasRun())
		{
			fireIndexChanged(indexName);
		}
	}

	protected boolean isTable()
	{
		return (currentTable != null ? "TABLE".equalsIgnoreCase(currentTable.getType()) : false);
	}

	protected boolean hasPkColumn()
	{
		if (!isTable()) return false;
		for (int row = 0; row < this.tableDefinition.getRowCount(); row++)
		{
			String flag = tableDefinition.getValueAsString(row, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_PK_FLAG);
			boolean isPk = StringUtil.stringToBool(flag);
			if (isPk) return true;
		}
		return false;
	}

	/**
	 * Invoked when the selection in the table list has changed
	 */
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!initialized) return;

		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.tableDefinition.getSelectionModel())
		{
			boolean rowsSelected = (this.tableDefinition.getSelectedRowCount() > 0);

			boolean isTable = isTable();
			boolean hasPk = hasPkColumn();
			createPKAction.setEnabled(rowsSelected && isTable && !hasPk);
			dropPKAction.setEnabled(isTable && hasPk);
			createIndexAction.setEnabled(rowsSelected && isTable);
			deleteColumn.setEnabled(rowsSelected && isTable && GuiSettings.allowAlterInDbExplorer());
		}
	}

	/**
	 * Returns a SELECT statement for retrieve all rows and columns from the displayed table.
	 *
	 * @see TableSelectBuilder#getSelectForTable(workbench.db.TableIdentifier)
	 */
	public String getSelectForTable()
	{
		List<ColumnIdentifier> cols = TableColumnsDatastore.createColumnIdentifiers(this.dbConnection.getMetadata(), this.tableDefinition.getDataStore());
		TableSelectBuilder builder = new TableSelectBuilder(dbConnection);
		return builder.getSelectForColumns(currentTable, cols);
	}

	public int getRowCount()
	{
		if (tableDefinition == null) return 0;
		return this.tableDefinition.getRowCount();
	}

	public DataStore getDataStore()
	{
		if (tableDefinition == null) return null;
		return this.tableDefinition.getDataStore();
	}

	public void restoreSettings()
	{
		if (columnFilter != null)
		{
			this.columnFilter.restoreSettings();
			doRestore = false;
		}
		else
		{
			doRestore = true;
		}
	}

	public void saveSettings()
	{
		if (columnFilter != null)
		{
			this.columnFilter.saveSettings();
			doRestore = false;
		}
		else
		{
			doRestore = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(GuiSettings.PROPERTY_ALLOW_ALTER_TABLE))
		{
			tableDefinition.setReadOnly(!GuiSettings.allowAlterInDbExplorer());
		}
	}
}
