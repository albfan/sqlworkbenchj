package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.StartEditAction;
import workbench.gui.actions.UpdateDatabaseAction;


import workbench.gui.components.*;
import workbench.interfaces.DbData;
import workbench.interfaces.DbUpdater;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;



/**
 *	A Panel which displays the result of a SELECT statement.
 */
public class DwPanel 
	extends JPanel
	implements TableModelListener, ListSelectionListener, RowActionMonitor, DbData, DbUpdater
{
	private WbTable infoTable;
	private DwStatusBar statusBar;
	
	private String sql;
	private String lastMessage;
	private WbConnection dbConnection;
	private TableModel errorModel;
	private TableModel resultEmptyMsgModel;
	private boolean hasResultSet = false;
	
	private WbScrollPane scrollPane;
	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private int maxRows = 0;
	
	private WbConnection lastConnection;
	private boolean cancelled;
	private boolean success = false;
	private boolean showLoadProgress = false;

	private UpdateDatabaseAction updateAction = null;
	private InsertRowAction insertRow = null;
	private DeleteRowAction deleteRow = null;
	private StartEditAction startEdit = null;
	private boolean editingStarted = false;
	private boolean batchUpdate = false;
	private boolean manageUpdateAction = false;
	private boolean readOnly = false;
	
	private StatementRunner stmtRunner;

	public DwPanel()
	{
		JTextField stringField = new JTextField();
		stringField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		stringField.addMouseListener(new TextComponentMouseListener());
		this.defaultEditor = new DefaultCellEditor(stringField);
		
		JTextField numberField = new JTextField();
		numberField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		numberField.addMouseListener(new TextComponentMouseListener());
		
		this.defaultNumberEditor = new DefaultCellEditor(numberField);
		this.initLayout();
		
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(infoTable);
		pol.addComponent(infoTable);
		pol.addComponent(statusBar.tfMaxRows);
		this.setFocusTraversalPolicy(pol);
		this.setDoubleBuffered(true);
		this.stmtRunner = new StatementRunner();
		this.infoTable.addTableModelListener(this);	
		
		this.updateAction = new UpdateDatabaseAction(this);
		this.insertRow = new InsertRowAction(this);
		this.deleteRow = new DeleteRowAction(this);
		this.startEdit = new StartEditAction(this);
		
		//infoTable.addPopupAction(this.startEdit, true);
		infoTable.addPopupAction(this.updateAction, true);
		infoTable.addPopupAction(this.insertRow, true);
		infoTable.addPopupAction(this.deleteRow, false);
		
	}

	public void setManageActions(boolean aFlag)
	{
		this.manageUpdateAction = aFlag;
		if (aFlag)
		{
			infoTable.getSelectionModel().addListSelectionListener(this);
		}
		else
		{
			infoTable.getSelectionModel().removeListSelectionListener(this);
		}
	}
	
	public void setDefaultStatusMessage(String aMessage)
	{
		this.statusBar.setReadyMsg(aMessage);
	}
	
	public void disconnect()
	{
		try
		{
			this.setConnection(null);
		}
		catch (Exception e)
		{
		}
	}

	public void setShowLoadProcess(boolean aFlag)
	{
		this.showLoadProgress = aFlag;
	}
	
	private String updateMsg;
	private int monitorType = -1;
	
	private void clearRowMonitorSettings()
	{
		this.updateMsg = null;
		this.monitorType = -1;
	}

	public void setMonitorType(int aType) 
	{ 
		this.monitorType = aType; 
		try
		{
			switch (aType)
			{
				case RowActionMonitor.MONITOR_INSERT:
					this.updateMsg = ResourceMgr.getString("MsgImportingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_UPDATE:
					this.updateMsg = ResourceMgr.getString("MsgUpdatingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_LOAD:
					this.updateMsg = ResourceMgr.getString("MsgLoadingRow") + " ";
					break;
				default:
					clearRowMonitorSettings();
			}
		}
		catch (Exception e)
		{
			clearRowMonitorSettings();
		}
	}
	
	public void setCurrentRow(int currentRow, int totalRows)
	{
		if (this.monitorType < 0) return;
		StringBuffer msg = new StringBuffer(40);
		msg.append(this.updateMsg);
		msg.append(currentRow);
		if (totalRows > 0)
		{
			msg.append('/');
			msg.append(totalRows);
		}
		this.statusBar.setStatusMessage(msg.toString());
	}
	
	
	/**
	 *	Defines the connection for this DBPanel.
	 *	@see setSqlStatement(String)
	 */
	public void setConnection(WbConnection aConn)
		throws SQLException, WbException
	{
		this.clearContent();
		this.sql = null;
		//this.lastStatement = null;
		this.lastMessage = null;
		this.dbConnection = aConn;
		this.hasResultSet = false;
		this.clearStatusMessage();
		this.stmtRunner.setConnection(aConn);
	}

	public void setUpdateDelegate(DbUpdater aDelegate)
	{
		this.updateAction.setClient(aDelegate);
	}
	
	public synchronized void saveChangesToDatabase()
	{
		if (this.dbConnection == null) return;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			this.saveChanges(dbConnection);
		}
		catch (Exception e)
		{
			String msg = ResourceMgr.getString("ErrorUpdatingDb");
			WbManager.getInstance().showErrorMessage(this, msg + "\n" + e.getMessage());
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}
	
	public synchronized int saveChanges(WbConnection aConnection)
		throws WbException, SQLException
	{
		int rows = 0;
		
		this.infoTable.stopEditing();
		
		try
		{
			DataStore ds = this.infoTable.getDataStore();
			if (WbManager.getSettings().getDbDebugMode())
			{
				WbSwingUtilities.showWaitCursor(this);
				Window win = SwingUtilities.getWindowAncestor(this);
				try
				{
					Dimension max = new Dimension(800,600);
					Dimension pref = new Dimension(400, 300);
					EditorPanel preview = EditorPanel.createSqlEditor();
					preview.setBorder(WbSwingUtilities.EMPTY_BORDER);
					preview.setPreferredSize(pref);
					preview.setMaximumSize(max);
					JScrollPane scroll = new JScrollPane(preview);
					scroll.setMaximumSize(max);
					List stmts = ds.getUpdateStatements(aConnection);
					StringBuffer text = new StringBuffer(stmts.size() * 80);
					for (int i=0; i < stmts.size(); i++)
					{
						DmlStatement dml = (DmlStatement)stmts.get(i);
						text.append(dml.getExecutableStatement(aConnection.getDatabaseProductName()));
						text.append(";\n");
					}
					preview.setText(text.toString());
					preview.setCaretPosition(0);
					WbSwingUtilities.showDefaultCursor(this);
					int choice = JOptionPane.showConfirmDialog(win, scroll, ResourceMgr.getString("MsgConfirmUpdates"), JOptionPane.OK_CANCEL_OPTION);
					if (choice == JOptionPane.CANCEL_OPTION) return 0;
				}
				catch (OutOfMemoryError mem)
				{
					String msg = ResourceMgr.getString("MsgOutOfMemorySQLPreview");
					int choice = JOptionPane.showConfirmDialog(win, msg, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.NO_OPTION) return 0;
				}
				finally
				{
					WbSwingUtilities.showDefaultCursorOnWindow(this);
				}
			}
			long start, end;
      WbSwingUtilities.showWaitCursor(this);
			ds.setProgressMonitor(this);
			start = System.currentTimeMillis();
			rows = ds.updateDb(aConnection);
			end = System.currentTimeMillis();
			ds.setProgressMonitor(null);
			long sqlTime = (end - start);
			this.lastMessage = ResourceMgr.getString("MsgUpdateSuccessfull");
			this.lastMessage = this.lastMessage + "\n" + rows + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED);
			this.lastMessage = this.lastMessage + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
		}
		catch (SQLException e)
		{
			this.lastMessage = ExceptionUtil.getDisplay(e);
			throw e;
		}
		catch (OutOfMemoryError mem)
		{
			WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgOutOfMemoryError"));
		}
		catch (Throwable th)
		{
			LogMgr.logError("DwPanel.saveChanges()", "Error when saving changes to the database", th);
		}
		finally
		{
      WbSwingUtilities.showDefaultCursor(this);
			this.clearStatusMessage();
			this.endEdit();
		}
		this.repaint();
		
		return rows;
	}
	
	public String getCurrentSql()
	{
		return this.sql;
	}

	public void setUpdateTable(String aTable)
	{
		this.infoTable.getDataStore().setUpdateTable(aTable);
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
			this.restoreOriginalValues();
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
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return false;
		if (this.dbConnection == null) return false;
		if (this.sql == null) return false;
		return ds.checkUpdateTable(this.sql, this.dbConnection);
	}
	
	public boolean isUpdateable()
	{
		if (this.infoTable.getDataStore() == null) return false;
		return this.infoTable.getDataStore().isUpdateable();
	}

	public boolean hasUpdateableColumns()
	{
		if (this.infoTable.getDataStore() == null) return false;
		return this.infoTable.getDataStore().hasUpdateableColumns();
	}

	public int getMaxRows()
	{
		return this.statusBar.getMaxRows();
	}
	public void setMaxRows(int aMax)
	{
		this.statusBar.setMaxRows(aMax);
	}
	
	public void runStatement(String aSql)
		throws SQLException, WbException
	{
		this.runStatement(aSql, this.dbConnection);
	}

	/**
	 *	Execute the given SQL statement.
	 */
	public void runStatement(String aSql, WbConnection aConnection)
		throws SQLException, WbException
	{
		if (aSql == null || aConnection == null || aConnection.isClosed())
		{
			LogMgr.logInfo(this, "No connection given or connection closed!");
			return;
		}
		
		this.lastConnection = aConnection;
		this.success = false;
		StatementRunnerResult result = null;
		
		try
		{
			long end, sqlTime = 0;
			long execTime = 0;
			
			this.clearContent();
			
			boolean repeatLast = aSql.equals(this.sql);
			this.sql = aSql;
		
			final long start = System.currentTimeMillis();
			
			this.stmtRunner.runStatement(aSql, this.statusBar.getMaxRows());
			end = System.currentTimeMillis();
			sqlTime = (end - start);
			
			result = this.stmtRunner.getResult();
			this.hasResultSet = false;
			DataStore newData = null;
			this.success = result.isSuccess();
			
			if (result.isSuccess() && result.hasData())
			{
				this.hasResultSet = true;
				
				if (result.hasDataStores())
				{
					newData = result.getDataStores()[0];
				}
				else
				{
					// the resultset will be closed when stmtRunner.done() is called
					// in the finally block
					ResultSet rs = result.getResultSets()[0];
					if (this.showLoadProgress) 
					{
						newData = new DataStore(rs, true, this);
					}
					else
					{
						newData = new DataStore(rs, true);
					}
					newData.setOriginalStatement(aSql);
					newData.setSourceConnection(this.dbConnection);
					newData.checkUpdateTable();
					newData.setProgressMonitor(null);
					if (this.showLoadProgress)
					{
						this.clearStatusMessage();
					}
				}
				end = System.currentTimeMillis();
				
				if (repeatLast)
				{
					this.infoTable.saveColumnSizes();
				}
				
				this.infoTable.reset();
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(new DataStoreTableModel(newData), true);

				if (repeatLast)
				{
					this.infoTable.restoreColumnSizes();
				}
				else
				{
					this.infoTable.adjustColumns();
				}
				this.infoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.infoTable.setRowSelectionAllowed(true);
				
				if (this.manageUpdateAction)
				{
					boolean update = this.isUpdateable();
					this.insertRow.setEnabled(true);
				}
        this.dataChanged();
			}
			else if (result.isSuccess())
			{
				end = System.currentTimeMillis();
				this.hasResultSet = false;
				this.setMessageDisplayModel(this.getEmptyMsgTableModel());
			}
			else 
			{
				end = System.currentTimeMillis();
				this.hasResultSet = false;
				this.setMessageDisplayModel(this.getErrorTableModel());
				this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			}
			execTime = (end - start);
			
			String[] messages = result.getMessages();
			StringBuffer msg = null;
			if (messages != null)
			{
				msg = new StringBuffer(messages.length * 80);
				for (int i=0; i < messages.length; i++)
				{
					msg.append(messages[i]);
					msg.append('\n');
				}
				this.lastMessage = msg.toString();
			}
			if (LogMgr.isDebug())
			{
				this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgSqlVerbTime") + " " + (((double)sqlTime) / 1000.0) + "s";
			}
			this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgExecTime") + " " + (((double)execTime) / 1000.0) + "s";
		}
		catch (SQLException sqle)
		{
			LogMgr.logError(this, "SQL error when executing statement: \r\n" + this.sql, sqle);
			this.setMessageDisplayModel(this.getErrorTableModel());
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			this.lastMessage = this.lastMessage + ExceptionUtil.getDisplay(sqle);
			throw sqle;
		}
		catch (Throwable e)
		{
			if (e instanceof OutOfMemoryError)
			{
				WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgOutOfMemoryError"));
			}
			LogMgr.logError(this, "Error executing statement: \r\n" + this.sql, e);
			this.setMessageDisplayModel(this.getErrorTableModel());
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			String s = ExceptionUtil.getDisplay(e);
			this.lastMessage = this.lastMessage + s;
			throw new WbException(s);
		}
		finally
		{
			result = null;
		}
  }

	public void scriptStarting()
	{
	}
	
	public void scriptFinished()
	{
		this.stmtRunner.done();
	}
	
	public boolean wasSuccessful()
	{
		return this.success;
	}
	
  public void dataChanged()
  {
		this.statusBar.setRowcount(this.infoTable.getRowCount());
  }
  
	public void deleteRow()
	{
		if (this.readOnly) return;
		
		DataStoreTableModel ds = this.infoTable.getDataStoreTableModel();
		if (ds == null) return;
		
		int selectedRow = this.infoTable.getSelectedRow();
		if (selectedRow != -1)
		{
			ds.deleteRow(selectedRow);
			if (selectedRow >= ds.getRowCount())
			{
				selectedRow --;
			}
			this.infoTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
		}
    this.dataChanged();
	}
	
	public long addRow()
	{
		if (this.readOnly) return -1;
		DataStoreTableModel ds = this.infoTable.getDataStoreTableModel();
		if (ds == null) return -1;
		
		int selectedRow = this.infoTable.getSelectedRow();
		final int newRow;
		
		this.infoTable.stopEditing();
		
		if (selectedRow == -1)
		{
			newRow = ds.addRow();
		}
		else
		{
			newRow = ds.insertRow(selectedRow);
		}
		this.infoTable.getSelectionModel().setSelectionInterval(newRow, newRow);
		this.infoTable.scrollToRow(newRow);
		infoTable.grabFocus();
		infoTable.setEditingRow(newRow);
		infoTable.setEditingColumn(1);
		infoTable.editCellAt(newRow, 1);
		Component edit = infoTable.getEditorComponent();
		if (edit != null)
		{
			edit.requestFocus();
		}
    this.dataChanged();
		return newRow;
	}

	public boolean cancelExecution()
	{
		if (this.stmtRunner != null)
		{
			this.stmtRunner.cancel();
		}
		return false;
	}

	public void restoreOriginalValues()
	{
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return;
		ds.restoreOriginalValues();
		this.repaint();
	}
	
	public String getLastMessage() { return this.lastMessage; }
	public boolean hasResultSet() { return this.hasResultSet; }

	public boolean isModified() 
	{ 
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return false;
		else return ds.isModified();
	}

	private void initLayout()
	{
		this.setLayout(new BorderLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.infoTable = new WbTable();
		this.infoTable.setRowResizeAllowed(true);
		this.statusBar = new DwStatusBar();
		this.statusBar.setFocusable(false);
		this.setFocusable(false);
		this.scrollPane = new WbScrollPane(this.infoTable);
		this.add(this.scrollPane, BorderLayout.CENTER);
		this.add(this.statusBar, BorderLayout.SOUTH);
		//this.infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.infoTable.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.infoTable.setDoubleBuffered(true);
		this.infoTable.setAdjustToColumnLabel(true);
	}
	
	public void setStatusMessage(String aMsg)
	{
		this.statusBar.setStatusMessage(aMsg);
	}
	
	public void clearStatusMessage()
	{
		this.statusBar.clearStatusMessage();
	}
	
	private void setMessageDisplayModel(TableModel aModel)
	{
		this.infoTable.setModel(aModel);
		TableColumnModel colMod = this.infoTable.getColumnModel();
		TableColumn col = colMod.getColumn(0);
		col.setPreferredWidth(this.getWidth() - 10);
	}

	public void clearContent()
	{
		this.infoTable.reset();
		this.endEdit();
		this.hasResultSet = false;
		this.cancelled = false;
		this.lastMessage = StringUtil.EMPTY_STRING;
		this.sql = null;
		this.statusBar.clearRowcount();
	}
	
	private TableModel getEmptyMsgTableModel()
	{
		if (this.resultEmptyMsgModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.MSG_WARN_NO_RESULT);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.resultEmptyMsgModel = new OneLineTableModel(title, msg);
		}
		return this.resultEmptyMsgModel;
	}
	
	private TableModel getErrorTableModel()
	{
		if (this.errorModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_DATA);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.errorModel = new OneLineTableModel(title, msg);
		}
		return this.errorModel;
	}

  public void selectMaxRowsField()
  {
    this.statusBar.selectMaxRowsField();
  }

	public WbTable getTable() { return this.infoTable; }

	private void doRepaint()
	{
		this.paint(this.getGraphics());
	}

	public void endEdit()
	{
		this.editingStarted = false;
		this.infoTable.setShowStatusColumn(false);
		this.updateAction.setEnabled(false);
		this.insertRow.setEnabled(false);
		this.deleteRow.setEnabled(false);
		this.startEdit.setSwitchedOn(false);
		this.restoreOriginalValues();
	}
	
	public boolean startEdit()
	{
		if (this.readOnly) return false;
		this.editingStarted = false;
		
		// if the result is not yet updateable (automagically)
		// then try to find the table. If the table cannot be
		// determined, then ask the user
		if (!this.isUpdateable())
		{
			if (!this.checkUpdateTable())
			{
				String sql = this.getCurrentSql();
				List tables = SqlUtil.getTables(sql);
				String table = null;

				if (tables.size() > 1)
				{
					table = (String)JOptionPane.showInputDialog(this,
							null, ResourceMgr.getString("MsgEnterUpdateTable"),
							JOptionPane.QUESTION_MESSAGE,
							null,tables.toArray(),null);
				}

				if (table != null)
				{
					this.setUpdateTable(table);
				}
			}
		}
		boolean update = this.isUpdateable();
		if (update)
		{
			this.infoTable.setShowStatusColumn(true);
			if (this.updateAction != null)
			{
				if (this.infoTable.getDataStore().isModified())
				{
					this.updateAction.setEnabled(true);
				}
			}

			this.editingStarted = true;
			this.startEdit.setSwitchedOn(true);
		}
		if (this.insertRow != null) this.insertRow.setEnabled(update);
		if (this.deleteRow != null) this.deleteRow.setEnabled(update);

		return update;
	}
	
	public InsertRowAction getInsertRowAction() { return this.insertRow; }
	public DeleteRowAction getDeleteRowAction() { return this.deleteRow; }
	public UpdateDatabaseAction getUpdateDatabaseAction() { return this.updateAction; }
	public StartEditAction getStartEditAction() { return this.startEdit; }

	public void setBatchUpdate(boolean aFlag)
	{
		this.batchUpdate = aFlag;
	}

	public void tableChanged(TableModelEvent e)
	{
		if (this.batchUpdate) return;
		if (this.readOnly) return;
		
		if (e.getFirstRow() != TableModelEvent.ALL_COLUMNS && this.isModified())
		{
			if (!this.editingStarted)
			{
				this.startEdit();
			}
			else
			{
				if (this.updateAction != null) this.updateAction.setEnabled(true);
			}
		}
	}
	
	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (this.readOnly) return;
		long rows = this.infoTable.getSelectedRowCount();
		this.deleteRow.setEnabled( (rows == 1) );
	}
	
}
