package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;


import workbench.gui.components.*;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.util.StringUtil;



/**
 *	A Panel which displays the result of a SELECT statement.
 */
public class DwPanel extends JPanel
{
	private WbTable infoTable;
	private DwStatusBar statusBar;
	
	private String sql;
	private String lastMessage;
	private WbConnection dbConnection;
	private TableModel errorModel;
	private TableModel resultEmptyMsgModel;
	private boolean hasResultSet = false;
	//private PreparedStatement prepStatement;
	private Statement lastStatement;
	
	private WbScrollPane scrollPane;
	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private int maxRows = 0;
	private int objectId;
	
	private static int nextId = 0;
	private WbConnection lastConnection;
	private boolean cancelled;
	private boolean success = false;
	private StatementRunner stmtRunner;
	
	public DwPanel()
	{
		this.objectId = nextId++;
		
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
	
	/**
	 *	Defines the connection for this DBPanel.
	 *	@see setSqlStatement(String)
	 */
	public void setConnection(WbConnection aConn)
		throws SQLException, WbException
	{
		this.clearContent();
		this.sql = null;
		this.lastStatement = null;
		this.lastMessage = null;
		this.dbConnection = aConn;
		this.hasResultSet = false;
		this.stmtRunner.setConnection(aConn);
	}

	public int saveChanges(WbConnection aConnection)
		throws WbException, SQLException
	{
		int rows = 0;
		
		this.infoTable.stopEditing();
		
		try
		{
			DataStore ds = this.infoTable.getDataStore();
			if (WbManager.getSettings().getDbDebugMode())
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
				Window win = SwingUtilities.getWindowAncestor(this);
				int choice = JOptionPane.showConfirmDialog(win, scroll, "Please confirm updates", JOptionPane.OK_CANCEL_OPTION);
				if (choice == JOptionPane.CANCEL_OPTION) return 0;
			}
			long start, end;
      WbSwingUtilities.showWaitCursorOnWindow(this);
			start = System.currentTimeMillis();
			rows = ds.updateDb(aConnection);
			end = System.currentTimeMillis();
			long sqlTime = (end - start);
      WbSwingUtilities.showDefaultCursorOnWindow(this);
			this.infoTable.repaint();
			this.lastMessage = ResourceMgr.getString("MsgUpdateSuccessfull");
			this.lastMessage = this.lastMessage + "\n" + rows + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED);
			this.lastMessage = this.lastMessage + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			this.lastMessage = ExceptionUtil.getDisplay(e);
			throw e;
		}
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
	
	public boolean checkUpdateTable()
	{
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
			long start, end, sqlTime = 0;
			this.clearContent();
			
			boolean repeatLast = aSql.equals(this.sql);
			this.sql = aSql;
		
			start = System.currentTimeMillis();
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
					newData = new DataStore(rs, true);
					newData.setOriginalStatement(aSql);
					newData.setSourceConnection(this.dbConnection);
					newData.checkUpdateTable();
				}
				
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
        this.dataChanged();
			}
			else if (result.isSuccess())
			{
				this.hasResultSet = false;
				this.setMessageDisplayModel(this.getEmptyMsgTableModel());
			}
			else 
			{
				this.hasResultSet = false;
				this.setMessageDisplayModel(this.getErrorTableModel());
				this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			}
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
			this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
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
	
	public void addRow()
	{
		DataStoreTableModel ds = this.infoTable.getDataStoreTableModel();
		if (ds == null) return;
		
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
	}

	public boolean cancelExecution()
	{
		if (this.lastStatement != null)
		{
			try
			{
				LogMgr.logDebug(this, "Trying to cancel the current statement...");
				this.lastStatement.cancel();
				this.lastStatement.close();
				this.cancelled = true;
				if (this.lastConnection != null && this.lastConnection.cancelNeedsReconnect())
				{
					LogMgr.logInfo(this, "Cancelling needs a reconnect to the database for this DBMS...");
					this.lastConnection.reconnect();
				}
				LogMgr.logDebug(this, "Cancelling succeeded.");
				return true;
			}
			catch (Exception e)
			{
				LogMgr.logWarning(this, "Error while cancelling SQL execution!", e);
				return false;
			}
		}
		else
		{
			return false;
		}
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
	
}
