package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.InvalidStatementException;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ResultSetTableModel;
import workbench.gui.components.WbTable;
import workbench.gui.renderer.DateColumnRenderer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;



/**
 *	A Panel which displays the result of a SELECT statement.
 *	A DBReader is used to read the results from the
 *	database and a DBReaderTableModel is used for the TableModel
 *	of the resulting JTable
 */
public class DwPanel extends JPanel
{
	private WbTable infoTable;
	private DwStatusBar statusBar;
	
	private ResultSetTableModel realModel;
	private String sql = null;
	private String lastMessage = null;
	private WbConnection dbConnection = null;
	private DefaultTableModel errorModel = null;
	private DefaultTableModel emptyModel = null;
	private int maxWidth = 250;
	private boolean hasResultSet = true;
	private PreparedStatement prepStatement;
	private JScrollPane scrollPane = null;
	
	public DwPanel()
	{
		this.initLayout();
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
	
	/**
	 *	Define the SELECT statement for this DBPanel.
	 *	If the given statement is NOT a SELECT statement
	 *	an InvalidStatementException is thrown.
	 */
	public void setSqlStatement(String aStatement)
		throws SQLException, InvalidStatementException, WbException
	{
		this.sql = aStatement;
	}
	
	public void saveChangesToDatabase()
		throws SQLException
	{
		try
		{
			this.realModel.saveChangesToDatabase(this.dbConnection);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			this.lastMessage = ExceptionUtil.getDisplay(e);
			throw e;
		}
	}
	
	public boolean isUpdateable()
	{
		if (this.realModel == null) return false;
		return this.realModel.isUpdateable();
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
		
		try
		{
			long start, end, sqlTime = 0;
			this.lastMessage = null;
			if (aSql.endsWith(";"))
			{
				aSql = aSql.substring(0, aSql.length() - 1);
			}
			String verb = SqlUtil.getSqlVerb(aSql);
			Connection sqlcon = aConnection.getSqlConnection();
			ResultSet rs = null;
			List keepColumns = null;
			
			this.statusBar.clearRowcount();
			
			if (verb.equalsIgnoreCase("DESC"))
			{
				StringTokenizer tok = new StringTokenizer(aSql, " ");
				tok.nextToken();
				String table = tok.nextToken();
				if (aConnection.getMetadata().storesUpperCaseIdentifiers()) 
				{
					table = table.toUpperCase();
				}
				else if (aConnection.getMetadata().storesLowerCaseIdentifiers())
				{
					table = table.toLowerCase();
				}
				rs = aConnection.getMetadata().getTableDefinition(table);
				final String[] cols = {"COLUMN_NAME", "TYPE_NAME","COLUMN_SIZE","COLUMN_DEF","IS_NULLABLE"};
				keepColumns = Arrays.asList(cols);
			}
			else if (verb.equalsIgnoreCase("LIST"))
			{
				rs = aConnection.getMetadata().getTables();
				final String[] cols = {"TABLE_CAT", "TABLE_NAME", "TABLE_TYPE"};
				keepColumns = Arrays.asList(cols);
			}
			else if (verb.equalsIgnoreCase("LISTPROCS"))
			{
				rs = sqlcon.getMetaData().getProcedures(null, null,"%");
				final String[] cols = {"PROCEDURE_CAT", "PROCEDURE_NAME", "PROCEDURE_TYPE"};
				keepColumns = Arrays.asList(cols);
			}
			else
			{
				this.prepStatement = null;
				if (verb.equalsIgnoreCase("SELECT"))
				{
					try
					{
						this.prepStatement = sqlcon.prepareStatement(aSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					}
					catch (Throwable th)
					{
						LogMgr.logWarning(this, "Could not create updateable ResultSet!");
						this.prepStatement = null;
					}
				}
				if (this.prepStatement == null)
				{
					this.prepStatement = sqlcon.prepareStatement(aSql);
				}
				start = System.currentTimeMillis();
				this.prepStatement.execute();
				end = System.currentTimeMillis();
				sqlTime = (end - start);
				rs = this.prepStatement.getResultSet();
				keepColumns = null;
			}
			if (rs != null)
			{
				this.hasResultSet = true;
				start = System.currentTimeMillis();
				this.realModel = new ResultSetTableModel(rs, keepColumns);
				this.setVisible(false);
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(this.realModel, true);
				this.initColumns();
				this.infoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.setVisible(true);
				end = System.currentTimeMillis();
				System.out.println("populate = " + (end-start));
				this.lastMessage = ResourceMgr.getString(ResourceMgr.MSG_SQL_EXCUTE_OK);
				rs.close();
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
	
	public String getLastMessage() { return this.lastMessage; }
	public boolean hasResultSet() { return this.hasResultSet; }

	private void initColumns()
	{
		Font f = this.infoTable.getFont();
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.infoTable.getColumnModel();
		String format = WbManager.getSettings().getDefaultDateFormat();
		this.infoTable.setDefaultRenderer(Date.class, new DateColumnRenderer(format));

		JTextField stringField = new JTextField();
		stringField.setBorder(null);
		DefaultCellEditor defEditor = new DefaultCellEditor(stringField);
		
		JTextField numberField = new JTextField();
		numberField.setBorder(null);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		DefaultCellEditor numbEditor = new DefaultCellEditor(numberField);
		
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			if (Number.class.isAssignableFrom(this.realModel.getColumnClass(i)))
			{
				col.setCellEditor(numbEditor);
			}
			else
			{
				col.setCellEditor(defEditor);
			}
			String s = this.realModel.getColumnName(i);
			int width = this.realModel.getColumnWidth(i) * charWidth;
			int lblWidth = fm.stringWidth(s) + (charWidth * 3);
			int w = Math.max(width, lblWidth);
			w = Math.min(w, maxWidth);
			col.setPreferredWidth(w);
		}
	}
	
	private void initLayout()
	{
		this.setLayout(new BorderLayout());
		this.infoTable = new WbTable();
		this.statusBar = new DwStatusBar();
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
}
