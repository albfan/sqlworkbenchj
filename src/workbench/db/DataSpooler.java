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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	public static final int EXPORT_ORALOADER = 3; // export in Oracle Loader format
	public static final int EXPORT_BCP = 4; // export in MS SQL Server bcp format
	
	private WbConnection dbConn;
	private String sql;
	private String outputfile;
	private int exportType;
	private boolean exportHeaders;
	private boolean includeCreateTable = false;
	private String tableName;
	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";
	
	/** If true, then cr/lf characters will be removed from
	 *  character columns
	 */
	private boolean cleancr = false;
	
	private boolean showProgress = false;
	private SpoolerProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;

	private boolean jobsRunning = false;
	
	
	private ArrayList jobQueue;
	
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
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolingRow"));
	
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

	public void addJob(String anOutputfile, String aStatement)
	{
		if (this.jobQueue == null)
		{
			this.jobQueue = new ArrayList();
		}
		SpoolerJob job = new DataSpooler.SpoolerJob();
		job.outputFile = anOutputfile;
		job.sqlStatement = aStatement;
		this.jobQueue.add(job);
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
		this.exportDataAsText(aConnection, aSql, anOutputfile, "\t", includeHeaders);
	}
	
	public void exportDataAsText(WbConnection aConnection
	                            ,String aSql
															,String anOutputfile
															,String aDelimiter
															, boolean includeHeaders)
		throws IOException, SQLException
	{
		this.dbConn = aConnection;
		this.sql = aSql;
		this.outputfile = anOutputfile;
		this.exportHeaders = includeHeaders;
		this.exportType = EXPORT_TXT;
		this.delimiter = aDelimiter;
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

	public void setTextDelimiter(String aDelimiter) { this.delimiter = aDelimiter; }
	public String getTextDelimiter() { return this.delimiter; }

	public void setTextQuoteChar(String aQuote) { this.quoteChar = aQuote; }
	public String getTextQuoteChar() { return this.quoteChar; }

	public void setTextDateFormat(String aFormat) { this.dateFormat = aFormat; }
	public String getTextDateFormat() { return this.dateFormat; }
	
	public void setTextTimestampFormat(String aFormat) { this.dateTimeFormat = aFormat; }
	public String getTextTimestampFormat() { return this.dateTimeFormat; }
	
	public void setOutputTypeText() { this.exportType = EXPORT_TXT; }
	public void setOutputTypeSqlInsert() { this.exportType = EXPORT_SQL; }
	public boolean isOutputTypeText() { return this.exportType == EXPORT_TXT; }
	public boolean isOutputTypeSqlInsert() { return this.exportType == EXPORT_SQL; }
	
	public void setOutputFilename(String aFilename) { this.outputfile = aFilename; }
	public String getOutputFilename() { return this.outputfile; }
	
	public void setCleanCarriageReturns(boolean aFlag)
	{
		this.cleancr = aFlag;
	}
	
	public void setConcatString(String aConcatString)
	{
		if (aConcatString == null) return;
		this.concatString = aConcatString;
	}
	
	public void setChrFunction(String aFunc)
	{
		this.chrFunc = aFunc;
	}
	
	public void setDecimalSymbol(char aSymbol)
	{
		this.decimalSymbol = aSymbol;
	}

	public void setDecimalSymbol(String aSymbol)
	{
		if (aSymbol == null || aSymbol.length() == 0) return;
		this.decimalSymbol = aSymbol.charAt(0);
	}
	
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

	public void startExportJobs()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { runJobs(); } catch (Throwable th) {}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void runJobs()
	{
		if (this.jobQueue == null) return;
		int count = this.jobQueue.size();
		this.jobsRunning = true;
		for (int i=0; i < count; i++)
		{
			SpoolerJob job = (SpoolerJob)this.jobQueue.get(i);
			this.sql = job.sqlStatement;
			this.outputfile = job.outputFile;
			if (this.progressPanel != null)
			{
				this.progressPanel.setFilename(this.outputfile);
				this.progressPanel.setRowInfo(0);
			}
			try
			{
				this.startExport();
			}
			catch (Throwable th)
			{
				LogMgr.logError("DataSpooler.runJobs()", "Error spooling data for [" + this.sql + "] to file: " + this.outputfile, th);
			}
		}
		this.jobsRunning = false;
		this.closeProgress();
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
		
		int colCount = meta.getColumnCount();
		int types[] = new int[colCount];
		for (int i=0; i < colCount; i++)
		{
			types[i] = meta.getColumnType(i+1);
		}
			
		boolean useQuotes = (this.quoteChar != null) && (this.quoteChar.trim().length() > 0);
		
		if (showProgress)
		{
			if (this.progressPanel == null) this.openProgressMonitor();
		}

		//byte[] quoteBytes = quoteChar.getBytes();
		//byte[] lineEnd = StringUtil.LINE_TERMINATOR.getBytes();
		//byte[] fieldBytes = delimiter.getBytes();
	
		SimpleDateFormat dateFormatter = null;
		SimpleDateFormat dateTimeFormatter = null;
		DecimalFormat numberFormatter = null;
		
		if (this.exportType == EXPORT_TXT)
		{
			if (this.dateFormat != null) 
			{
				try
				{
					dateFormatter = new SimpleDateFormat(this.dateFormat);
				}
				catch (IllegalArgumentException i)
				{
					dateFormatter = null;
				}
			}
			if (this.dateTimeFormat != null)
			{
				try
				{
					dateTimeFormatter = new SimpleDateFormat(this.dateTimeFormat);
				}
				catch (Exception e)
				{
					dateTimeFormatter = null;
				}
			}
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(this.decimalSymbol);
			numberFormatter = new DecimalFormat("#.#", symbols);
			numberFormatter.setGroupingUsed(false);
		}
		try
		{
			Object value = null;
			boolean quote = false;
			
			pw = new BufferedWriter(new FileWriter(this.outputfile), 16*1024);
			
			if (exportType == EXPORT_TXT && exportHeaders)
			{
				pw.write(ds.getHeaderString(this.delimiter).toString());
				pw.newLine();
			}
			else if (this.exportType == EXPORT_SQL && this.includeCreateTable)
			{
				String table = ds.getUpdateTable();
				String source = null;
				try
				{
					DbMetadata db = this.dbConn.getMetadata();
					DataStore def = db.getTableDefinition(table);
					source = db.getTableSource(table, def);
				}
				catch (Exception e)
				{
					LogMgr.logError("DataSpooler.startExport()", "Could not retrieve table definition for " + table, e);
					source = null;
				}
				if (source != null)
				{
					pw.write(source);
					pw.newLine();
				}
			}
			
			FieldPosition position = new FieldPosition(0);
			
			while (rs.next())
			{
				currentRow ++;
				if (showProgress)
				{
					progressPanel.setRowInfo(currentRow);
				}

				if (this.exportType == EXPORT_SQL)
				{
					row = ds.addRow(rs);
					line = ds.getRowDataAsSqlInsert(row, StringUtil.LINE_TERMINATOR, this.dbConn, this.chrFunc, this.concatString);
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
						
						if (value != null && !rs.wasNull())
						{
							if (currentRow == 1) System.out.println("value.class=" + value.getClass().getName());
							if (dateFormatter != null && value instanceof Date)
							{
								pw.write(dateFormatter.format((Date)value));
							}
							else if (this.dateTimeFormat != null && value instanceof Timestamp)
							{
								pw.write(dateTimeFormatter.format((Timestamp)value));
							}
							/*
							else if (numberFormatter != null && value instanceof Double)
							{
								pw.write(numberFormatter.format(((Double)value).doubleValue()));
							}
							else if (numberFormatter != null && value instanceof Float)
							{
								pw.write(numberFormatter.format(((Float)value).doubleValue()));
							}
							else if (numberFormatter != null && value instanceof BigDecimal)
							{
								pw.write(numberFormatter.format(((BigDecimal)value).doubleValue()));
							}
							*/
							else if (value instanceof Number)
							{
								position.setBeginIndex(0);
								position.setEndIndex(0);
								pw.write(numberFormatter.format(value, new StringBuffer(25), position).toString());
							}
							else if (value instanceof String)
							{
								if (quote) pw.write(quoteChar);
								if (this.cleancr)
								{
									pw.write(StringUtil.cleanNonPrintable((String)value));
								}
								else
								{
									pw.write((String)value);
								}
								if (quote) pw.write(quoteChar); 
							}
							else
							{
								pw.write(value.toString());
							}
						}
						if (i < colCount - 1) pw.write(this.delimiter);
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
			if (!jobsRunning) this.closeProgress();
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

	public boolean isIncludeCreateTable()
	{
		return includeCreateTable;
	}
	
	public void setIncludeCreateTable(boolean includeCreateTable)
	{
		this.includeCreateTable = includeCreateTable;
	}

	private class SpoolerJob
	{
		private String outputFile;
		private String sqlStatement;
	}

	public static void main(String[] args)
	{
		BigDecimal d = new BigDecimal(123.456);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat f = new DecimalFormat("#.#", symbols);
		FieldPosition p = new FieldPosition(0);
		System.out.println("d=" + f.format(d, new StringBuffer(), p));
	}
}
