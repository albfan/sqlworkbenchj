/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
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
import javax.swing.JOptionPane;

import workbench.WbManager;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataSpooler
	implements Interruptable
{
	public static final int EXPORT_SQL = 1;
	public static final int EXPORT_TXT = 2;
	public static final int EXPORT_XML = 3;
	
	private WbConnection dbConn;
	private String sql;
	private String outputfile;
	private String fullOutputFileName;
	private int exportType;
	private boolean exportHeaders;
	private boolean includeCreateTable = false;
	private boolean headerOnly = false;
	private String tableName;
	private String encoding = "UTF-8";
	
	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";
	private int commitEvery=0;
	private int maxDigits=340;
	
	/** If true, then cr/lf characters will be removed from
	 *  character columns
	 */
	private boolean cleancr = false;

	private boolean showProgress = false;
	private ProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;
	private boolean cancelJobs = false;
	private int pendingJobs = 0;
	private boolean jobsRunning = false;
	private RowActionMonitor rowMonitor;

	private boolean success = false;
	private boolean hasWarning = false;

	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
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
		
		progressPanel = new ProgressPanel(this);
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

	public boolean confirmCancel()
	{
		if (!this.jobsRunning) return true;
		String msg = ResourceMgr.getString("MsgCancelAllCurrent");
		String current = ResourceMgr.getString("LabelCancelCurrentExport");
		String all = ResourceMgr.getString("LabelCancelAllExports");
		int answer = WbSwingUtilities.getYesNo(this.progressWindow, msg, new String[] { current, all });
		if (answer == 1)
		{
			this.cancelJobs = true;
		}
		return true;
	}
	
	public void cancelExecution() 
	{ 
		this.keepRunning = false; 
	}
	
	public void setTableName(String aTablename)
	{
		this.tableName = aTablename;
	}

	public void setXmlEncoding(String enc) { this.encoding = enc; }
	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
	}
	
	public void setExportHeaderOnly(boolean aFlag) { this.headerOnly = aFlag; }
	public boolean getExportHeaderOnly() { return this.headerOnly; }
	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }
	
	public void setShowProgress(boolean aFlag) 
	{
	  if (!WbManager.getInstance().isBatchMode())
	  {
	    this.showProgress = aFlag; 
	  }
	}
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

	public void setOutputTypeXml() { this.exportType = EXPORT_XML; }
	public void setOutputTypeText() { this.exportType = EXPORT_TXT; }
	public void setOutputTypeSqlInsert() { this.exportType = EXPORT_SQL; }
	
	public boolean isOutputTypeText() { return this.exportType == EXPORT_TXT; }
	public boolean isOutputTypeSqlInsert() { return this.exportType == EXPORT_SQL; }
	public boolean isOutputTypeSqlXml() { return this.exportType == EXPORT_XML; }
	
	public void setOutputFilename(String aFilename) { this.outputfile = aFilename; }
	public String getOutputFilename() { return this.outputfile; }
	public String getFullOutputFilename() { return this.fullOutputFileName; }
	
	
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
		this.pendingJobs = count;
		this.jobsRunning = true;
		this.cancelJobs = false;
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
			this.pendingJobs --;
			if (this.cancelJobs) break;
		}
		this.pendingJobs = 0;
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
			stmt.setFetchSize(1500);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DataSpooler.startExport()", "Setting fetch size to 500 failed!");
		}
		
		try
		{
			stmt.execute(this.sql);
			rs = stmt.getResultSet();
			this.startExport(rs);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataSpooler.startExport()", "Could not execute SQL statement: " + e.getMessage(), e);
			if (this.showProgress)
			{
				WbManager.getInstance().showErrorMessage(this.progressWindow, ResourceMgr.getString("MsgExecuteError") + ": " + e.getMessage());
			}
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
	}
	
	public boolean isSuccess() { return this.errors.size() == 0; }
	public boolean hasWarning() { return this.warnings.size() > 0; }

	public String[] getErrors()
	{
		int count = this.errors.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.errors.get(i);
		}
		return result;
	}
	public String[] getWarnings()
	{
		int count = this.warnings.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.warnings.get(i);
		}
		return result;
	}
	
	/**
	 *	Export a table to an external file.
	 *	The data will be "piped" through a DataStore in order to use 
	 *	the SQL scripting built into that object.
	 */
	public long startExport(ResultSet rs)
		throws IOException, SQLException, WbException
	{
		int interval = 1;
		int currentRow = 0;
		
		this.warnings.clear();
		this.errors.clear();
		
		StringBuffer line = null;
		ResultSetMetaData meta = rs.getMetaData();
		DataStore ds = new DataStore(meta, this.dbConn);
		ds.setOriginalStatement(this.sql);
		
		if (showProgress)
		{
			if (this.progressPanel == null) this.openProgressMonitor();
		}
		
		
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
		else if (this.exportType == EXPORT_XML)
		{
			if (this.tableName == null)
			{
				if (ds.useUpdateTableFromSql(this.sql))
				{
					this.tableName = ds.getUpdateTable();
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
		
		//byte[] quoteBytes = quoteChar.getBytes();
		//byte[] lineEnd = StringUtil.LINE_TERMINATOR.getBytes();
		//byte[] fieldBytes = delimiter.getBytes();
	
		SimpleDateFormat dateFormatter = null;
		SimpleDateFormat dateTimeFormatter = null;
		DecimalFormat numberFormatter = null;
		
		if (this.exportType == EXPORT_TXT || this.exportType == EXPORT_XML)
		{
			if (this.dateFormat != null) 
			{
				try
				{
					dateFormatter = new SimpleDateFormat(this.dateFormat);
					ds.setDefaultDateFormatter(dateFormatter);
				}
				catch (IllegalArgumentException i)
				{
					this.warnings.add(ResourceMgr.getString("ErrorWrongDateFormat") + " " + this.dateFormat);
					dateFormatter = null;
				}
			}
			if (this.dateTimeFormat != null)
			{
				try
				{
					dateTimeFormatter = new SimpleDateFormat(this.dateTimeFormat);
					ds.setDefaultTimestampFormatter(dateTimeFormatter);
				}
				catch (Exception e)
				{
					this.warnings.add(ResourceMgr.getString("ErrorWrongDateFormat") + " " + this.dateTimeFormat);
					dateTimeFormatter = null;
				}
			}
			
			if (this.decimalSymbol != 0)
			{
				DecimalFormatSymbols symbols = new DecimalFormatSymbols();
				symbols.setDecimalSeparator(this.decimalSymbol);
				numberFormatter = new DecimalFormat("0.#", symbols);
				numberFormatter.setGroupingUsed(false);
				numberFormatter.setMaximumFractionDigits(999);
				ds.setDefaultNumberFormatter(numberFormatter);
			}
		}
		
		try
		{
			Object value = null;
			boolean quote = false;
	
			File f = new File(this.outputfile);
			this.fullOutputFileName = f.getAbsolutePath();
			if (this.exportType == EXPORT_XML)
			{
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f), this.encoding);
				pw = new BufferedWriter(out);
			}
			else
			{
				pw = new BufferedWriter(new FileWriter(f), 16*1024);
			}
			
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
			else if (this.exportType == EXPORT_XML)
			{
				pw.write(ds.getXmlStart(this.encoding).toString());
			}
			
			FieldPosition position = new FieldPosition(0);

			while (rs.next())
			{
				currentRow ++;
				if (showProgress)
				{
					progressPanel.setRowInfo(currentRow);
				}
				if (this.rowMonitor != null)
				{
					this.rowMonitor.setCurrentRow(currentRow, -1);
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
						if (this.commitEvery > 0)
						{
							if ((currentRow % this.commitEvery) == 0)
							{
								pw.write("COMMIT;");
								pw.newLine();
								pw.newLine();
							}
						}
					}
				}
				else if (this.exportType == EXPORT_XML)
				{
					row = ds.addRow(rs);
					line = ds.getRowDataAsXml(row, "    ", (int)currentRow);
					ds.discardRow(row);
					if (line != null)
					{
						// the line returned by getRowDataAsXml() already contains
						// a NEWLINE, so we don't need to create it here
						pw.write(line.toString());
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
							//if (currentRow == 1) System.out.println("value.class=" + value.getClass().getName());
							if (dateFormatter != null && value instanceof Date)
							{
								pw.write(dateFormatter.format((Date)value));
							}
							else if (this.dateTimeFormat != null && value instanceof Timestamp)
							{
								pw.write(dateTimeFormatter.format((Timestamp)value));
							}
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
			
			if (this.exportType == EXPORT_XML)
			{
				pw.write(ds.getXmlEnd().toString());
			}
			else if (this.exportType == EXPORT_SQL)
			{
				pw.write("COMMIT;");
				pw.newLine();
			}
		}
		catch (IOException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataSpooler", "Error writing data file", e);
			throw e;
		}
		catch (SQLException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataSpooler", "SQL Error", e);
			throw e;
		}
		finally 
		{
			try { if (pw != null) pw.close(); } catch (Throwable th) {}
			if (!jobsRunning) this.closeProgress();
		}
		return currentRow;
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
		String cleanSql = SqlUtil.makeCleanSql(aSql, false);
		List tables = SqlUtil.getTables(cleanSql);
		boolean includeSqlExport = (tables.size() == 1);
		String tablename = null;
		if (includeSqlExport)
		{
			tablename = (String)tables.get(0);
		}
		String filename = WbManager.getInstance().getExportFilename(aParent, includeSqlExport);
		if (filename != null)
		{
			//DataSpooler spool = new DataSpooler();
			try
			{
				this.setConnection(aConnection);
				this.setOutputFilename(filename);
				this.setShowProgress(true);
				this.setSql(cleanSql);
				
				if (ExtensionFileFilter.hasSqlExtension(filename))
				{
					this.setOutputTypeSqlInsert();
					this.setTableName(tablename);
				}
				else if (ExtensionFileFilter.hasXmlExtension(filename))
				{
					this.setOutputTypeXml();
					this.setTableName(tablename);
				}
				else
				{
					this.setOutputTypeText();
				}
				this.startBackgroundThread();
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
