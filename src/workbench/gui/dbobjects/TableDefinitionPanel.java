/*
 * TableDefinitionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.db.ColumnDropper;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.IndexColumn;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.QuickFilterPanel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.renderer.RendererFactory;
import workbench.gui.sql.ExecuteSqlDialog;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A panel to display the table definition information inside the DbExplorer.
 *
 * @see workbench.db.DbMetadata#getTableDefinition(TableIdentifier)
 *
 * @author  support@sql-workbench.net
 */
public class TableDefinitionPanel
	extends JPanel
	implements Reloadable, ActionListener, ListSelectionListener, Resettable, DbObjectList
{
	public static final String INDEX_PROP = "index";
	public static final String DEFINITION_PROP = "tableDefinition";

	private WbTable tableDefinition;
	private JLabel tableNameLabel;
	private QuickFilterPanel columnFilter;
	private WbAction createIndexAction;
	private TableIdentifier currentTable;
	private WbConnection dbConnection;
	private WbAction reloadAction;
	private DropDbObjectAction dropColumnsAction;
	private JPanel toolbar;
	private boolean busy;

	public TableDefinitionPanel()
	{
		super();
		this.tableDefinition = new WbTable(true, false, false);
		this.tableDefinition.setAdjustToColumnLabel(false);
		this.tableDefinition.setSelectOnRightButtonClick(true);
		this.tableDefinition.getExportAction().setEnabled(true);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setEnabled(false);
		this.reloadAction.addToInputMap(this.tableDefinition);

		toolbar = new JPanel(new GridBagLayout());

		String[] cols = new String[] {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "REMARKS", "JAVA_TYPE"};
		columnFilter  = new QuickFilterPanel(this.tableDefinition, cols, true, "columnlist");
		columnFilter.addToToolbar(reloadAction, 0);
		GridBagConstraints cc = new GridBagConstraints();

		cc.anchor = GridBagConstraints.WEST;
		cc.fill = GridBagConstraints.HORIZONTAL;
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
		cc.gridx ++;
		cc.weightx = 0.0;
		cc.fill = GridBagConstraints.NONE;
		cc.insets = new Insets(0, 0, 0, 0);
		toolbar.add(tableNameLabel, cc);

		cc.gridx ++;
		cc.weightx = 1.0;
		cc.fill = GridBagConstraints.HORIZONTAL;
		toolbar.add(new JPanel(), cc);

		WbScrollPane scroll = new WbScrollPane(this.tableDefinition);
		this.setLayout(new BorderLayout());
		this.add(toolbar, BorderLayout.NORTH);
		this.add(scroll, BorderLayout.CENTER);

		this.createIndexAction = new WbAction(this, "create-index");
		this.createIndexAction.setEnabled(false);
		this.createIndexAction.initMenuDefinition("MnuTxtCreateIndex");

		this.tableDefinition.getSelectionModel().addListSelectionListener(this);
		this.tableDefinition.addPopupAction(this.createIndexAction, true);
	}

	protected void fireTableDefinitionChanged()
	{
		firePropertyChange(DEFINITION_PROP, null, this.currentTable.getTableName());
	}

	protected void fireIndexChanged(String indexName)
	{
		firePropertyChange(INDEX_PROP, null, indexName);
	}

	public void showFocusBorder()
	{
		this.tableDefinition.showFocusBorder();
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

	/**
	 * Retrieve the definition of the given table.
	 */
	public void retrieve(TableIdentifier table)
		throws SQLException
	{
		this.currentTable = table;
		this.tableDefinition.reset();
		retrieveTableDefinition();
	}

	protected void retrieveTableDefinition()
		throws SQLException
	{
		if (this.isBusy()) return;

		synchronized (this.dbConnection)
		{
			try
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					public void run()
					{
						reloadAction.setEnabled(false);
						String msg = "<html>" + ResourceMgr.getString("TxtRetrieveTableDef") + " <b>" + currentTable.getTableName() + "</b></html>";
						tableNameLabel.setText(msg);
					}
				});
				DbMetadata meta = this.dbConnection.getMetadata();
				DataStore def = meta.getTableDefinition(this.currentTable);
				
				final DataStoreTableModel model = new DataStoreTableModel(def);
				WbSwingUtilities.invoke(new Runnable()
				{
					public void run()
					{
						applyTableModel(model);
					}
				});
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

	protected void applyTableModel(DataStoreTableModel model)
	{
		tableDefinition.setPrintHeader(this.currentTable.getTableName());
		tableDefinition.setAutoCreateColumnsFromModel(true);
		tableDefinition.setModel(model, true);
		tableNameLabel.setText("<html><b>" + currentTable.getTableName() + "</b></html>");

		// hide the the columns "SCALE/SIZE", "PRECISION"
		// they don't need to be displayed as this is "included" in the
		// displayed (DBMS) data type already

		// Columns may not be removed from the underlying DataStore because
		// that is also used to retrieve the table source and DbMetadata
		// relies on all tables being present when that datastore is passed
		// to getTableSource()
		
		// So we can only remove those columns from the view

		TableColumnModel colmod = tableDefinition.getColumnModel();

		// Assign the correct renderer to display java.sql.Types values
		// (should only appear for table definitions
		try
		{
			int typeIndex = colmod.getColumnIndex("java.sql.Types");
			TableColumn col = colmod.getColumn(typeIndex);//DbMetadata.COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE);
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
				// ignore, this is expected for some types
			}
		}
	}
	
	public void reset()
	{
		this.currentTable = null;
		this.tableDefinition.reset();
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
		this.dbConnection = conn;
		this.createIndexAction.setEnabled(this.dbConnection != null);
		this.reloadAction.setEnabled(this.dbConnection != null);
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
	}

	/**
	 * Implement the Reloadable interface for the reload action.
	 * This method should not be called directly, use {@link #retrieve(workbench.db.TableIdentifier) }
	 * instead.
	 */
	public void reload()
	{
		if (this.currentTable == null) return;
		if (this.dbConnection == null) return;

		this.tableDefinition.reset();

		WbThread t = new WbThread("TableDefinition Retrieve")
		{
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

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.createIndexAction)
		{
			createIndex();
		}
	}

	public List<DbObject> getSelectedObjects()
	{
		if (this.tableDefinition.getSelectedRowCount() <= 0) return null;
		int[] rows = this.tableDefinition.getSelectedRows();

		List<DbObject> columns = new ArrayList<DbObject>(rows.length);

		for (int i=0; i < rows.length; i++)
		{
			String column = this.tableDefinition.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			columns.add(new ColumnIdentifier(column));
		}
		return columns;
	}

	public Component getComponent()
	{
		return this;
	}

	public WbConnection getConnection()
	{
		return this.dbConnection;
	}

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
			String colName = this.tableDefinition.getValueAsString(rows[i], DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME).toLowerCase();
			IndexColumn col = new IndexColumn(colName, null);
			columns.add(col);
		}
		String sql = this.dbConnection.getMetadata().buildIndexSource(this.currentTable, indexName, false, columns);
		String title = ResourceMgr.getString("TxtWindowTitleCreateIndex");
		Window parent = SwingUtilities.getWindowAncestor(this);
		Frame owner = null;
		if (parent instanceof Frame)
		{
			owner = (Frame)parent;
		}
		ExecuteSqlDialog dialog = new ExecuteSqlDialog(owner, title, sql, indexName, this.dbConnection);
		dialog.setStartButtonText(ResourceMgr.getString("TxtCreateIndex"));
		dialog.setVisible(true);
		fireIndexChanged(indexName);
	}

	/**
	 * Invoked when the selection in the table list has changed
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		if (e.getSource() == this.tableDefinition.getSelectionModel())
		{
			boolean rowsSelected = (this.tableDefinition.getSelectedRowCount() > 0);
			this.createIndexAction.setEnabled(rowsSelected);
		}
	}

	public String getSelectForTable()
	{
		int colCount = this.tableDefinition.getRowCount();
		if (colCount == 0) return null;

		StringBuilder sql = new StringBuilder(colCount * 80);

		sql.append("SELECT ");
		DbMetadata meta = this.dbConnection.getMetadata();
		for (int i=0; i < colCount; i++)
		{
			String column = this.tableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			column = meta.quoteObjectname(column);
			if (i > 0 && i < colCount) sql.append(",\n");
			if (i > 0) sql.append("       ");
			sql.append(column);
		}
		sql.append("\nFROM ");
		sql.append(this.currentTable.getTableExpression(this.dbConnection));
		return sql.toString();
	}

	public int getRowCount()
	{
		return this.tableDefinition.getRowCount();
	}

	public DataStore getDataStore()
	{
		return this.tableDefinition.getDataStore();
	}

	public void restoreSettings()
	{
		this.columnFilter.restoreSettings();
	}

	public void saveSettings()
	{
		this.columnFilter.saveSettings();
	}
}
