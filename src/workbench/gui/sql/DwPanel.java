package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.OneLineTableModel;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.util.LineTokenizer;
import workbench.util.SqlUtil;


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
	private static final ArrayList knownSqlVerbs;
	private static final ArrayList noResultVerbs;
	private static final ArrayList noUpdateCountVerbs;
	static
	{
		knownSqlVerbs = new ArrayList();
		knownSqlVerbs.add("SELECT");
		knownSqlVerbs.add("UPDATE");
		knownSqlVerbs.add("INSERT");
		knownSqlVerbs.add("DELETE");
		knownSqlVerbs.add("CREATE");
		knownSqlVerbs.add("DROP");
		knownSqlVerbs.add("ALTER");
		knownSqlVerbs.add("GRANT");
		knownSqlVerbs.add("COMMIT");
		knownSqlVerbs.add("ROLLBACK");
		
		noResultVerbs = new ArrayList();
		noResultVerbs.add("COMMIT");
		noResultVerbs.add("ROLLBACK");
		noResultVerbs.add("UPDATE");
		noResultVerbs.add("INSERT");
		noResultVerbs.add("DELETE");

		noUpdateCountVerbs = new ArrayList();
		noUpdateCountVerbs.add("COMMIT");
		noUpdateCountVerbs.add("ROLLBACK");
		noUpdateCountVerbs.add("DROP");
		noUpdateCountVerbs.add("CREATE");
		noUpdateCountVerbs.add("ALTER");
		noUpdateCountVerbs.add("GRANT");
	}
	
	
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
				Dimension max = new Dimension(400,400);
				JTextArea preview = new JTextArea();
				preview.setLineWrap(false);
				preview.setColumns(60);
				preview.setPreferredSize(null);
				preview.setMaximumSize(max);
				JScrollPane scroll = new JScrollPane(preview);
				scroll.setMaximumSize(max);
				List stmts = ds.getUpdateStatements(aConnection);
				for (int i=0; i < stmts.size(); i++)
				{
					DmlStatement dml = (DmlStatement)stmts.get(i);
					preview.append(dml.getExecutableStatement(aConnection));
					preview.append(";\n");
				}
				if (stmts.size() > 20)
				{
					scroll.setPreferredSize(max);
				}
				Window win = SwingUtilities.getWindowAncestor(this);
				int choice = JOptionPane.showConfirmDialog(win, scroll, "Please confirm updates", JOptionPane.OK_CANCEL_OPTION);
				if (choice == JOptionPane.CANCEL_OPTION) return 0;
			}
			long start, end;
			start = System.currentTimeMillis();
			rows = ds.updateDb(aConnection);
			end = System.currentTimeMillis();
			long sqlTime = (end - start);
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

	public void setMaxRows(int aRowCount)
	{
		if (aRowCount <= 0) this.maxRows = 0;
		else this.maxRows = aRowCount;
	}
	public int getMaxRows(){ return this.maxRows; }
	
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
		
		try
		{
			long start, end, sqlTime = 0;
			String cleanSql = null;
			StringBuffer msg = new StringBuffer(500);
			
			this.lastMessage = "";
			
			if (aSql.endsWith(";"))
				aSql = aSql.substring(0, aSql.length() - 1);
				
			boolean repeatLast = false;
			repeatLast = aSql.equals(this.sql);
			
			// the cleanSql is necessary to strip the 
			// statement from all comments so that the actual SQL verb 
			// can be identified
			cleanSql = SqlUtil.makeCleanSql(aSql, false);
			this.sql = null;
			
			String verb = SqlUtil.getSqlVerb(cleanSql).toUpperCase();
			Connection sqlcon = aConnection.getSqlConnection();
			ResultSet rs = null;
			List keepColumns = null;
			DataStore newData = null;
		
			this.clearContent();
			this.hasResultSet = false;
			this.setMaxRows(this.statusBar.getMaxRows());
			StringBuffer rows = null;
			
			if (verb.equalsIgnoreCase("DESC"))
			{
				LineTokenizer tok = new LineTokenizer(aSql, " ");
				tok.nextToken();
				String table = tok.nextToken();
				String schema = null;
				int pos = table.indexOf('.');
				if (pos > -1)
				{
					schema = table.substring(0, pos);
					table = table.substring(pos + 1);
				}
				this.hasResultSet = true;
				newData = aConnection.getMetadata().getTableDefinition(null, schema, table);
			}
			else if (verb.equalsIgnoreCase("LIST"))
			{
				this.hasResultSet = true;
				newData = aConnection.getMetadata().getTables();
			}
			else if (verb.equalsIgnoreCase("LISTPROCS"))
			{
				newData = aConnection.getMetadata().getProcedures(null, null);
				this.hasResultSet = true;
			}
			else if (verb.equalsIgnoreCase("LISTDB"))
			{
				newData = aConnection.getMetadata().getCatalogInformation();
				this.hasResultSet = true;
			}
			else if (verb.equalsIgnoreCase("ENABLEOUT"))
			{
				StringTokenizer tok = new StringTokenizer(aSql, " ");
				long limit = -1;
				tok.nextToken(); // skip the verb
				if (tok.hasMoreTokens())
				{
					String value = tok.nextToken();
					try
					{
						limit = Long.parseLong(value);
					}
					catch (NumberFormatException nfe)
					{
						limit = -1;
					}
				}
				this.dbConnection.getMetadata().enableOutput(limit);
				this.hasResultSet = false;
				this.lastStatement = null;
			}
			else if (verb.equalsIgnoreCase("DISABLEOUT"))
			{
				this.dbConnection.getMetadata().disableOutput();
				this.hasResultSet = false;
				this.lastStatement = null;
			}
			else
			{
				boolean checkForResultSet = false;
				
				this.lastStatement = sqlcon.createStatement();

				if (verb.equalsIgnoreCase("SELECT"))
				{
					this.lastStatement.setMaxRows(this.maxRows);
				}
				else
				{
					this.lastStatement.setMaxRows(0);
				}					
				start = System.currentTimeMillis();
				
				boolean hasResult = this.lastStatement.execute(aSql);
				int updateCount = -1;
				
				if (hasResult) 
				{
					rs = this.lastStatement.getResultSet();
				}
				else
				{
					updateCount = this.lastStatement.getUpdateCount();
					if (updateCount > -1 && !this.noUpdateCountVerbs.contains(verb))
					{
						rows = new StringBuffer(100);
						rows.append(updateCount + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
						rows.append('\n');
					}
				}
				
				boolean moreResults = false; 
				
				if (!noResultVerbs.contains(verb))
				{
					if (rs == null)
					{
						moreResults = this.lastStatement.getMoreResults();
					}

					while (moreResults || (updateCount != -1))
					{
						if (moreResults)
						{
							rs  = this.lastStatement.getResultSet();
						}

						if (updateCount > -1 && !this.noUpdateCountVerbs.contains(verb))
						{
							if (rows == null) rows = new StringBuffer(100);
							rows.append(updateCount + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
							rows.append('\n');
						}

						moreResults = this.lastStatement.getMoreResults();
						updateCount = this.lastStatement.getUpdateCount();
					}			
				}
				
				if (rs != null)
				{
					this.hasResultSet = true;
					newData = new DataStore(rs, this.dbConnection);
					rs.close();
				}
				else
				{
					this.hasResultSet = false;
				}
				end = System.currentTimeMillis();
				sqlTime = (end - start);
			}

			
			if (this.hasResultSet)
			{
				this.sql = aSql;
				if (repeatLast)
				{
					this.infoTable.saveColumnSizes();
				}
				newData.setOriginalStatement(aSql);
				newData.setSourceConnection(this.dbConnection);
				newData.checkUpdateTable();
				
				this.infoTable.reset();
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(new DataStoreTableModel(newData), true);

				/*
				if (repeatLast)
				{
					this.infoTable.restoreColumnSizes();
				}
				else
				{
					this.infoTable.adjustColumns();
				}
				*/
				this.infoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.infoTable.setRowSelectionAllowed(true);
				this.statusBar.setRowcount(this.infoTable.getRowCount());
			}
			else if (this.lastStatement != null)
			{
				this.setMessageDisplayModel(this.getEmptyMsgTableModel());
				SQLWarning warn = this.lastStatement.getWarnings();
				boolean warnings = warn != null;
				while (warn != null)
				{
					msg.append('\n');
					msg.append(warn.getMessage());
					warn = warn.getNextWarning();
				}
				if (warnings) msg.append("\n\n");
				String outMsg = this.lastConnection.getOutputMessages();
				if (outMsg.length() > 0)
				{
					msg.append(outMsg);
					msg.append("\n\n");
				}
			}
			msg.append(verb.toUpperCase());
			msg.append(' ');
			msg.append(ResourceMgr.getString("MsgKnownStatementOK"));
			msg.append('\n');
			
			if (rows != null) msg.append(rows);
			
			this.lastMessage = msg.toString();
			this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
			if (this.lastStatement != null) this.lastStatement.close();
			this.lastStatement = null;
		}
		catch (SQLException sql)
		{
			this.setMessageDisplayModel(this.getErrorTableModel());
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			this.lastMessage = this.lastMessage + ExceptionUtil.getDisplay(sql);
			throw sql;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			LogMgr.logError(this, "Error executing statement: \r\n" + this.sql, e);
			this.setMessageDisplayModel(this.getErrorTableModel());
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			String s = ExceptionUtil.getDisplay(e);
			this.lastMessage = this.lastMessage + s;
			throw new WbException(s);
		}
		
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
		this.lastMessage = null;
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

	public WbTable getTable() { return this.infoTable; }
	
}
