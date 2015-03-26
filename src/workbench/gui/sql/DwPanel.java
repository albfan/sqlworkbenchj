/*
 * DwPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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

import workbench.WbManager;
import workbench.interfaces.DbData;
import workbench.interfaces.DbUpdater;
import workbench.interfaces.Interruptable;
import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.CreateDeleteScriptAction;
import workbench.gui.actions.DeleteDependentRowsAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ColumnOrderMgr;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.GenericRowMonitor;
import workbench.gui.components.OneLineTableModel;
import workbench.gui.components.TableRowHeader;
import workbench.gui.components.UpdateTableSelector;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTextCellEditor;
import workbench.gui.renderer.RendererSetup;
import workbench.resource.DataTooltipType;

import workbench.storage.DataStore;
import workbench.storage.NamedSortDefinition;
import workbench.storage.ResultColumnMetaData;
import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.ExceptionUtil;
import workbench.util.HtmlUtil;
import workbench.util.LowMemoryException;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A Panel which displays the result of a SELECT statement and
 * can save changes to the database
 *
 * @author Thomas Kellerer
 */
public class DwPanel
	extends JPanel
	implements TableModelListener, ListSelectionListener, ChangeListener,
				     DbData, DbUpdater, Interruptable, JobErrorHandler, PropertyChangeListener, StatusBar
{
	public static final String PROP_UPDATE_TABLE = "updateTable";
  private static int nextId;
  private final int id;
	protected WbTable dataTable;

	protected DwStatusBar statusBar;

	private String sql;
	private String lastMessage;
	protected WbConnection dbConnection;
	private boolean hasResultSet;

	protected WbScrollPane scrollPane;
	private long lastExecutionDuration;
	private boolean showLoadProgress;
	private boolean savingData = false;

	protected UpdateDatabaseAction updateAction;
	protected InsertRowAction insertRow;
	protected CopyRowAction duplicateRow;
	protected DeleteRowAction deleteRow;
	protected DeleteDependentRowsAction deleteDependentRow;
	protected CreateDeleteScriptAction createDeleteScript;
	protected SelectKeyColumnsAction selectKeys;

	private boolean batchUpdate;
	private boolean readOnly;
	private boolean sharedStatusBar;

	private StatementRunner stmtRunner;
	private GenericRowMonitor genericRowMonitor;
	private ReferenceTableNavigator referenceNavigator;
	private boolean showSQLAsTooltip;
	private JLabel sqlInfo;
	private boolean enableSqlInfo;
	private boolean disconnected;
	private boolean wasReUsed;

	public DwPanel()
	{
		this(null);
	}

	public DwPanel(DwStatusBar statusBar)
	{
		super();
    this.id = ++nextId;
		this.initLayout(statusBar);

		this.setDoubleBuffered(true);
		this.dataTable.addTableModelListener(this);

		this.updateAction = new UpdateDatabaseAction(this);
		this.updateAction.setEnabled(GuiSettings.getAlwaysEnableSaveButton());
		this.insertRow = new InsertRowAction(this);
		this.insertRow.setEnabled(false);
		this.deleteRow = new DeleteRowAction(this);
		this.deleteDependentRow = new DeleteDependentRowsAction(this);
		this.duplicateRow = new CopyRowAction(this);
		this.selectKeys = new SelectKeyColumnsAction(this);

		dataTable.addPopupAction(this.updateAction, true);
		dataTable.addPopupAction(this.insertRow, true);
		dataTable.addPopupAction(this.deleteRow, false);
		dataTable.addPopupAction(this.deleteDependentRow, false);

		this.dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.dataTable.setRowSelectionAllowed(true);
		this.dataTable.getSelectionModel().addListSelectionListener(this);
		this.dataTable.setStatusBar(this.statusBar);
		this.genericRowMonitor = new GenericRowMonitor(this.statusBar);
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_SHOW_RESULT_SQL);
		initColors();
	}

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 97 * hash + this.id;
    return hash;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final DwPanel other = (DwPanel)obj;
    if (this.id != other.id) return false;
    return true;
  }

  public int getId()
  {
    return id;
  }

	public void setReUsed(boolean flag)
	{
		this.wasReUsed = flag;
	}

	public boolean wasReUsed()
	{
		return wasReUsed;
	}

	public void dispose()
	{
		clearContent();
		Settings.getInstance().removePropertyChangeListener(this);
		if (referenceNavigator != null) referenceNavigator.dispose();
		WbAction.dispose(createDeleteScript,deleteDependentRow,deleteRow,duplicateRow,insertRow,selectKeys,updateAction);
		if (dataTable != null) dataTable.dispose();
	}

	public void setSqlInfoEnabled(boolean flag)
	{
		this.enableSqlInfo = flag;
		if (!enableSqlInfo)
		{
			hideSQLInfo();
		}
	}

	private void initColors()
	{
		dataTable.setRendererSetup(new RendererSetup());
		dataTable.setModifiedColor(GuiSettings.getColumnModifiedColor());
		dataTable.setHighlightRequiredFields(GuiSettings.getHighlightRequiredFields());
	}

	public void showCreateDeleteScript()
	{
		if (createDeleteScript == null)
		{
			this.createDeleteScript = new CreateDeleteScriptAction(dataTable);
			dataTable.addPopupActionAfter(createDeleteScript, deleteDependentRow);
		}
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
		if (checkUpdateTable() == TableCheck.tableOk)
		{
			dataTable.selectKeyColumns();
		}
	}

	public void setDefaultStatusMessage(String aMessage)
	{
		this.statusBar.setReadyMsg(aMessage);
	}

	public void disconnect()
	{
		this.setConnection(null);
	}

	@Override
	public void setCursor(Cursor newCursor)
	{
		super.setCursor(newCursor);
		dataTable.setCursor(newCursor);
	}

	public void setShowLoadProcess(boolean aFlag)
	{
		showLoadProgress = aFlag;
	}

	public void setPrintHeader(String header)
	{
		dataTable.setPrintHeader(header);
	}

	public int getStatusBarHeight()
	{
		if (this.statusBar == null) return 32; // default height;
		return statusBar.getPreferredSize().height;
	}

	public void detachConnection()
	{
		if (this.dbConnection != null)
		{
			this.dbConnection.removeChangeListener(this);
		}
		if (this.dataTable == null) return;
		DataStore ds = this.dataTable.getDataStore();
		if (ds != null)
		{
			ds.setOriginalConnection(null);
		}
		this.disconnected = true;
		this.dataTable.removePopupAction(updateAction);
		this.dataTable.removePopupAction(deleteDependentRow);
		this.dataTable.removePopupAction(deleteRow);
		this.dataTable.removePopupAction(insertRow);
		this.dataTable.removePopupAction(dataTable.getReplacer().getReplaceAction());
		if (this.referenceNavigator != null)
		{
			this.referenceNavigator.removeFromPopup();
		}

		dbConnection = null;
		if (stmtRunner != null)
		{
			stmtRunner.done();
		}
		stmtRunner = null;
		clearStatusMessage();
		checkResultSetActions();
	}

	/**
	 *	Defines the connection for this DwPanel.
	 */
	public void setConnection(WbConnection aConn)
	{
		if (this.dbConnection != null)
		{
			this.dbConnection.removeChangeListener(this);
		}

		clearContent();
		dbConnection = aConn;
		if (stmtRunner != null)
		{
			stmtRunner.done();
		}
		stmtRunner = null;
		clearStatusMessage();

		if (dbConnection != null)
		{
			setReadOnly(dbConnection.isSessionReadOnly());
			this.dbConnection.addChangeListener(this);
		}
	}

	private void createStatementRunner()
	{
		if (this.stmtRunner == null)
		{
			this.stmtRunner = new StatementRunner();
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
	 *
	 * This will check for the PK columns and if necessary
	 * ask the user to specify them.
	 * It will also prompt the user to verify the generated
	 * update statements.
	 * If everything is OK, true will be returned
	 * If the user cancels the PK column selection or the
	 * statement preview, false will be returned. In that
	 * case saveChanges() should not be called
	 */
	boolean prepareDatabaseUpdate(boolean confirm)
	{
		if (this.dbConnection == null) return false;
		DataStore ds = this.dataTable.getDataStore();
		if (ds == null) return false;

		dataTable.stopEditing();

		if (ds.getUpdateTable() == null)
		{
			// this can happen if the Save button is always enabled and was clicked before the
			// update table was checked.
			TableCheck check = checkUpdateTable();
			if (check != TableCheck.tableOk)
			{
				// no table --> can't do anything
				LogMgr.logError("DwPanel.prepareDatabaseUpdate()", "No update table found! Cannot save changes for SQL=" + ds.getGeneratingSql(), null);
				return false;
			}
		}

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

    if (confirm)
    {
      if (!this.shouldSaveChanges(this.dbConnection)) return false;
    }
		return true;
	}

	private boolean shouldSaveChanges(WbConnection aConnection)
	{
		this.dataTable.stopEditing();
		DataStore ds = this.dataTable.getDataStore();
		DwUpdatePreview preview = new DwUpdatePreview();

		boolean doSave = preview.confirmUpdate(this, ds, aConnection);

		return doSave;
	}

	/**
	 * Starts the saving of the data in the background
	 */
	@Override
	public void saveChangesToDatabase(boolean confirm)
	{
		if (savingData)
		{
			Exception e = new IllegalStateException("Concurrent save called");
			LogMgr.logWarning("DwPanel.saveChangesToDatase()", "Save changes called while save in progress", e);
			return;
		}

		if (!this.prepareDatabaseUpdate(confirm)) return;

		WbThread t = new WbThread("DwPanel update")
		{
			@Override
			public void run()
			{
				try
				{
					saveChanges(dbConnection, DwPanel.this);
				}
				catch (Exception e)
				{
					// Exception have already been displayed to the user --> Log only
					LogMgr.logError("DwPanel.doSave()", "Error saving data", e);
				}
			}
		};
		t.start();
	}

  @Override
  public WbConnection getConnection()
  {
    return dbConnection;
  }



	public int saveChanges(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException
	{
		int rows = 0;

		synchronized (this)
		{
			this.dataTable.stopEditing();
			this.disableUpdateActions();

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
				this.lastMessage = this.lastMessage + "\n " + ResourceMgr.getFormattedString("MsgRowsAffected", rows) + "\n";
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
				this.checkResultSetActions();
			}
		}

		return rows;
	}

	public void disableUpdateActions()
	{
		this.updateAction.setEnabled(GuiSettings.getAlwaysEnableSaveButton());
		this.insertRow.setEnabled(false);
		this.duplicateRow.setEnabled(false);
		this.deleteRow.setEnabled(false);
		this.deleteDependentRow.setEnabled(false);
	}

	/**
	 * Pass the full table definition to be used for future updates to the underlying
	 * DataStore.
	 * This will also reset the internal cache of the ReferenceTableNavigator.
	 *
	 * @see workbench.gui.sql.ReferenceTableNavigator#reset()
	 * @see workbench.storage.DataStore#setUpdateTableToBeUsed(workbench.db.TableIdentifier)
	 */
	public void defineUpdateTable(TableDefinition table)
	{
		DataStore ds = this.dataTable.getDataStore();
		if (ds != null && table != null)
		{
			ds.setUpdateTable(table);
		}
		if (referenceNavigator != null)
		{
			referenceNavigator.reset();
		}
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
		if (ds != null && table != null)
		{
			ds.setUpdateTableToBeUsed(table);
		}
		if (referenceNavigator != null)
		{
			referenceNavigator.reset();
		}
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
			if (ds != null && table != null)
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
		if (this.readOnly && isEditingStarted())
		{
			this.dataTable.cancelEditing();
		}
		checkResultSetActions();
	}

	public boolean isReadOnly()
	{
		return this.readOnly;
	}

	private static enum TableCheck
	{
		tableOk,
		cancel,
		noTable;
	}

	private TableCheck checkUpdateTable()
	{
		if (this.readOnly || dbConnection == null || sql == null) return TableCheck.noTable;

		setStatusMessage(ResourceMgr.getString("MsgCheckingUpdateTable"));
		statusBar.forcePaint();

		TableCheck checkResult = TableCheck.noTable;
		try
		{
			DataStore ds = this.dataTable.getDataStore();
			if (ds == null) return TableCheck.noTable;
			boolean result = ds.checkUpdateTable(this.dbConnection);

			if (!result)
			{
				UpdateTableSelector selector = new UpdateTableSelector(dataTable);
				TableIdentifier tbl = selector.selectUpdateTable();
				if (tbl == null) return TableCheck.cancel;
				if (tbl != null)
				{
					this.setUpdateTable(tbl);
					checkResult = TableCheck.tableOk;
				}
			}
			else
			{
				checkResult = TableCheck.tableOk;
			}
			this.selectKeys.setEnabled(checkResult == TableCheck.tableOk);
		}
		finally
		{
			this.clearStatusMessage();
		}
		return checkResult;
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

	public long getLastExecutionTime()
	{
		return lastExecutionDuration;
	}

	public void setStatusBar(DwStatusBar status)
	{
		if (this.statusBar != null)
		{
			this.remove(this.statusBar);
			statusBar.removeSelectionIndicator(this.dataTable);
		}
		sharedStatusBar = false;
		this.statusBar = status;
		this.add(this.statusBar, BorderLayout.SOUTH);
		if (this.dataTable != null)
		{
			this.dataTable.setStatusBar(this.statusBar);
		}
		this.invalidate();
		this.doLayout();
	}

	/**
	 * Displays the last execution duration in the status bar
	 */
	public void showLastExecutionDuration()
	{
		statusBar.setExecutionTime(lastExecutionDuration);
	}

	public void setSortDefinition(NamedSortDefinition sort)
	{
		DataStoreTableModel model = this.dataTable.getDataStoreTableModel();
		if (model != null)
		{
			try
			{
				this.dataTable.sortingStarted();
				model.setSortDefinition(sort);
			}
			finally
			{
				this.dataTable.sortingFinished();
			}
		}
	}

	public DataStore getDataStore()
	{
		if (dataTable == null) return null;
		return this.dataTable.getDataStore();
	}

	public NamedSortDefinition getCurrentSort()
	{
		if (dataTable == null) return null;
		return dataTable.getCurrentSort();
	}

	public void runCurrentSql(boolean respectMaxRows)
		throws SQLException, Exception
	{
		DataStore ds = getDataStore();
		if (ds == null) return;
		String generatingSql = ds.getGeneratingSql();
		if (generatingSql == null) return;

		NamedSortDefinition sort = this.dataTable.getCurrentSort();
		runQuery(generatingSql, respectMaxRows);
		if (sort != null)
		{
			dataTable.getDataStoreTableModel().setSortDefinition(sort);
			dataTable.adjustColumns();
		}
		if (GuiSettings.getShowMaxRowsReached())
		{
			checkLimitReachedDisplay();
		}

		if (showSQLAsTooltip)
		{
			showGeneratingSQLAsTooltip(); // re-create the tooltip because it contains the last execution time
		}
	}

	/**
	 *	Execute the given SQL statement and display the result.
	 */
	public boolean runQuery(String aSql, boolean respectMaxRows)
		throws SQLException, Exception
	{
		if (this.stmtRunner == null) this.createStatementRunner();

		boolean success = false;

		try
		{
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					clearContent();
					initColors();
				}
			});

			this.sql = aSql;
			int max = (respectMaxRows ? this.statusBar.getMaxRows() : 0);
			int timeout = this.statusBar.getQueryTimeout();

			this.stmtRunner.setMaxRows(max);
			this.stmtRunner.setQueryTimeout(timeout);
			this.stmtRunner.runStatement(aSql);
			StatementRunnerResult result = this.stmtRunner.getResult();

			if (result != null)
			{
				if (result.isSuccess())
				{
					success = true;
					showData(result);
				}
				else
				{
					String err = result.getMessageBuffer().toString();
					showError(err);
					WbSwingUtilities.showErrorMessage(SwingUtilities.getWindowAncestor(this), err);
				}
			}
		}
		finally
		{
			stmtRunner.done();
			clearStatusMessage();
		}
		return success;
	}

	/**
	 * Display any result set that is contained in the StatementRunnerResult.
	 * @param result the result from the {@link workbench.sql.StatementRunner} to be displayed
	 *
	 * @see workbench.sql.StatementRunner
	 */
	public void showData(final StatementRunnerResult result)
		throws SQLException
	{
		if (result == null || !result.isSuccess())
		{
			lastExecutionDuration = 0;
			hasResultSet = false;
		}
		else
		{
			if (result.hasDataStores())
			{
				showData(result.getDataStores().get(0), result.getSourceCommand(), result.getExecutionDuration());
			}
			else if (result.hasResultSets())
			{
				showData(result.getResultSets().get(0), result.getSourceCommand(), result.getExecutionDuration());
			}
		}
	}

	public void showData(final ResultSet result, final String sql, long executionTime)
		throws SQLException
	{
		DataStore newData = null;

		// passing the maxrows to the datastore is a workaround for JDBC drivers
		// which do not support the setMaxRows() method.
		// The datastore will make sure that not more rows are read than really requested
		try
		{
			if (this.showLoadProgress)
			{
				newData = new DataStore(result, true, this.genericRowMonitor, this.getMaxRows(), this.dbConnection);
			}
			else
			{
				newData = new DataStore(result, true, null, this.getMaxRows(), this.dbConnection);
			}
			showData(newData, sql, executionTime);
		}
		catch (LowMemoryException mem)
		{
			WbManager.getInstance().showLowMemoryError();
		}

	}

	public void showData(final DataStore newData, final String statement, long executionTime)
		throws SQLException
	{
		try
		{
			this.setBatchUpdate(true);
			this.hasResultSet = true;
			this.sql = statement;
			this.lastExecutionDuration = executionTime;

			newData.setOriginalConnection(this.dbConnection);
			newData.setProgressMonitor(null);

			clearStatusMessage();

			// Make sure this is executed on the EDT
			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					dataTable.reset();
					dataTable.setAutoCreateColumnsFromModel(true);
					dataTable.setModel(new DataStoreTableModel(newData), true);
					setPrintHeader(sql);
					checkResultSetActions();
					dataTable.applyHighlightExpression(newData.getGeneratingFilter());
					if (GuiSettings.getShowTableRowNumbers())
					{
						TableRowHeader.showRowHeader(dataTable);
					}
					if (enableSqlInfo && GuiSettings.getShowResultSQL())
					{
						showSQLInfo();
					}
				}
			});
		}
		finally
		{
			setBatchUpdate(false);
		}
	}

	private JTabbedPane getTabParent()
	{
		Component c = getParent();
		while (c != null)
		{
			if (c instanceof JTabbedPane)
			{
				return (JTabbedPane)c;
			}
			c = c.getParent();
		}
		return null;
	}

	private int getTabIndex(JTabbedPane tab)
	{
		if (tab == null) return -1;

		int index = tab.indexOfComponent(this);
		if (index == -1)
		{
			index = tab.indexOfComponent(this.getParent());
		}
		return index;
	}

	private boolean maxRowsReached()
	{
		if (!GuiSettings.getShowMaxRowsReached()) return false;

		int maxRows = getMaxRows();
		int rowCount = getTable().getRowCount();

		// Some drivers return one more row than defined by maxRows...
		boolean maxReached = maxRows > 0 && (maxRows == rowCount || maxRows == rowCount - 1);
		return maxReached;
	}

	private void clearWarningIcon()
	{
		JTabbedPane tab = getTabParent();
		int index = getTabIndex(tab);
		if (index > -1)
		{
			tab.setIconAt(index, null);
		}
	}

	public void showGeneratingSQLAsTooltip()
	{
		showGeneratingSQLAsTooltip(maxRowsReached(), GuiSettings.showSQLAsDataTooltip());
	}

  public void showGeneratingSQLAsTooltip(boolean includeMaxRowsWarning)
  {
    showGeneratingSQLAsTooltip(includeMaxRowsWarning, GuiSettings.showSQLAsDataTooltip());
  }

  public void showGeneratingSQLAsTooltip(DataTooltipType tooltipType)
  {
    showGeneratingSQLAsTooltip(maxRowsReached(), tooltipType);
  }

	public void showGeneratingSQLAsTooltip(boolean includeMaxRowsWarning, DataTooltipType tooltipType)
	{
		if (sql == null) return;

    if (tooltipType == DataTooltipType.none)
		{
			showSQLAsTooltip = false;
			return;
		}

		JTabbedPane tab = getTabParent();
		int index = getTabIndex(tab);
		if (index == -1) return;

		DataStore ds = getDataStore();
		if (ds == null) return;

		String timeString  = StringUtil.formatIsoTimestamp(ds.getLoadedAt());

		String msg = ResourceMgr.getFormattedString("TxtLastExec", timeString);
		String tip = "<html>";
		if (includeMaxRowsWarning)
		{
			tip += "<b>" + ResourceMgr.getString("MsgRetrieveAbort") + "</b><br>";
		}
		tip += "(" + msg + ")";

    if (tooltipType == DataTooltipType.full)
    {
      tip += "<br><pre>" + HtmlUtil.escapeXML(sql.trim(), false) + "</pre></html>";
    }
		tab.setToolTipTextAt(index, tip);
		showSQLAsTooltip = true;
		if (sqlInfo != null)
		{
			sqlInfo.setToolTipText(tip);
		}
	}

	public void checkLimitReachedDisplay()
	{
		JTabbedPane tab = getTabParent();
		int index = getTabIndex(tab);

		if (index > -1)
		{
			boolean maxRows = maxRowsReached();
			if (maxRows)
			{
				tab.setIconAt(index, getWarningIcon());
				JComponent comp = (JComponent)tab.getTabComponentAt(index);
				if (GuiSettings.getShowMaxRowsTooltip())
				{
					WbSwingUtilities.showToolTip(comp,
						"<html><p style=\"margin-top:10px;margin-bottom:10px;margin-left:5px;margin-right:5px\">" +
						ResourceMgr.getString("MsgRetrieveAbort") +
						"</p></html>");
				}
			}
			else
			{
				clearWarningIcon();
			}
		}
	}

	private ImageIcon getWarningIcon()
	{
		return IconMgr.getInstance().getLabelIcon("alert");
	}

	public void readColumnComments()
	{
		readColumnComments(null);
	}

	public void readColumnComments(TableDefinition tableDef)
	{
		DataStore ds = getDataStore();
		if (ds == null) return;
		try
		{
			setStatusMessage(ResourceMgr.getString("MsgRetrievingColComments"));
			ResultColumnMetaData meta = new ResultColumnMetaData(ds);
			meta.retrieveColumnRemarks(ds.getResultInfo(), tableDef);
		}
		catch (Exception e)
		{
			LogMgr.logError("DwPanel.readColumnComments()", "Error reading comments", e);
		}
		finally
		{
			clearStatusMessage();
		}
	}

	private void checkResultSetActions()
	{
		if (this.disconnected)
		{
			disableUpdateActions();
			if (this.selectKeys != null) this.selectKeys.setEnabled(false);
			return;
		}

		boolean hasResult = this.hasResultSet();
		int rows = this.getTable().getSelectedRowCount();

		if (readOnly)
		{
			this.disableUpdateActions();
		}
		else
		{
			if (this.updateAction != null) this.updateAction.setEnabled(GuiSettings.getAlwaysEnableSaveButton() || isModified());
			if (this.duplicateRow != null) this.duplicateRow.setEnabled(rows == 1);
			if (this.deleteRow != null) this.deleteRow.setEnabled(rows > 0);
			if (this.insertRow != null) this.insertRow.setEnabled(hasResult);
			if (this.deleteDependentRow != null) this.deleteDependentRow.setEnabled(rows > 0);
		}

		dataTable.setReadOnly(this.readOnly);

		if (this.selectKeys != null) this.selectKeys.setEnabled(hasResult);
		this.dataTable.checkCopyActions();
	}

	/**
	 *  This method will update the row info display on the statusbar.
	 */
	public void rowCountChanged()
	{
		if (this.isVisible())
		{
			int startRow = 0;
			int endRow = 0;
			int count = 0;
			startRow = this.dataTable.getFirstVisibleRow();
			endRow = this.dataTable.getLastVisibleRow();
			count = this.dataTable.getRowCount();
			statusBar.setRowcount(startRow + 1, endRow + 1, count);
		}
	}

	@Override
	public int duplicateRow()
	{
		if (this.dataTable.getSelectedRowCount() != 1) return -1;
		int row = this.dataTable.getSelectedRow();
		if (row < 0) return -1;

		if (this.readOnly) return -1;
		if (!this.startEdit(false)) return -1;

		final int newRow = this.dataTable.duplicateRow(row);

		if (newRow >= 0)
		{
			// Make the new row the current row
			// and start editing in the first column
			EventQueue.invokeLater(new Runnable()
			{
				@Override
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

	@Override
	public void deleteRowWithDependencies()
	{
		if (this.readOnly) return;

		if (!this.startEdit(true)) return;

		setStatusMessage(ResourceMgr.getString("MsgCalcDependencies"));
		final Component c = this;
		WbSwingUtilities.showWaitCursor(this);

		// As checking all dependent tables can potentially
		// take some time (especially with Oracle...)
		// I'm starting a new thread to do the work
		// And I cannot re-use the deleteRow() function as
		// that wouldn't allow me to change the status message
		// to indicate the dependency checking is running
		// startEdit() is also changing the status message
		// and would be executed after a local change here
		// if I reused deleteRow()

		Thread t = new WbThread("DeleteDependency")
		{
			@Override
			public void run()
			{
				try
				{
					dbConnection.setBusy(true);
					dataTable.deleteRow(true);
					rowCountChanged();
				}
				catch (SQLException e)
				{
					LogMgr.logError("DwPanel.deleteRow()", "Error deleting row from table", e);
					WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
				}
				finally
				{
					dbConnection.setBusy(false);
					clearStatusMessage();
					WbSwingUtilities.showDefaultCursor(c);
				}
			}
		};
		t.start();
	}

	private boolean isLastRowSelected()
	{
		int[] rows = dataTable.getSelectedRows();
		if (rows == null || rows.length == 0) return false;
		return rows[rows.length - 1] == dataTable.getRowCount() - 1;
	}


	@Override
	public void deleteRow()
	{
		if (this.readOnly) return;
		if (!this.startEdit(true)) return;

		try
		{
			boolean needClear = isLastRowSelected();

			dataTable.deleteRow(false);
			rowCountChanged();
			if (needClear)
			{
				// for some strange reason all rows are selected when
				// the last row is selected and deleted
				// something must try to restore the selection
				// but as that row does not longer exist, it results in
				// selecting all rows...
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						dataTable.clearSelection();
					}
				});
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DwPanel.deleteRow()", "Error deleting row from table", e);
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
		}
	}

	@Override
	public int addRow()
	{
		if (this.readOnly) return -1;
		if (!this.startEdit()) return -1;

		int newRow = this.dataTable.addRow();
		if (newRow > -1) this.rowCountChanged();
		return newRow;
	}

	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	@Override
	public void cancelExecution()
	{
		if (this.stmtRunner != null)
		{
			this.stmtRunner.cancel();
		}
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
		this.dataTable.showInputFormAction();
		this.dataTable.setRowResizeAllowed(GuiSettings.getAllowRowHeightResizing());
		if (status != null)
		{
			statusBar = status;
			sharedStatusBar = true;
		}
		else
		{
			statusBar = new DwStatusBar(true, false);
			sharedStatusBar = false;
			add(this.statusBar, BorderLayout.SOUTH);
		}

		this.statusBar.setFocusable(false);
		this.setFocusable(false);
		this.scrollPane = new WbScrollPane(this.dataTable);
		this.scrollPane.getViewport().addChangeListener(this);

		this.add(this.scrollPane, BorderLayout.CENTER);
		this.dataTable.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.dataTable.setAdjustToColumnLabel(true);
	}

	public int getVerticalScrollBarWidth()
	{
		if (this.scrollPane == null) return 0;
		JScrollBar scrollbar = this.scrollPane.getVerticalScrollBar();
		if (scrollbar != null)
		{
			return scrollbar.getWidth();
		}
		return 0;
	}
	public void hideSQLInfo()
	{
		if (sqlInfo != null)
		{
			this.remove(sqlInfo);
			sqlInfo = null;
		}
	}

	public void showSQLInfo()
	{
		if (sqlInfo == null)
		{
			this.sqlInfo = new JLabel("");
			Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1,1,1,1));
			b = new CompoundBorder(new EmptyBorder(2,1,5,1), b);
			this.sqlInfo.setBorder(b);
			this.add(sqlInfo, BorderLayout.NORTH);
		}
		sqlInfo.setText(SqlUtil.makeCleanSql(sql == null ? "" : sql, false, false));
	}

	private void updateSqlInfoTooltip()
	{
		JTabbedPane tab = getTabParent();
		int index = getTabIndex(tab);
		if (index == -1) return;

		String tip = tab.getToolTipTextAt(index);
		if (sqlInfo != null)
		{
			sqlInfo.setToolTipText(tip);
		}
	}

	public void updateStatusBar()
	{
		if (this.isVisible())
		{
			rowCountChanged();
			if (GuiSettings.getShowSelectionSummary() && this.statusBar != null)
			{
				statusBar.showSelectionIndicator(this.dataTable);
			}
		}
	}

	@Override
	public void doRepaint()
	{
		statusBar.doRepaint();
	}

	@Override
	public String getText()
	{
		return statusBar.getText();
	}

	/**
	 *	Show a message in the status panel.
	 *
	 *  @see DwStatusBar#setStatusMessage(String)
	 */
	@Override
	public void setStatusMessage(final String aMsg)
	{
		this.statusBar.setStatusMessage(aMsg);
	}

	@Override
	public void setStatusMessage(final String msg, final int duration)
	{
		statusBar.setStatusMessage(msg, duration);
	}

	/**
	 *	Clears the display on the status bar.
	 *
	 *  @see DwStatusBar#clearStatusMessage()
	 */
	@Override
	public void clearStatusMessage()
	{
		statusBar.clearStatusMessage();
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
			@Override
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
		dataTable.reset();
		hasResultSet = false;
		sql = null;
		selectKeys.setEnabled(false);

		clearWarningIcon();
		statusBar.removeSelectionIndicator(dataTable);
		lastMessage = null;

		if (!sharedStatusBar)
		{
			statusBar.clearRowcount();
			statusBar.clearExecutionTime();
		}
		checkResultSetActions();
	}

	@Override
	public int getActionOnError(int errorRow, String errorColumn, String data, String errorMessage)
	{
		String msg = ResourceMgr.getFormattedString("ErrUpdateSqlError", NumberStringCache.getNumberString(errorRow),
			StringUtil.getMaxSubstring(data, 50), errorMessage);

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
	@Override
	public void endEdit()
	{
		if (!this.isEditingStarted()) return;

		dataTable.stopEditing();
		dataTable.setShowStatusColumn(false);
		checkResultSetActions();
		updateAction.setEnabled(GuiSettings.getAlwaysEnableSaveButton());
		dataTable.restoreOriginalValues();

		TableRowHeader header = TableRowHeader.getRowHeader(dataTable);
		if (header != null)
		{
			header.rowHeightChanged();
		}
	}

	@Override
	public boolean startEdit()
	{
		return startEdit(true);
	}

	public boolean isEditingStarted()
	{
		return this.dataTable.getShowStatusColumn();
	}

	/**
	 *	Starts the "edit" mode of the table.
	 *
	 * It will not start the edit mode, if the table is "read only"
	 * meaning if no update table (=database table) is defined.
	 *
	 * The following actions are carried out:
	 * <ul>
	 *  <li>if the updateable flag is not yet set, try to find out which table to update</li>
	 *  <li>the status column is displayec</li>
	 *  <li>the corresponding actions (insert row, delete row) are enabled</li>
	 * <li>the startEdit action is turned to "switched on"</li>
	 * </ul>
	 * @param restoreSelection if true the selected rows before starting the edit mode are restored
	 */
	public boolean startEdit(boolean restoreSelection)
	{
		if (this.readOnly) return false;


		int[] selectedRows = this.dataTable.getSelectedRows();
		int currentRow = this.dataTable.getEditingRow();

		// The offset is necessary because if the status column is shown,
		// the current editing column will change (but only if the status column is not already shown)
		int offset = dataTable.getShowStatusColumn() ? 0 : 1;
		int currentColumn = this.dataTable.getEditingColumn() + offset;

		Window w = SwingUtilities.getWindowAncestor(this);

		// if the result is not yet updateable (automagically)
		// then try to find the table. If the table cannot be
		// determined, then ask the user
		TableCheck checkResult = this.checkUpdateTable();
		if (checkResult == TableCheck.cancel) return false;

		boolean updateable = this.isUpdateable();

		if (!updateable && checkResult == TableCheck.noTable)
		{
			// checkUpdateTable() will have taken every attempt to find an update table
			// including asking the user to select a table from a multi-table result set

			// So if we wind up here, there is no way to update the underlying DataStore
			WbSwingUtilities.showErrorMessageKey(w, "MsgNoTables");
			this.setUpdateTable(null);
			this.disableUpdateActions();
			this.selectKeys.setEnabled(false);
			return false;
		}

		if (updateable)
		{
			// If the column order has been changed by the user using
			// drag and drop, we need to restore that ordering after
			// turning on the status column
			List<String> colOrder = null;
			if (dataTable.isColumnOrderChanged())
			{
				colOrder = ColumnOrderMgr.getInstance().getColumnOrder(dataTable);
			}

			this.dataTable.setShowStatusColumn(true);

			if (colOrder != null)
			{
				// The saved order will not have the status column in the list,
				// thus the status column would go to the end of the columns
				// but we need it as the first column
				String status = dataTable.getColumnModel().getColumn(0).getIdentifier().toString();
				colOrder.add(0, status);
			}

			// When changing the table model (which is happening
			// when the status column is displayed) we need to restore
			// the current editing column/row
			if (restoreSelection)
			{
				if (currentRow > -1 && currentColumn > -1)
				{
					dataTable.selectCell(currentRow, currentColumn);
					dataTable.setColumnSelectionAllowed(false);
				}
				else if (currentRow > -1)
				{
					dataTable.scrollToRow(currentRow);
					dataTable.setRowSelectionInterval(currentRow, currentRow);
				}
				else if (selectedRows != null)
				{
					ListSelectionModel model = dataTable.getSelectionModel();
					model.setValueIsAdjusting(true);
					// make sure nothing is selected, then restore the old selection
					model.clearSelection();
					for (int i = 0; i < selectedRows.length; i++)
					{
						model.addSelectionInterval(selectedRows[i], selectedRows[i]);
					}
					model.setValueIsAdjusting(false);
				}

				if (colOrder != null)
				{
					ColumnOrderMgr.getInstance().applyColumnOrder(dataTable, colOrder, true);
				}
				dataTable.requestFocusInWindow();
			}

			WbSwingUtilities.invoke(new Runnable()
			{
				@Override
				public void run()
				{
					checkResultSetActions();
				}
			});
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

		return updateable;
	}

	public InsertRowAction getInsertRowAction()
	{
		return this.insertRow;
	}

	public CopyRowAction getCopyRowAction()
	{
		return this.duplicateRow;
	}

	public DeleteRowAction getDeleteRowAction()
	{
		return this.deleteRow;
	}

	public DeleteDependentRowsAction getDeleteDependentRowsAction()
	{
		return this.deleteDependentRow;
	}

	public UpdateDatabaseAction getUpdateDatabaseAction()
	{
		return this.updateAction;
	}

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
	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (this.batchUpdate) return;
		if (this.readOnly) return;

		final boolean editing = isEditingStarted();
		boolean modified = this.isModified();
		int firstRow = e.getFirstRow();
		boolean structureChange = (firstRow == TableModelEvent.ALL_COLUMNS || firstRow == TableModelEvent.HEADER_ROW);

		if (modified && !structureChange)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (!editing) startEdit();
					checkResultSetActions();
				}
			});
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(WbConnection.PROP_READONLY) && evt.getSource() == this.dbConnection)
		{
			setReadOnly(this.dbConnection.isSessionReadOnly());
		}
		if (evt.getPropertyName().equals(GuiSettings.PROPERTY_SHOW_RESULT_SQL))
		{
			if (GuiSettings.getShowResultSQL())
			{
				showSQLInfo();
				updateSqlInfoTooltip();
			}
			else
			{
				hideSQLInfo();
			}
		}
	}

	/**
	 *	This is called when the selection in the table changes.
	 *  The copy row action will be enabled when exactly one row
	 *  is selected
	 * @see #disableUpdateActions()
	 * @see #checkResultSetActions()
	 */
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (this.readOnly)
		{
			disableUpdateActions();
		}
		else
		{
			checkResultSetActions();
		}
	}

	/**
	 *	Called from the viewport, when the display has been scrolled
	 *  We need to update the row display then.
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		this.rowCountChanged();
	}

	@Override
	public void fatalError(String msg)
	{
		WbSwingUtilities.showFriendlyErrorMessage(this, msg);
	}

}
