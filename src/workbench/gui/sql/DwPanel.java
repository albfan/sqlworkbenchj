package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.InvalidStatementException;
import workbench.exception.WbException;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTableSorter;
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
	
	private DwTableModel realModel;
	private String sql = null;
	private String lastMessage = null;
	private WbConnection dbConnection = null;
	private DefaultTableModel errorModel = null;
	private DefaultTableModel emptyModel = null;
	private int maxWidth = 250;
	//private int preferredWidth = 100;
	private boolean hasResultSet = true;
	private PreparedStatement prepStatement;
	private JScrollPane scrollPane = null;
	
	public DwPanel()
	{
		this.initLayout();
	}
	
	public void setMaxColWidth(int aWidth) { this.maxWidth = aWidth; }
	public int getMaxColWidth() { return this.maxWidth; }

	//public int getPreferredColWidth() { return this.maxWidth; }
	//public void setPreferredColWidth(int aWidth) { this.preferredWidth = aWidth; }
	
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
		if (this.dbConnection != null && this.sql != null)
		{
			this.sql = null;
			this.clearContent();
		}
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
			this.realModel.saveChangesToDatabase();
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
	
	public void runStatement()
		throws SQLException, WbException
	{
		this.runStatement(this.sql, this.dbConnection);
	}
	/**
	 *	Initialize the reader.
	 *	This method is called whenever the SQL statement
	 *	or the connection is set.
	 *	If both properties are present, this method
	 *	will send the statement to the database (via
	 *	a DBReader) and construct a TableModel from
	 *	the result set.
	 */
	public void runStatement(String aSql, WbConnection aConnection)
		throws SQLException, WbException
	{
		if (aSql == null || 
		    aConnection == null || 
				aConnection.isClosed())
		{
			return;
		}
		
		try
		{
			long start, end;
			this.lastMessage = null;
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
				rs = sqlcon.getMetaData().getColumns(null, null, table, "%");
				final String[] cols = {"COLUMN_NAME", "TYPE_NAME","COLUMN_SIZE","COLUMN_DEF","IS_NULLABLE"};
				keepColumns = Arrays.asList(cols);
			}
			else if (verb.equalsIgnoreCase("LIST"))
			{
				rs = sqlcon.getMetaData().getTables(null, null, null, null);
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
				this.prepStatement = sqlcon.prepareStatement(aSql);
				start = System.currentTimeMillis();
				this.prepStatement.execute();
				end = System.currentTimeMillis();
				System.out.println("retrieve " + (end - start));
				rs = this.prepStatement.getResultSet();
				keepColumns = null;
			}
			if (rs != null)
			{
				this.hasResultSet = true;
				start = System.currentTimeMillis();
				this.realModel = new DwTableModel(rs, this.dbConnection);
				this.setVisible(false);
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(this.realModel, true);
				this.initColumns(keepColumns);
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
				msg.append("\r\n");
				msg.append(count + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED));
				this.setMessageDisplayModel(this.getEmptyTableModel());
				this.lastMessage = msg.toString();
			}
			if (this.prepStatement != null) this.prepStatement.close();
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

	private void initColumns(List keepColumns)
	{
		Font f = this.infoTable.getFont();
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.infoTable.getColumnModel();
		this.infoTable.setDefaultRenderer(java.util.Date.class, new DateColumnRenderer());

		List tbr = new ArrayList();
		if (keepColumns != null)
		{
			for (int i=0; i < colMod.getColumnCount(); i++)
			{
				String s = this.realModel.getColumnName(i);
				if (keepColumns != null && !keepColumns.contains(s))
				{
					TableColumn col = colMod.getColumn(i);
					tbr.add(col);
				}
			}
			
			for (int i=0; i < tbr.size(); i++)
			{
				colMod.removeColumn((TableColumn)tbr.get(i));
			}
		}
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
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
		this.scrollPane.setBorder(null);
		this.add(this.scrollPane, BorderLayout.CENTER);
		this.add(this.statusBar, BorderLayout.SOUTH);
		this.infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.infoTable.setBorder(null);
		this.infoTable.setDoubleBuffered(true);
		this.maxWidth = WbManager.getSettings().getMaxColumnWidth();
		//this.preferredWidth = WbManager.getSettings().getPreferredColumnWidth();
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
