package workbench.gui.sql;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ResultSetTableModel;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.DateColumnRenderer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;




/**
 *	A Panel which displays the result of a SELECT statement.
 *	A DBReader is used to read the results from the
 *	database and a DBReaderTableModel is used for the TableModel
 *	of the resulting JTable
 */
public class DwPanel extends JPanel
	implements TableModelListener
{
	private WbTable infoTable;
	private DwStatusBar statusBar;
	
	private ResultSetTableModel realModel;
	private String sql;
	private String lastMessage;
	private WbConnection dbConnection;
	private DefaultTableModel errorModel;
	private DefaultTableModel emptyModel;
	private int maxWidth = 250;
	private boolean hasResultSet = false;
	private PreparedStatement prepStatement;
	private JScrollPane scrollPane;
	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private int maxRows = 0;
	
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
		//this.setFocusCycleRoot(true);
	}
	
	public void setMaxColWidth(int aWidth) { this.maxWidth = aWidth; }
	public int getMaxColWidth() { return this.maxWidth; }

	/**
	 *	Defines the connection for this DBPanel.
	 *	If the statement is already defined this method
	 *	will initialize the complete panel and display
	 *	the result set of the statement.
	 *
	 *	@see setSqlStatement(String)
	 */
	public void setConnection(WbConnection aConn)
		throws SQLException, WbException
	{
		this.sql = null;
		this.prepStatement = null;
		this.lastMessage = null;
		this.clearContent();
		this.dbConnection = aConn;
	}
	
	public int saveChanges(WbConnection aConnection)
		throws WbException, SQLException
	{
		int rows = 0;
		try
		{
			DataStore ds = this.realModel.getDataStore();
			if (WbManager.getSettings().getDbDebugMode())
			{
				Dimension max = new Dimension(300,32768);
				JTextArea preview = new JTextArea();
				preview.setLineWrap(false);
				preview.setColumns(40);
				preview.setPreferredSize(null);
				preview.setMaximumSize(max);
				JScrollPane scroll = new JScrollPane(preview);
				scroll.setMaximumSize(max);
				List stmts = ds.getUpdateStatements(aConnection);
				for (int i=0; i < stmts.size(); i++)
				{
					preview.append(stmts.get(i).toString());
					preview.append(";\r\n");
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
			this.lastMessage = this.lastMessage + "\r\n" + rows + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED);
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

	public void setMaxRows(int aRowCount)
	{
		if (aRowCount <= 0) this.maxRows = 0;
		else this.maxRows = aRowCount;
	}
	public int getMaxRows(){ return this.maxRows; }
	
	public void setUpdateTable(String aTable)
	{
		this.realModel.setUpdateTable(aTable);
	}
	
	public boolean checkUpdateTable()
	{
		return this.realModel.getDataStore().checkUpdateTable(this.sql);
	}
	
	public boolean isUpdateable()
	{
		if (this.realModel == null) return false;
		if (this.realModel.getDataStore().hasUpdateableColumns())
			return this.realModel.isUpdateable();
		else
			return false;
	}

	public boolean hasUpdateableColumns()
	{
		if (this.realModel == null) return false;
		return this.realModel.getDataStore().hasUpdateableColumns();
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
		if (aSql == null || 
		    aConnection == null || 
				aConnection.isClosed())
		{
			LogMgr.logInfo(this, "No connection given or connection closed!");
			return;
		}
		this.sql = null;
		
		try
		{
			long start, end, sqlTime = 0;
			this.lastMessage = null;
			this.realModel = null;
			if (aSql.endsWith(";"))
			{
				aSql = aSql.substring(0, aSql.length() - 1);
			}
			String verb = SqlUtil.getSqlVerb(aSql);
			Connection sqlcon = aConnection.getSqlConnection();
			ResultSet rs = null;
			List keepColumns = null;
			
			this.statusBar.clearRowcount();
			this.setMaxRows(this.statusBar.getMaxRows());
			
			if (verb.equalsIgnoreCase("DESC"))
			{
				StringTokenizer tok = new StringTokenizer(aSql, " ");
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
				this.realModel = aConnection.getMetadata().getTableDefinitionModel(null, schema, table);
			}
			else if (verb.equalsIgnoreCase("LIST"))
			{
				this.hasResultSet = true;
				this.realModel = aConnection.getMetadata().getListOfTables();
			}
			else if (verb.equalsIgnoreCase("LISTPROCS"))
			{
				this.realModel = aConnection.getMetadata().getListOfProcedures();
				this.hasResultSet = true;
			}
			else if (verb.equalsIgnoreCase("LISTDB"))
			{
				this.realModel = aConnection.getMetadata().getListOfCatalogs();
				this.hasResultSet = true;
			}
			else
			{
				this.prepStatement = sqlcon.prepareStatement(aSql);
				// Only set the maxrows parameter for selects
				if (verb.equalsIgnoreCase("SELECT"))
				{
					this.prepStatement.setMaxRows(this.maxRows);
				}
				else
				{
					this.prepStatement.setMaxRows(0);
				}					
				start = System.currentTimeMillis();
				this.prepStatement.execute();
				end = System.currentTimeMillis();
				sqlTime = (end - start);
				rs = this.prepStatement.getResultSet();
				if (rs != null)
				{
					this.hasResultSet = true;
					this.realModel = new ResultSetTableModel(rs, null);
					rs.close();
				}
				else
				{
					this.hasResultSet = false;
				}
			}
			if (this.hasResultSet)
			{
				start = System.currentTimeMillis();
				this.sql = aSql;
				this.realModel.addTableModelListener(this);
				this.realModel.getDataStore().checkUpdateTable();
				this.infoTable.setVisible(false);
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(this.realModel, true);
				this.initColumns();
				this.infoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.infoTable.setVisible(true);
				end = System.currentTimeMillis();
				//System.out.println("populate = " + (end-start));
				this.lastMessage = ResourceMgr.getString(ResourceMgr.MSG_SQL_EXCUTE_OK);
				this.statusBar.setRowcount(this.infoTable.getModel().getRowCount());
			}
			else
			{
				StringBuffer msg = new StringBuffer(verb.toUpperCase() + " executed successfully.\r\n");
				this.hasResultSet = false;
				int count = 0;
				count = this.prepStatement.getUpdateCount();
				if (count >= 0)
				{
					msg.append("\r\n");
					msg.append(count + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
				}
				this.setMessageDisplayModel(this.getEmptyTableModel());
				this.lastMessage = msg.toString();
			}
			if (this.prepStatement != null) this.prepStatement.close();
			this.lastMessage = this.lastMessage + "\r\n" + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
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
		int selectedRow = this.infoTable.getSelectedRow();
		if (selectedRow != -1)
		{
			this.realModel.deleteRow(selectedRow);
			if (selectedRow >= this.realModel.getRowCount())
			{
				selectedRow --;
			}
			this.infoTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
		}
	}
	
	public void addRow()
	{
		int selectedRow = this.infoTable.getSelectedRow();
		final int newRow;
		
		if (selectedRow == -1)
		{
			newRow = this.realModel.addRow();
		}
		else
		{
			newRow = this.realModel.insertRow(selectedRow);
		}
		this.infoTable.getSelectionModel().setSelectionInterval(newRow, newRow);
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
		if (this.prepStatement != null)
		{
			try
			{
				LogMgr.logDebug(this, "Trying to cancel the current statement...");
				this.prepStatement.cancel();
				LogMgr.logDebug(this, "Call of cancel() finished.");
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
		this.realModel.getDataStore().restoreOriginalValues();
		this.repaint();
	}
	
	public String getLastMessage() { return this.lastMessage; }
	public boolean hasResultSet() { return this.hasResultSet; }

	public boolean isModified() { return this.realModel.getDataStore().isModified(); }

	private void initColumns()
	{
		this.infoTable.adjustColumns(this.maxWidth);
	}
	
	private void initLayout()
	{
		this.setLayout(new BorderLayout());
		this.infoTable = new WbTable();
		this.statusBar = new DwStatusBar();
		this.statusBar.setFocusable(false);
		this.setFocusable(false);
		this.scrollPane = new JScrollPane(this.infoTable);
		this.add(this.scrollPane, BorderLayout.CENTER);
		this.add(this.statusBar, BorderLayout.SOUTH);
		this.infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//this.infoTable.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.infoTable.setDoubleBuffered(true);
		this.maxWidth = WbManager.getSettings().getMaxColumnWidth();
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
		if (this.realModel != null)
		{
			this.realModel.dispose();
			this.realModel = null;
		}
		this.infoTable.setModel(new DefaultTableModel());
		this.statusBar.clearRowcount();
	}
	
	private TableModel getEmptyTableModel()
	{
		if (this.emptyModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.MSG_WARN_NO_RESULT);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.emptyModel = new DefaultTableModel(new Object[][] { {msg} } , new String[] {title});
		}
		return this.emptyModel;
	}
	
	private TableModel getErrorTableModel()
	{
		if (this.errorModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_DATA);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.errorModel = new DefaultTableModel(new Object[][] { {msg} } , new String[] {title});
		}
		return this.errorModel;
	}

	public WbTable getTable() { return this.infoTable; }
	
	/** This fine grain notification tells listeners the exact range
	 * of cells, rows, or columns that changed.
	 *
	 */
	public void tableChanged(TableModelEvent e)
	{
		if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
		{
			this.initColumns();
		}
	}
	
}
