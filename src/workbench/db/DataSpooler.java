/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import javax.swing.JFrame;

import workbench.WbManager;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.dbobjects.SpoolerProgressPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataSpooler
{
	public static final int EXPORT_SQL = 1;
	public static final int EXPORT_TXT = 2;

	private WbConnection dbConn;
	private String sql;
	private String outputfile;
	private int exportType;
	private boolean exportHeaders;
	private String tableName;
	
	private boolean showProgress = false;
	private SpoolerProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;
	
	public DataSpooler()
	{
	}

	/**
	 *	Open the progress monitor window.
	 */
	private void openProgressMonitor()
	{
		File f = new File(this.outputfile);
		String fname = f.getName();
		
		progressPanel = new SpoolerProgressPanel(this);
		this.progressPanel.setFilename(this.outputfile);
	
		this.progressWindow = new JFrame();
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setTitle(ResourceMgr.getString("MsgSpoolWindowTitle"));
		this.progressWindow.setIconImage(ResourceMgr.getPicture("SpoolData16").getImage());
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				keepRunning = false;
			}
		});
		
		WbSwingUtilities.center(this.progressWindow, null);
		this.progressWindow.show();
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
	}
	
	public void exportDataAsText(WbConnection aConnection
	                            ,String aSql
															,String anOutputfile
															, boolean includeHeaders)
		throws IOException, SQLException
	{
		this.dbConn = aConnection;
		this.sql = aSql;
		this.outputfile = anOutputfile;
		this.exportHeaders = includeHeaders;
		this.exportType = EXPORT_TXT;
		if (this.showProgress)
		{
			this.openProgressMonitor();
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run() { startBackgroundThread(); }
		});
	}
	
	public void stopExport() 
	{ 
		this.keepRunning = false; 
	}
	
	public void setTableName(String aTablename)
	{
		this.tableName = aTablename;
	}
	
	public void exportDataAsSqlInsert(WbConnection aConnection, String aSql, String anOutputfile)
		throws IOException, SQLException
	{
		this.dbConn = aConnection;
		this.sql = aSql;
		this.outputfile = anOutputfile;
		this.exportHeaders = false;
		this.exportType = EXPORT_SQL;
		if (this.showProgress)
		{
			this.openProgressMonitor();
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run() { startBackgroundThread(); }
		});
	}
	
	public void setShowProgress(boolean aFlag) { this.showProgress = aFlag; }
	public boolean getShowProgress() { return this.showProgress; }

	public void setExportHeaders(boolean aFlag) { this.exportHeaders = aFlag; }
	public boolean getExportHeaders() { return this.exportHeaders; }
	
	public void setOutputTypeText() { this.exportType = EXPORT_TXT; }
	public void setOutputTypeSqlInsert() { this.exportType = EXPORT_SQL; }
	public boolean isOutputTypeText() { return this.exportType == EXPORT_TXT; }
	public boolean isOutputTypeSqlInsert() { return this.exportType == EXPORT_SQL; }
	
	public void setOutputFilename(String aFilename) { this.outputfile = aFilename; }
	public String getOutputFilename() { return this.outputfile; }
	
	public void setSql(String aSql) { this.sql = aSql; }
	public String getSql() { return this.sql; }
	
	private void startBackgroundThread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { startExport(); } catch (Throwable th) {}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public void startExport()
		throws IOException, SQLException, WbException
	{
		Statement stmt = this.dbConn.createStatement();
		ResultSet rs = null;
		try
		{
			stmt.execute(this.sql);
			rs = stmt.getResultSet();
			this.startExport(rs);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
	}
	
	/**
	 *	Export a table to an external file.
	 *	The data will be "piped" through a DataStore in order to use 
	 *	the SQL scripting built into that object.
	 */
	public void startExport(ResultSet rs)
		throws IOException, SQLException, WbException
	{
		int interval = 1;
		int currentRow = 0;
		
		StringBuffer line = null;
		ResultSetMetaData meta = rs.getMetaData();
		DataStore ds = new DataStore(meta, this.dbConn);
		ds.setOriginalStatement(this.sql);
		if (this.exportType == EXPORT_SQL)
		{
			if (this.tableName == null)
			{
				if (!ds.useUpdateTableFromSql(this.sql))
				{
					throw new WbException(ResourceMgr.getString("ErrorSpoolSqlNotPossible"));
				}
			}
			else
			{
				ds.useUpdateTable(this.tableName);
			}
		}
		int row = 0;

		BufferedWriter pw = null;
		String fieldDelimit = WbManager.getSettings().getDefaultTextDelimiter();
		
		int colCount = meta.getColumnCount();
		int types[] = new int[colCount];
		for (int i=0; i < colCount; i++)
		{
			types[i] = meta.getColumnType(i+1);
		}
			
		String quoteChar = WbManager.getSettings().getQuoteChar();
		boolean useQuotes = (quoteChar != null) && (quoteChar.trim().length() > 0);
		
		if (showProgress)
		{
			this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolingRow"));
		}

		byte[] quoteBytes = quoteChar.getBytes();
		byte[] lineEnd = StringUtil.LINE_TERMINATOR.getBytes();
		byte[] fieldBytes = fieldDelimit.getBytes();
		
		try
		{
			Object value = null;
			boolean quote = false;
			
			pw = new BufferedWriter(new FileWriter(this.outputfile), 16*1024);
			
			if (exportHeaders && exportType == EXPORT_TXT)
			{
				pw.write(ds.getHeaderString().toString());
				pw.newLine();
			}
			
			while (rs.next())
			{
				currentRow ++;
				if (showProgress)
				{
//					if (interval == 1 && rsRow > 1000 ) interval = 1000;
//					else if (interval == 1000 && rsRow > 10000) interval = 5000;
//					if ( (rsRow % interval) == 0)
					progressPanel.setRowInfo(currentRow);
				}

				if (this.exportType == EXPORT_SQL)
				{
					row = ds.addRow(rs);
					line = ds.getRowDataAsSqlInsert(row, StringUtil.LINE_TERMINATOR, this.dbConn);
					ds.discardRow(row);
					if (line != null)
					{
						pw.write(line.toString());
						pw.newLine();
						pw.newLine();
					}
				}
				else 
				{
					// we don't use the DataStore when exporting to text for performance reasons
					for (int i=0; i < colCount; i++)
					{
						value = rs.getObject(i+1);
						quote = useQuotes && (types[i] == Types.VARCHAR || types[i] == Types.CHAR);
						if (quote) pw.write(quoteChar);
						
						if (value != null && !rs.wasNull())
						{
							pw.write(value.toString());
						}
						if (quote) pw.write(quoteChar); 
						
						if (i < colCount - 1) pw.write(fieldDelimit);
					}
					pw.newLine();
				}
				if (!this.keepRunning) break;
			}
		}
		catch (IOException e)
		{
			LogMgr.logError("DataSpooler", "Error writing data file", e);
			throw e;
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataSpooler", "SQL Error", e);
			throw e;
		}
		finally 
		{
			try { if (pw != null) pw.close(); } catch (Throwable th) {}
			this.closeProgress();
		}
	}

	public void closeProgress()
	{
		if (this.progressWindow != null)
		{
			this.progressWindow.hide();
			this.progressWindow.dispose();
			this.progressPanel = null;
		}
	}
	
	public void executeStatement(WbConnection aConnection, String aSql)
	{
		this.executeStatement(null, aConnection, aSql);
	}
	
	public void executeStatement(Window aParent, WbConnection aConnection, String aSql)
	{
		List tables = SqlUtil.getTables(aSql);
		boolean includeSqlExport = (tables.size() == 1);
		String filename = WbManager.getInstance().getExportFilename(aParent, includeSqlExport);
		if (filename != null)
		{
			DataSpooler spool = new DataSpooler();
			spool.setShowProgress(true);
			try
			{
				if (ExtensionFileFilter.hasSqlExtension(filename))
				{
					spool.exportDataAsSqlInsert(aConnection, aSql, filename);
				}
				else
				{
					spool.exportDataAsText(aConnection, aSql, filename, true);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpoolThread", "Could not export data", e);
			}
		}
	}
	
	public static void main(String[] args)
	{
		Connection con = null;
		try
		{
			Class.forName("com.inet.tds.TdsDriver");
			//Class.forName("oracle.jdbc.OracleDriver");
			//con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
			//con = DriverManager.getConnection("jdbc:inetdae:reosqlpro08:1433?database=visa", "visa", "savivisa");

			//con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
			con = DriverManager.getConnection("jdbc:inetdae:cpqdevdb01:1433?database=cpl_hq", "rds", "version42");

			WbConnection wb = new WbConnection(con);
			DataSpooler spooler = new DataSpooler();
			spooler.setShowProgress(true);
			//spooler.exportData(wb, "select * from visa_product", "c:/thomas/temp/visa_product.txt", true, EXPORT_TXT);
			//spooler.exportData(wb, "select * from siebel.s_contact", "c:/thomas/temp/contact.txt", true, EXPORT_TXT);
			//spooler.exportData(wb, "select * from epl_base_item", "c:/temp/test.txt", true, EXPORT_TXT);
			//spooler.openProgressMonitor("test.txt");
			System.exit(0);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}
	
}
