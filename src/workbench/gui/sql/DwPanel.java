/*
 * DwPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Window;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.JOptionPane;
import javax.swing.JOptionPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.components.GenericRowMonitor;
import workbench.gui.components.WbTextCellEditor;
import workbench.gui.sql.ReferenceTableNavigator;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.OneLineTableModel;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.DbData;
import workbench.interfaces.DbUpdater;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.StatementRunner;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *	A Panel which displays the result of a SELECT statement and
 *  can save changes to the database
 *
 *	@author support@sql-workbench.net
 */
public class DwPanel
	extends JPanel
	implements TableModelListener, ListSelectionListener, ChangeListener,
						 DbData, DbUpdater, Interruptable, JobErrorHandler
{
	public static final String PROP_UPDATE_TABLE = "updateTable";
	
	protected WbTable dataTable;
	
	protected DwStatusBar statusBar;
	
	private String sql;
	private String lastMessage;
	protected WbConnection dbConnection;
	private boolean hasResultSet;
	
	protected WbScrollPane scrollPane;
	private long lastExecutionTime = 0;
	
	private boolean showLoadProgress;
	private boolean savingData = false;
	
	protected UpdateDatabaseAction updateAction;
	protected InsertRowAction insertRow;
	protected CopyRowAction duplicateRow;
	protected DeleteRowAction deleteRow;
	protected SelectKeyColumnsAction selectKeys;
	
	private boolean editingStarted;
	private boolean batchUpdate;
	private boolean manageUpdateAction;
	private boolean showErrorMessages;
	private boolean readOnly;
	
	private String[] lastResultMessages;
	private StatementRunner stmtRunner;
	private GenericRowMonitor genericRowMonitor;
	private ReferenceTableNavigator referenceNavigator;
	
	public DwPanel()
	{
		this(null);
	}
	
	public DwPanel(DwStatusBar aStatusBar)
	{
		JTextField stringField = new JTextField();
		stringField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		stringField.addMouseListener(new TextComponentMouseListener());
		
		JTextField numberField = new JTextField();
		numberField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		numberField.addMouseListener(new TextComponentMouseListener());
		
		this.initLayout(aStatusBar);
		
		this.setDoubleBuffered(true);
		this.dataTable.addTableModelListener(this);
		
		this.updateAction = new UpdateDatabaseAction(this);
		this.insertRow = new InsertRowAction(this);
		this.deleteRow = new DeleteRowAction(this);
		this.duplicateRow = new CopyRowAction(this);
		this.selectKeys = new SelectKeyColumnsAction(this);
		
		dataTable.addPopupAction(this.updateAction, true);
		dataTable.addPopupAction(this.insertRow, true);
		dataTable.addPopupAction(this.deleteRow, false);
		
		this.dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.dataTable.setRowSelectionAllowed(true);
		this.dataTable.getSelectionModel().addListSelectionListener(this);
		this.dataTable.setHighlightRequiredFields(Settings.getInstance().getHighlightRequiredFields());
		this.dataTable.setStatusBar(this.statusBar);
		
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(dataTable);
		pol.addComponent(dataTable);
		this.setFocusTraversalPolicy(pol);
		this.genericRowMonitor = new GenericRowMonitor(this.statusBar);
	}

	public void initTableNavigation(MainWindow container)
	{
		this.referenceNavigator = new ReferenceTableNavigator(this, container);
	}
	
	public SelectKeyColumnsAction getSelectKeysAction()
	{
		return this.selectKeys;
	}

	public void checkAndSelectKeyColumns()
	{
		if (checkUpdateTable())
		{	
			dataTable.selectKeyColumns();
		}
	}
	
	public void setManageUpdateAction(boolean aFlag)
	{
		this.manageUpdateAction = aFlag;
	}
	
	public void setDefaultStatusMessage(String aMessage)
	{
		this.statusBar.setReadyMsg(aMessage);
	}
	
	public void disconnect()
	{
		this.setConnection(null);
	}
	
	public void setCursor(Cursor newCursor)
	{
		super.setCursor(newCursor);
		this.dataTable.setCursor(newCursor);
	}
	
	public void setShowLoadProcess(boolean aFlag)
	{
		this.showLoadProgress = aFlag;
	}
	
	public void setPrintHeader(String header)
	{
		this.dataTable.setPrintHeader(header);
	}
	
	/**
	 *	Enables or disables the display of error messages.
	 */
	public void setShowErrorMessages(boolean aFlag)
	{
		this.showErrorMessages = aFlag;
	}
	
	/**
	 *	Defines the connection for this DwPanel.
	 */
	public void setConnection(WbConnection aConn)
	{
		this.clearContent();
		this.sql = null;
		this.lastMessage = null;
		this.dbConnection = aConn;
		this.hasResultSet = false;
		this.stmtRunner = null;
		this.clearStatusMessage();
	}

	private void createStatementRunner()
	{
		if (this.stmtRunner == null)
		{
			try
			{
				// Use reflection to create instance to avoid class loading upon startup
				this.stmtRunner = (StatementRunner)Class.forName("workbench.sql.DefaultStatementRunner").newInstance();
			}
			catch (Exception ignore)
			{
			}
			this.stmtRunner.setRowMonitor(this.genericRowMonitor);
		}
		if (this.stmtRunner != null)
		{
			this.stmtRunner.setConnection(this.dbConnection);
		}
	}
	
	/**
	 *	Sets the handler which performs the update to the database.
	 *
	 *  This delegate is passed to the UpdateDatabaseAction. The action will in turn
	 *  call the delegate's saveChangesToDatabase() method instead of ours.
	 *
	 *  @see workbench.interfaces.DbUpdater#saveChangesToDatabase()
	 *  @see #saveChangesToDatabase()
	 *  @see workbench.gui.sql.SqlPanel#saveChangesToDatabase()
	 */
	public void setUpdateHandler(DbUpdater aDelegate)
	{
		this.updateAction.setClient(aDelegate);
	}
	
	/**
	 * Prepare the DwPanel for saving any changes to the database.
	 * This will check for the PK columns and if necessary
	 * ask the user to specify them. 
	 * It will also prompt the user to verify the generated
	 * update statements. 
	 * If everything is OK, true will be returned
	 * If the user cancels the PK column selection or the 
	 * statement preview, false will be returned. In that 
	 * case saveChanges() should not be called
	 */
	boolean prepareDatabaseUpdate()
	{
		if (this.dbConnection == null) return false;
		DataStore ds = this.dataTable.getDataStore();
		if (ds == null) return false;
		
		boolean needPk = ds.needPkForUpdate();
		if (needPk)
		{
			boolean hasPk = dataTable.detectDefinedPkColumns();
			if (!hasPk)
			{
				hasPk = getTable().selectKeyColumns();
			}
			if (!hasPk) return false;
		}
		
		boolean pkComplete = ds.pkColumnsComplete();
		if (needPk && !pkComplete)
		{
			List<ColumnIdentifier> columns = ds.getMissingPkColumns();
			MissingPkDialog dialog = new MissingPkDialog(columns);
			boolean ok = dialog.checkContinue(this);
			if (!ok) return false;
		}
		
		// check if we really want to save the currentData
		// it fhe SQL Preview is not enabled this will
		// always return true, otherwise it depends on the user's
		// selection after the SQL preview has been displayed
		if (!this.shouldSaveChanges(this.dbConnection)) return false;
		return true;
	}
	
	/**
	 * Starts the saving of the data in the background
	 */
	public void saveChangesToDatabase()
	{
		if (savingData) 
		{
			Exception e = new IllegalStateException("Concurrent save called");
			LogMgr.logWarning("DwPanel.saveChangesToDatase()", "Save changes called while save in progress", e);
			return;
		}

		if (!this.prepareDatabaseUpdate()) return;

		WbThread t = new WbThread("DwPanel update")
		{
			public void run()
			{
				doSave();
			}
		};
		t.start();

	}
	
	protected void doSave()
	{
		try
		{
			this.saveChanges(dbConnection, this);
		}
		catch (Exception e)
		{
			// Exception have already been displayed to the user --> Log only
			LogMgr.logError("DwPanel.doSave()", "Error saving data", e);
		}
	}
	
	public boolean shouldSaveChanges(WbConnection aConnection)
	{
		if (!Settings.getInstance().getPreviewDml() && !this.dbConnection.getProfile().isConfirmUpdates()) return true;
		
		this.dataTable.stopEditing();
		DataStore ds = this.dataTable.getDataStore();
		DwUpdatePreview preview = new DwUpdatePreview();
		
		boolean doSave = preview.confirmUpdate(this, ds, dbConnection);
		
		return doSave;
	}
	
	public int saveChanges(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException
	{
		int rows = 0;
	
		synchronized (this)
		{
			this.dataTable.stopEditing();
			if (this.manageUpdateAction)
			{
				this.disableUpdateActions();
			}

			try
			{
				setStatusMessage(ResourceMgr.getString("MsgUpdatingDatabase"));
				savingData = true;
				DataStore ds = this.dataTable.getDataStore();
				long start, end;
				ds.setProgressMonitor(this.genericRowMonitor);
				start = System.currentTimeMillis();
				rows = ds.updateDb(aConnection, errorHandler);
				end = System.currentTimeMillis();
				ds.setProgressMonitor(null);
				long sqlTime = (end - start);
				this.lastMessage = ResourceMgr.getString("MsgUpdateSuccessfull");
				this.lastMessage = this.lastMessage + "\n" + rows + " " + ResourceMgr.getString("MsgRowsAffected") + "\n";
				this.lastMessage = this.lastMessage + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
				if (!ds.lastUpdateHadErrors())
				{
					endEdit();
				}
			}
			catch (SQLException e)
			{
				this.lastMessage = ExceptionUtil.getDisplay(e);
				rows = -1;
				throw e;
			}
			finally
			{
				savingData = false;
				this.clearStatusMessage();
				if (this.manageUpdateAction) this.enableUpdateActions();
			}
		}
		
		return rows;
	}
	
	protected void enableUpdateActions()
	{
		int rows = this.dataTable.getSelectedRowCount();
		this.insertRow.setEnabled(true);
		this.duplicateRow.setEnabled(rows == 1);
		this.deleteRow.setEnabled(rows > 0);
	}
	
	protected void disableUpdateActions()
	{
		this.insertRow.setEnabled(false);
		this.duplicateRow.setEnabled(false);
		this.deleteRow.setEnabled(false);
	}
	
	public String getCurrentSql()
	{
		return this.sql;
	}

	/**
	 * Pass the table to be used for future updates to the underlying 
	 * DataStore. 
	 * This will also reset the internal cache of the ReferenceTableNavigator.
	 * 
	 * @see workbench.gui.sql.ReferenceTableNavigator#reset()
	 * @see workbench.storage.DataStore#setUpdateTableToBeUsed(workbench.db.TableIdentifier)
	 */
	public void setUpdateTableToBeUsed(TableIdentifier table)
	{
		DataStore ds = this.dataTable.getDataStore();
		if (ds != null)
		{
			ds.setUpdateTableToBeUsed(table);
		}
		referenceNavigator.reset();
	}
	
	/**
	 * Define the update table to be used.
	 * @see workbench.storage.DataStore#setUpdateTable(TableIdentifier)
	 */
	public void setUpdateTable(TableIdentifier table)
	{
		this.setStatusMessage(ResourceMgr.getString("MsgRetrieveUpdateTableInfo"));
		try
		{
			DataStore ds = this.dataTable.getDataStore();
			if (ds != null)
			{
				ds.setUpdateTable(table);
			}
			checkResultSetActions();
			this.fireUpdateTableChanged();
		}
		finally
		{
			this.clearStatusMessage();
		}
	}
	
	private void fireUpdateTableChanged()
	{
		TableIdentifier table = null;
		
		if (this.getTable() != null)
		{
			DataStore ds = this.getTable().getDataStore();
			if (ds != null) table = ds.getUpdateTable();
		}
		if (table != null) firePropertyChange(PROP_UPDATE_TABLE, null, table.getTableExpression());
	}
	
	public void setReadOnly(boolean aFlag)
	{
		this.readOnly = aFlag;
		if (this.readOnly && this.editingStarted)
		{
			this.endEdit();
			this.insertRow.setEnabled(false);
			this.deleteRow.setEnabled(false);
			this.updateAction.setEnabled(false);
			this.dataTable.restoreOriginalValues();
		}
		else
		{
			this.insertRow.setEnabled(true);
		}
	}
	
	public boolean isReadOnly() { return this.readOnly; }
	
	public boolean checkUpdateTable()
	{
		if (this.readOnly) return false;
		this.setStatusMessage(ResourceMgr.getString("MsgCheckingUpdateTable"));
		boolean result = false;
		try
		{
			DataStore ds = this.dataTable.getDataStore();
			if (ds == null) return false;
			if (this.dbConnection == null) return false;
			if (this.sql == null) return false;
			result = ds.checkUpdateTable(this.sql, this.dbConnection);
			
			if (!result)
			{
				TableIdentifier tbl = selectUpdateTable();
				if (tbl != null)
				{
					this.setUpdateTable(tbl);
					result = true;
				}
			}
			
			if (result)
			{
				this.fireUpdateTableChanged();
			}
			
			this.selectKeys.setEnabled(result);
		}
		finally
		{
			this.clearStatusMessage();
		}
		return result;
	}
	
	protected TableIdentifier selectUpdateTable()
	{
		String csql = this.getCurrentSql();
		List tables = SqlUtil.getTables(csql, false);
		TableIdentifier table = null;

		if (tables.size() > 1)
		{
			String s = (String)JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(this),
				null, ResourceMgr.getString("MsgEnterUpdateTable"),
				JOptionPane.QUESTION_MESSAGE,
				null,tables.toArray(),null);
			
			if (s != null)
			{
				table = new TableIdentifier(s);
			}
		}
		return table;
	}
	
	public boolean hasKeyColumns()
	{
		if (this.dataTable.getDataStore() == null) return false;
		return this.dataTable.getDataStore().hasPkColumns();
	}
	
	public boolean isUpdateable()
	{
		if (this.dataTable.getDataStore() == null) return false;
		return this.dataTable.getDataStore().isUpdateable();
	}
	
	public boolean hasUpdateableColumns()
	{
		if (this.dataTable.getDataStore() == null) return false;
		return this.dataTable.getDataStore().hasUpdateableColumns();
	}
	
	public int getQueryTimeout()
	{
		return this.statusBar.getQueryTimeout();
	}
	
	public void setQueryTimeout(int value)
	{
		this.statusBar.setQueryTimeout(value);
	}
	
	public int getMaxRows()
	{
		return this.statusBar.getMaxRows();
	}
	
	public void setMaxRows(int aMax)
	{
		this.statusBar.setMaxRows(aMax);
	}
	
	/**
	 * Displays the last execution time in the status bar
	 */
	public void showlastExecutionTime()
	{
		statusBar.setExecutionTime(lastExecutionTime);
	}
	
	/**
	 *	Execute the given SQL statement and display the result. 
	 */
	public void runQuery(String aSql, boolean respectMaxRows)
		throws SQLException, Exception
	{
		this.lastMessage = null;
		this.statusBar.clearExecutionTime();

		if (this.stmtRunner == null) this.createStatementRunner();
		
		try
		{
			this.clearContent();
			this.setBatchUpdate(true);
			
			this.sql = aSql;
			int max = (respectMaxRows ? this.statusBar.getMaxRows() : 0);
			int timeout = this.statusBar.getQueryTimeout();
			
			this.stmtRunner.runStatement(aSql, max, timeout);
			StatementRunnerResult result = this.stmtRunner.getResult();

			if (result != null)
			{
				if (result.isSuccess())
				{
					this.hasResultSet = true;
					this.showData(result);
					this.lastExecutionTime = result.getExecutionTime();
				}
				else
				{
					this.hasResultSet = false;
					showError(result.getMessageBuffer().toString());
					if (this.showErrorMessages)
					{
						WbSwingUtilities.showErrorMessage(SwingUtilities.getWindowAncestor(this), result.getMessageBuffer().toString());
					}
				}
				checkResultSetActions();
			}
		}
		finally
		{
			this.setBatchUpdate(false);
			this.clearStatusMessage();
		}
	}
	
	/**
	 * Display any result set that is contained in the StatementRunnerResult. 
	 * @param result the result from the {@link workbench.interfaces.StatementRunner} to be displayed
	 *
	 * @see workbench.sql.DefaultStatementRunner
	 */
	public void showData(final StatementRunnerResult result)
		throws SQLException
	{
		if (result.hasDataStores())
		{
			showData(result.getDataStores().get(0), result.getSourceCommand());
		}
		else if (result.hasResultSets())
		{
			showData(result.getResultSets().get(0), result.getSourceCommand());
		}			
	}
	
	public void showData(final ResultSet result, final String sql)
		throws SQLException
	{
		DataStore newData = null;
		
		// passing the maxrows to the datastore is a workaround for JDBC drivers
		// which do not support the setMaxRows() method.
		// The datastore will make sure that not more rows are read than really requested
		if (this.showLoadProgress)
		{
			newData = new DataStore(result, true, this.genericRowMonitor, this.getMaxRows(), this.dbConnection);
		}
		else
		{
			newData = new DataStore(result, true, null, this.getMaxRows(), this.dbConnection);
		}
		showData(newData, sql);
	}
	
	public void showData(final DataStore newData, final String statement)
		throws SQLException
	{
		try
		{
			this.setBatchUpdate(true);
			this.hasResultSet = true;
			this.sql = statement;

			newData.setOriginalConnection(this.dbConnection);
			newData.setProgressMonitor(null);
			this.clearStatusMessage();

			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					dataTable.reset();
					dataTable.setAutoCreateColumnsFromModel(true);
					dataTable.setModel(new DataStoreTableModel(newData), true);
					dataTable.adjustOrOptimizeColumns(Settings.getInstance().getIncludeHeaderInOptimalWidth());
					StringBuilder header = new StringBuilder(80);
					header.append(ResourceMgr.getString("TxtPrintHeaderResultFrom"));
					header.append(sql);
					setPrintHeader(header.toString());

					dataTable.checkCopyActions();
					checkResultSetActions();
				}
			});
			
		}
		finally
		{
			setBatchUpdate(false);
		}
	}
	
	private void checkResultSetActions()
	{
		boolean hasResult = this.hasResultSet();
		int rows = this.getTable().getSelectedRowCount();

		this.dataTable.getExportAction().setEnabled(hasResult);
		this.updateAction.setEnabled(false);
		this.insertRow.setEnabled(hasResult);
		this.deleteRow.setEnabled(rows > 0);
		this.duplicateRow.setEnabled(rows == 1);
		this.selectKeys.setEnabled(hasResult);
		this.dataTable.checkCopyActions();
	}
	
	/**
	 *  This method will update the row info display on the statusbar.
	 */
	public void rowCountChanged()
	{
		int startRow = 0;
		int endRow = 0;
		int count = 0;
		startRow = this.dataTable.getFirstVisibleRow();
		endRow = this.dataTable.getLastVisibleRow(startRow);
		count = this.dataTable.getRowCount();
		statusBar.setRowcount(startRow + 1, endRow + 1, count);
	}
	
	public int duplicateRow()
	{
		if (this.readOnly) return -1;
		if (!this.startEdit()) return -1;
		final int newRow = this.dataTable.duplicateRow();
		if (newRow >= 0) 
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					dataTable.getSelectionModel().setSelectionInterval(newRow, newRow);
					dataTable.setEditingRow(newRow);
					dataTable.setEditingColumn(1);
					dataTable.editCellAt(newRow,1);
					CellEditor edit = dataTable.getCellEditor(newRow, 1);
					if (edit instanceof WbTextCellEditor)
					{
						((WbTextCellEditor)edit).requestFocus();
					}
					rowCountChanged();
				}
			});
		}
		
		return newRow;
	}
	
	public void deleteRow()
	{
		if (this.readOnly) return;
		if (!this.startEdit()) return;
		this.dataTable.deleteRow();
		this.rowCountChanged();
	}
	
	public long addRow()
	{
		if (this.readOnly) return -1;
		if (!this.startEdit()) return -1;
		long newRow = this.dataTable.addRow();
		if (newRow > -1) this.rowCountChanged();
		return newRow;
	}
	
	public boolean confirmCancel() { return true; }
	
	public void cancelExecution()
	{
		if (this.stmtRunner != null)
		{
			this.stmtRunner.cancel();
		}
	}
	
	public String getLastMessage()
	{
		if (this.lastResultMessages != null)
		{
			StringBuilder msg = new StringBuilder(lastResultMessages.length * 80);
			for (int i=0; i < lastResultMessages.length; i++)
			{
				msg.append(lastResultMessages[i]);
				msg.append('\n');
			}
			this.lastMessage = msg.toString();
			this.lastResultMessages = null;
		}
		
		if (this.lastMessage == null) this.lastMessage = "";
		
		return this.lastMessage;
	}
	
	public boolean hasResultSet()
	{ 
		return this.hasResultSet; 
	}
	
	/**
	 *	Returns true if the DataStore of the Table has been modified.
	 *	@see workbench.storage.DataStore#isModified()
	 */
	public boolean isModified()
	{
		DataStore ds = this.dataTable.getDataStore();
		if (ds == null) return false;
		else return ds.isModified();
	}
	
	private void initLayout(DwStatusBar status)
	{
		this.setLayout(new BorderLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.dataTable = new WbTable(true, true, true);
		this.dataTable.setRowResizeAllowed(Settings.getInstance().getAllowRowHeightResizing());
		if (status != null)
		{
			this.statusBar = status;
		}
		else
		{
			this.statusBar = new DwStatusBar();
			Border b = BorderFactory.createCompoundBorder(new EmptyBorder(2, 0, 0, 0), new EtchedBorder());
			this.statusBar.setBorder(b);
			this.add(this.statusBar, BorderLayout.SOUTH);
		}
		
		this.statusBar.setFocusable(false);
		this.setFocusable(false);
		this.scrollPane = new WbScrollPane(this.dataTable);
		this.scrollPane.getViewport().addChangeListener(this);
		
		this.add(this.scrollPane, BorderLayout.CENTER);
		this.dataTable.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.dataTable.setAdjustToColumnLabel(true);
	}
	
	public void updateStatusBar()
	{
		this.rowCountChanged();
	}
	/**
	 *	Show a message in the status panel.
	 * 
	 *  @see DwStatusBar#setStatusMessage(String)
	 */
	public void setStatusMessage(final String aMsg)
	{
		this.statusBar.setStatusMessage(aMsg);
	}
	
	/**
	 *	Clears the display on the status bar.
	 * 
	 *  @see DwStatusBar#clearStatusMessage()
	 */
	public void clearStatusMessage()
	{
		this.statusBar.clearStatusMessage();
	}

	public void showError(String error)
	{
		this.setMessageDisplayModel(this.getErrorTableModel(error));	
	}
	
	protected void setMessageDisplayModel(final TableModel aModel)
	{
		if (this.dataTable.getModel() == aModel) return;
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				dataTable.setModel(aModel);
				TableColumnModel colMod = dataTable.getColumnModel();
				TableColumn col = colMod.getColumn(0);
				col.setPreferredWidth(getWidth() - 10);
				statusBar.setRowcount(0,0,0);
			}
		});
	}
	
	/**
	 *	Clears everything.
	 *	<ul>
	 *	<li>Ends the edit mode</li>
	 *	<li>removes all rows from the table</li>
	 *	<li>sets the hasResultSet flag to false</li>
	 *  <li>the lastMessage is set to an empty string</li>
	 *  <li>the last SQL is set to null</li>
	 *  <li>the statusbar display is cleared</li>
	 *  </ul>
	 */
	public void clearContent()
	{
		this.endEdit();
		this.dataTable.reset();
		this.hasResultSet = false;
		this.lastMessage = null;
		this.sql = null;
		this.statusBar.clearRowcount();
		this.selectKeys.setEnabled(false);
	}
	
	public int getActionOnError(int errorRow, String errorColumn, String data, String errorMessage)
	{
		String msg = ResourceMgr.getString("ErrUpdateSqlError");
		try
		{
			String d = "";
			if (data != null)
			{
				if (data.length() > 50)
					d = data.substring(0, 50) + "...";
				else
					d = data;
			}
			
			msg = StringUtil.replace(msg,"%statement%", d);
			msg = StringUtil.replace(msg,"%message%", errorMessage);
			
			String r = "";
			if (errorRow > -1)
			{
				r = ResourceMgr.getString("TxtErrorRow").replaceAll("%row%", Integer.toString(errorRow));
			}
			msg = StringUtil.replace(msg, "%row%", r);
		}
		catch (Exception e)
		{
			LogMgr.logError("DwPanel.getActionOnError()", "Error while building error message", e);
			msg = "An error occurred during update: \n" + errorMessage;
		}
		
		Window w = SwingUtilities.getWindowAncestor(this);
		int choice = WbSwingUtilities.getYesNoIgnoreAll(w, msg);
		
		int result = JobErrorHandler.JOB_ABORT;
		
		if (choice == JOptionPane.YES_OPTION)
		{
			result = JobErrorHandler.JOB_CONTINUE;
		}
		else if (choice == WbSwingUtilities.IGNORE_ALL)
		{
			result = JobErrorHandler.JOB_IGNORE_ALL;
		}
		return result;
	}
	
	/**
	 *	Returns a TableModel which displays an error text.
	 *	This is used to show a hint in the table panel that an error
	 *  occurred and the actual error message is displayed in the log
	 *  panel
	 */
	private TableModel getErrorTableModel(String aMsg)
	{
		String title = ResourceMgr.getString("ErrMessageTitle");
		OneLineTableModel errorMessageModel = new OneLineTableModel(title, aMsg);
		return errorMessageModel;
	}
	
	public WbTable getTable() 
	{ 
		return this.dataTable; 
	}
	
	/**
	 *	Stops the editing mode of the displayed WbTable:
	 *	<ul>
	 *	<li>the status column is turned off</li>
	 *  <li>the edit actions are enabled/disabled correctly </li>
	 *  <li>the originalValues for the DataStore are restored </li>
	 *  </ul>
	 */
	public void endEdit()
	{
		endEdit(true);
	}
	
	public void endEdit(boolean restoreData)
	{
		if (!this.editingStarted) return;
		this.editingStarted = false;
		this.dataTable.stopEditing();
		this.dataTable.setShowStatusColumn(false);
		this.updateAction.setEnabled(false);
		int rows = this.dataTable.getSelectedRowCount();
		this.insertRow.setEnabled(true);
		this.deleteRow.setEnabled(rows > 0);
		this.duplicateRow.setEnabled(rows == 1);
		if (restoreData) this.dataTable.restoreOriginalValues();
	}
	
	/**
	 *	Starts the "edit" mode of the table. It will not start the edit
	 *  mode, if the table is "read only" meaning if no update table (=database
	 *  table) is defined.
	 *  The following actions are carried out:
	 *	<ul>
	 *	<li>if the updateable flag is not yet set, try to find out which table to update</li>
	 *  <li>the status column is displayec</li>
	 *  <li>the corresponding actions (insert row, delete row) are enabled</li>
	 *  <li>the startEdit action is turned to "switched on"</li>
	 *  </ul>
	 */
	public boolean startEdit()
	{
		if (this.readOnly) return false;
		
		this.editingStarted = false;
		
		int[] selectedRows = this.dataTable.getSelectedRows();
		// if the result is not yet updateable (automagically)
		// then try to find the table. If the table cannot be
		// determined, then ask the user
		Window w = SwingUtilities.getWindowAncestor(this);
		
		if (!this.isUpdateable() && !this.checkUpdateTable())
		{
			// checkUpdateTable() will have taken every attempt to find an update table
			// including asking the user to select a table from a multi-table result set
			
			// So if we wind up here, there is no way to update the
			// underlying DataStore
			WbSwingUtilities.showErrorMessageKey(w, "MsgNoTables");
			this.setUpdateTable((TableIdentifier)null);
			this.updateAction.setEnabled(false);
			this.insertRow.setEnabled(false);
			this.deleteRow.setEnabled(false);
			this.duplicateRow.setEnabled(false);
			this.selectKeys.setEnabled(false);
			return false;
		}
		
		// Verify if the data is really updateable!
		boolean update = this.isUpdateable();
		
		if (update)
		{
			this.dataTable.setShowStatusColumn(true);
			if (this.updateAction != null)
			{
				if (this.dataTable.getDataStore().isModified())
				{
					this.updateAction.setEnabled(true);
				}
			}
			
			this.editingStarted = true;

			// When changing the table model (which is happening
			// when the status column is displayed) we need to restore
			// the selection
			int numSelectedRows = selectedRows.length;
			if (selectedRows.length > 0)
			{
				ListSelectionModel model = this.dataTable.getSelectionModel();
				model.setValueIsAdjusting(true);
				// make sure nothing is selected, then restore the old selection
				model.clearSelection();
				for (int i = 0; i < numSelectedRows; i++)
				{
					model.addSelectionInterval(selectedRows[i], selectedRows[i]);
				}
				model.setValueIsAdjusting(false);
			}

			checkResultSetActions();
		}
		else
		{
			String msg = null;
			TableIdentifier tbl = (this.dataTable.getDataStore() != null ? this.dataTable.getDataStore().getUpdateTable() : null);
			if (tbl == null)
			{
				msg = ResourceMgr.getString("MsgNoUpdateTable");
			}
			else if (!this.hasUpdateableColumns())
			{
				msg = ResourceMgr.getString("MsgNoUpdateColumns");
				msg = StringUtil.replace(msg, "%table%", tbl.getTableExpression());
			}
			WbSwingUtilities.showErrorMessage(w, msg);
		}
		
		return update;
	}
	
	public InsertRowAction getInsertRowAction() { return this.insertRow; }
	public CopyRowAction getCopyRowAction() { return this.duplicateRow; }
	public DeleteRowAction getDeleteRowAction() { return this.deleteRow; }
	public UpdateDatabaseAction getUpdateDatabaseAction() { return this.updateAction; }
	
	/**
	 *	Turns on the batchUpdate mode.
	 *  In this mode, the automatic switch to edit mode is disabled. This
	 *  is used when populating the table from the database otherwise, the
	 *  first row, which is retrieved will start the edit mode
	 */
	public void setBatchUpdate(boolean aFlag)
	{
		this.batchUpdate = aFlag;
	}

	public RowActionMonitor getRowMonitor()
	{
		return this.genericRowMonitor;
	}
	/**
	 *	If the user changes something in the result set (which is possible, as
	 *  the table defaults to beeing editable) the edit mode (with status column
	 *  and the different actions enabled) is switched on automatically.
	 */
	public void tableChanged(TableModelEvent e)
	{
		if (this.batchUpdate) return;
		if (this.readOnly) return;
		
		if (e.getFirstRow() != TableModelEvent.ALL_COLUMNS &&
			e.getFirstRow() != TableModelEvent.HEADER_ROW &&
			this.isModified() )
		{
			if (!this.editingStarted)
			{
				this.startEdit();
			}
			if (this.updateAction != null) this.updateAction.setEnabled(true);
		}
	}
	
	/**
	 *	This is called when the selection in the table changes.
	 *  The copy row action will be enabled when exactly one row
	 *  is selected
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (this.readOnly) return;
		long rows = this.dataTable.getSelectedRowCount();
		this.deleteRow.setEnabled(rows > 0);
		this.duplicateRow.setEnabled(rows == 1);
	}
	
	/**
	 *	Called from the viewport, when the display has been scrolled
	 *  We need to update the row display then.
	 */
	public void stateChanged(ChangeEvent e)
	{
		this.rowCountChanged();
	}
	
	public void fatalError(String msg)
	{
		WbSwingUtilities.showErrorMessage(this, msg);
	}
	
}
