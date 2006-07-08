/*
 * DataExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;

import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.ErrorReporter;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.gui.dialogs.export.ExportFileDialog;
import workbench.gui.dialogs.export.ExportOptions;
import workbench.gui.dialogs.export.HtmlOptions;
import workbench.gui.dialogs.export.SqlOptions;
import workbench.gui.dialogs.export.TextOptions;
import workbench.gui.dialogs.export.XmlOptions;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.util.CharacterRange;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;


/**
 *
 * @author  support@sql-workbench.net
 */
public class DataExporter
	implements Interruptable, ErrorReporter
{
	public static final int EXPORT_SQL = 1;
	public static final int EXPORT_TXT = 2;
	public static final int EXPORT_XML = 3;
	public static final int EXPORT_HTML = 4;

	private WbConnection dbConn;
	private String sql;
	private String htmlTitle = null;
	private String outputfile;
	private String fullOutputFileName;
	private String xsltFile = null;
	private String transformOutputFile = null;
	private int exportType;
	private boolean exportHeaders;
	private boolean includeCreateTable = false;
	private boolean headerOnly = false;

	private int sqlType = SqlRowDataConverter.SQL_INSERT;
	private boolean useCDATA = false;
	private CharacterRange escapeRange = null;
	private String lineEnding = "\n";
	private String tableName;
	private String sqlTable;
	private String encoding;
	private List columnsToExport;

	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean quoteAlways = false;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";
	private String concatFunction = null;
	private int commitEvery=0;

	private SimpleDateFormat dateFormatter = null;
	private	SimpleDateFormat dateTimeFormatter = null;
	private DecimalFormat numberFormatter = null;

	private boolean cleancr = false;
	private boolean append = false;
	private boolean escapeHtml = true;
	private boolean createFullHtmlPage = true;
	private boolean verboseFormat = true;

	private boolean showProgressWindow = false;
	public static final int DEFAULT_PROGRESS_INTERVAL = 10;
	private int progressInterval = DEFAULT_PROGRESS_INTERVAL;

	private ProgressPanel progressPanel;
	private JDialog progressWindow;
	private ExportJobEntry currentJob;
	private boolean cancelJobs = false;
	//private int pendingJobs = 0;
	private boolean jobsRunning = false;
	private RowActionMonitor rowMonitor;

	private List keyColumnsToUse;

	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
	private ArrayList jobQueue;
	private ExportWriter exportWriter;
	private Window parentWindow;
	private int tablesExported;
	private long totalRows;
	
	private boolean writeOracleControlFile = false;
	
	public DataExporter(WbConnection con)
	{
		this.dbConn = con;
		this.setExportHeaders(Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false));
	}

	private void createProgressPanel()
	{
		progressPanel = new ProgressPanel(this);
		this.progressPanel.setFilename(this.outputfile);
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolStart"));
	}
	/**
	 *	Open the progress monitor window.
	 */
	private void openProgressMonitor(Frame parent, boolean modal)
	{

		if (this.progressPanel == null) createProgressPanel();

		this.progressWindow = new JDialog(parent, modal);
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setTitle(ResourceMgr.getString("MsgSpoolWindowTitle"));
		//this.progressWindow.setIconImage(ResourceMgr.getPicture("SpoolData16").getImage());
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				cancelExecution();
			}
		});

		WbSwingUtilities.center(this.progressWindow, null);
		this.progressWindow.setVisible(true);
	}

	public void clearJobs()
	{
		if (this.jobsRunning) return;
		if (this.jobQueue == null) return;
		this.jobQueue.clear();
	}
	
	public void addTableExportJob(String anOutputfile, TableIdentifier table)
		throws SQLException
	{
		if (this.jobQueue == null)
		{
			this.jobQueue = new ArrayList();
		}
		ExportJobEntry job = new ExportJobEntry(anOutputfile, table, this.dbConn);
		this.jobQueue.add(job);
	}

	public WbConnection getConnection()
	{
		return this.dbConn;
	}

	public boolean confirmCancel()
	{
		if (!this.jobsRunning) return true;
		String msg = ResourceMgr.getString("MsgCancelAllCurrent");
		String current = ResourceMgr.getString("LblCancelCurrentExport");
		String all = ResourceMgr.getString("LblCancelAllExports");
		int answer = WbSwingUtilities.getYesNo(this.progressWindow, msg, new String[] { current, all });
		if (answer == 1)
		{
			this.cancelJobs = true;
		}
		return true;
	}

	public void cancelExecution()
	{
		this.cancelJobs = true;
		if (this.exportWriter != null)
		{
			this.exportWriter.cancel();
			this.warnings.add(ResourceMgr.getString("MsgExportCancelled"));
		}
	}

	public void setTableName(String aTablename) { this.tableName = aTablename; }
	public String getTableName() { return this.tableName; }

	public void setEncoding(String enc) { this.encoding = enc; }
	public String getEncoding() { return this.encoding; }

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	/**
	 *	Define the columns that should be exported
	 *  This is only respected for the export of a DataStore, not
	 *  for exporting a ResultSet
	 *
	 *	@see #startExport(workbench.storage.DataStore)
	 */
	public void setColumnsToExport(List columns)
	{
		this.columnsToExport = columns;
	}

	public List getColumnsToExport()
	{
		return this.columnsToExport;
	}

	public void setExportAllColumns()
	{
		this.columnsToExport = null;
	}

	public void setUseCDATA(boolean flag) { this.useCDATA = flag; }
	public boolean getUseCDATA() { return this.useCDATA; }

	public void setAppendToFile(boolean aFlag) { this.append = aFlag; }
	public boolean getAppendToFile() { return this.append; }

	public void setExportHeaderOnly(boolean aFlag) { this.headerOnly = aFlag; }
	public boolean getExportHeaderOnly() { return this.headerOnly; }

	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }

	public String getTypeDisplay()
	{
		switch (this.exportType)
		{
			case EXPORT_HTML:
				return "HTML";

			case EXPORT_SQL:
				if (this.getSqlType() == SqlRowDataConverter.SQL_DELETE_INSERT)
					return "SQL DELETE/INSERT";
				else if (this.getSqlType() == SqlRowDataConverter.SQL_INSERT)
					return "SQL INSERT";
				else if (this.getSqlType() == SqlRowDataConverter.SQL_UPDATE)
					return "SQL UPDATE";
				else 
					return "SQL";
				
			case EXPORT_TXT:
				return "Text";

			case EXPORT_XML:
				return "XML";
		}
		return "";
	}

	/**
	 * Control the progress display in the RowActionMonitor
	 * This is used by the WBEXPORT command to turn off the row
	 * progress display. Turning off the display will speed up
	 * the export because the GUI does not need to be updated
	 */
	public void setProgressInterval(int interval)
	{
		if (interval <= 0)
			this.progressInterval = 0;
		else
			this.progressInterval = interval;
	}
	public int getProgressInterval() { return this.progressInterval; }


	/** Control the display of a progress window. This is used
	 *  from within the DbExplorer
	 */
	public void setShowProgressWindow(boolean aFlag)
	{
	  if (!WbManager.getInstance().isBatchMode())
	  {
	    this.showProgressWindow = aFlag;
	  }
	}
	public boolean getShowProgressWindow() { return this.showProgressWindow; }

	public void setXsltTransformation(String xsltFileName)
	{
		this.xsltFile = xsltFileName;
	}
	public String getXsltTransformation() { return this.xsltFile; }

	public void setXsltTransformationOutput(String aFilename)
	{
		this.transformOutputFile = aFilename;
	}
	public String getXsltTransformationOutput() { return this.transformOutputFile; }

	public void setExportHeaders(boolean aFlag) 
	{ 
		this.exportHeaders = aFlag; 
	}
	public boolean getExportHeaders() { return this.exportHeaders; }

	public void setCreateFullHtmlPage(boolean aFlag) { this.createFullHtmlPage = aFlag; }
	public boolean getCreateFullHtmlPage() { return this.createFullHtmlPage; }

	public void setEscapeHtml(boolean aFlag) { this.escapeHtml = aFlag; }
	public boolean getEscapeHtml() { return this.escapeHtml; }

	public void setTextDelimiter(String aDelimiter)
	{
		if (aDelimiter != null && aDelimiter.trim().length() > 0) this.delimiter = aDelimiter;
	}
	public String getTextDelimiter() { return this.delimiter; }

	public void setTextQuoteChar(String aQuote) { this.quoteChar = aQuote; }
	public String getTextQuoteChar() { return this.quoteChar; }

	public void setDateFormat(String aFormat)
	{
		if (StringUtil.isEmptyString(aFormat))
		{
			aFormat = Settings.getInstance().getDefaultDateFormat();
		}
		if (StringUtil.isEmptyString(aFormat)) return;
		this.dateFormat = aFormat;
		if (this.dateFormat != null)
		{
			try
			{
				dateFormatter = new SimpleDateFormat(this.dateFormat);
			}
			catch (IllegalArgumentException i)
			{
				this.addWarning(ResourceMgr.getString("ErrWrongDateFormat") + " " + this.dateFormat);
				dateFormatter = null;
			}
		}
	}

	public SimpleDateFormat getDateFormatter()
	{
		return this.dateFormatter;
	}

	public String getDateFormat()
	{
		return this.dateFormat;
	}

	public void setTimestampFormat(String aFormat)
	{
		if (StringUtil.isEmptyString(aFormat))
		{
			aFormat = Settings.getInstance().getDefaultTimestampFormat();
		}
		if (StringUtil.isEmptyString(aFormat)) return;
		this.dateTimeFormat = aFormat;
		if (this.dateTimeFormat != null)
		{
			try
			{
				dateTimeFormatter = new SimpleDateFormat(this.dateTimeFormat);
			}
			catch (Exception e)
			{
				this.warnings.add(ResourceMgr.getString("ErrWrongDateFormat") + " " + this.dateTimeFormat);
				dateTimeFormatter = null;
			}
		}
	}

	public String getTimestampFormat() { return this.dateTimeFormat; }
	public SimpleDateFormat getTimestampFormatter()
	{
		return this.dateTimeFormatter;
	}

	private void createExportWriter()
	{
		ExportWriter exporter = null;

		switch (this.exportType)
		{
			case EXPORT_HTML:
				this.exportWriter = new HtmlExportWriter(this);
				break;
			case EXPORT_SQL:
				this.exportWriter = new SqlExportWriter(this);
				break;
			case EXPORT_TXT:
				this.exportWriter = new TextExportWriter(this);
				break;
			case EXPORT_XML:
				this.exportWriter = new XmlExportWriter(this);
		}
	}
	
	public void setHtmlTitle(String aTitle) { this.htmlTitle = aTitle; }
	public String getHtmlTitle() { return this.htmlTitle; }

	public void setOutputTypeHtml() 
	{ 
		this.exportType = EXPORT_HTML; 
		createExportWriter();
	}
	
	public void setOutputTypeXml() 
	{ 
		this.exportType = EXPORT_XML; 
		createExportWriter();
	}
	
	public void setOutputTypeText() 
	{ 
		this.exportType = EXPORT_TXT; 
		createExportWriter();
	}

	public void setOutputTypeSqlInsert()
	{
		this.exportType = EXPORT_SQL;
		this.sqlType = SqlRowDataConverter.SQL_INSERT;
		createExportWriter();
	}

	public void setOutputTypeSqlUpdate()
	{
		this.exportType = EXPORT_SQL;
		this.sqlType = SqlRowDataConverter.SQL_UPDATE;
		createExportWriter();
	}

	public void setOutputTypeSqlDeleteInsert()
	{
		this.exportType = EXPORT_SQL;
		this.sqlType = SqlRowDataConverter.SQL_DELETE_INSERT;
		createExportWriter();
	}

	public int getSqlType()
	{
		if (this.exportType == EXPORT_SQL)
			return this.sqlType;
		else
			return -1;
	}

	public void setOutputFilename(String aFilename) 
	{ 
		this.outputfile = aFilename; 
	}

	public String getOutputFilename() { return this.outputfile; }
	public String getFullOutputFilename() { return this.fullOutputFileName; }

	public void setCleanupCarriageReturns(boolean aFlag) { this.cleancr = aFlag; }
	public boolean getCleanupCarriageReturns() { return this.cleancr; }

	public void setConcatString(String aConcatString)
	{
		if (aConcatString == null) return;
		this.concatString = aConcatString;
		this.concatFunction = null;
	}
	public String getConcatString() { return this.concatString; }

	public void setChrFunction(String aFunc) { this.chrFunc = aFunc; }
	public String getChrFunction() { return this.chrFunc; }

	public void setDecimalSymbol(char aSymbol)
	{
		this.decimalSymbol = aSymbol;
		if (this.decimalSymbol != 0)
		{
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(this.decimalSymbol);
			numberFormatter = new DecimalFormat("0.#", symbols);
			numberFormatter.setGroupingUsed(false);
			numberFormatter.setMaximumFractionDigits(999);
		}
		else
		{
			numberFormatter = Settings.getInstance().getDefaultDecimalFormatter();
		}
	}
	
	public DecimalFormat getDecimalFormatter() { return this.numberFormatter; }


	public void setDecimalSymbol(String aSymbol)
	{
		if (StringUtil.isEmptyString(aSymbol)) return;
		this.setDecimalSymbol(aSymbol.charAt(0));
	}

	public void setSql(String aSql)
	{
		this.sql = aSql;
		if (aSql != null) 
		{
			String cleanSql = SqlUtil.makeCleanSql(aSql, false);
			List tables = SqlUtil.getTables(cleanSql);
			if (tables.size() == 1);
			{
				this.sqlTable = (String)tables.get(0);
			}
		}
		else
		{
			this.sqlTable = null;
		}
	}

	public String getSql() { return this.sql; }
	public int getNumberExportedTables() { return this.tablesExported; }
	
	private void startBackgroundThread()
	{
		Thread t = new WbThread("Export")
		{
			public void run()
			{
				try { startExport(); } catch (Throwable th) {}
			}
		};
		t.start();
	}

	public void startExportJobs(final Frame parent)
	{
		if (this.showProgressWindow)
		{
			// the progress window is a modal dialog
			// so we need to open that in a new thread
			// otherwise this thread would be blocked
			WbThread p = new WbThread("Progress Thread")
			{
				public void run()
				{
					createProgressPanel();
					openProgressMonitor(parent, true);
				}
			};
			p.start();
		}
		Thread t = new WbThread("Export Jobs")
		{
			public void run()
			{
				try { runJobs(); } catch (Throwable th) {}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void runJobs()
	{
		if (this.jobQueue == null) return;
		int count = this.jobQueue.size();
		//this.pendingJobs = count;
		this.jobsRunning = true;
		this.cancelJobs = false;
		this.tablesExported = 0;
		this.totalRows = 0;
		this.setSql(null);
		
		if (this.exportWriter == null)
		{
			this.createExportWriter();
		}
		
		for (int i=0; i < count; i++)
		{
			this.currentJob = (ExportJobEntry)this.jobQueue.get(i);
			
			this.setOutputFilename(this.currentJob.getOutputFile());
			if (this.progressPanel != null)
			{
				this.progressPanel.setFilename(this.outputfile);
				this.progressPanel.setRowInfo(0);
				this.progressWindow.pack();
			}
			if (this.rowMonitor != null && this.currentJob.getTableName() != null)
			{
				StringBuffer msg = new StringBuffer(80);
				msg.append(this.currentJob.getTableName());
				msg.append(" [");
				msg.append(i+1);
				msg.append('/');
				msg.append(count);
				msg.append("] ");
				this.rowMonitor.setCurrentObject(msg.toString(), i+1, count);
			}

			try
			{
				totalRows += this.startExport();
				this.tablesExported ++;
			}
			catch (Throwable th)
			{
				LogMgr.logError("DataExporter.runJobs()", "Error exporting data for [" + this.sql + "] to file: " + this.outputfile, th);
				this.addError(th.getMessage());
			}
			//this.pendingJobs --;
			if (this.cancelJobs) break;
		}
		//this.pendingJobs = 0;
		this.jobsRunning = false;
		this.closeProgress();
	}

	public long getTotalRows() { return this.totalRows; }
	
	public void setCurrentRow(int currentRow)
	{
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentRow(currentRow, -1);
		}
	}

	/**
	 * Start the export. This will execute the defined query
	 * and then write the result into the outputfile
	 */
	public long startExport()
		throws IOException, SQLException
	{
		Statement stmt = this.dbConn.createStatementForQuery();
		ResultSet rs = null;
		long rows = 0;
		boolean busyControl = false;
		try
		{
			if (!this.dbConn.isBusy()) 
			{
				// only set the busy flag if the caller did not already do this!
				this.dbConn.setBusy(true);
				busyControl = true;
			}
			if (this.currentJob != null)
			{
				stmt.execute(this.currentJob.getQuerySql());
			}
			else
			{
				stmt.execute(this.sql);
			}
			rs = stmt.getResultSet();
			rows = this.startExport(rs);
		}
		catch (Exception e)
		{
			this.addError(ResourceMgr.getString("MsgExecuteError") + this.sql);
			this.addError(ExceptionUtil.getDisplay(e));
			LogMgr.logError("DataExporter.startExport()", "Could not execute SQL statement: " + this.sql + ", Error: " + e.getMessage(), e);
			if (this.showProgressWindow)
			{
				if (!jobsRunning) this.closeProgress();
				WbSwingUtilities.showErrorMessage(this.parentWindow, ResourceMgr.getString("MsgExecuteError") + ": " + e.getMessage());
			}
			if (!this.dbConn.getAutoCommit())
			{
				// Postgres needs a rollback, but this doesn't (or shouldn't!) 
				// hurt with other DBMS either
				try { this.dbConn.rollback(); } catch (Throwable th) {}
			}
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
			if (!jobsRunning) this.closeProgress();
			if (busyControl) this.dbConn.setBusy(false);
		}
		return rows;
	}

	public boolean isSuccess() { return this.errors.size() == 0; }
	public boolean hasWarning() { return this.warnings.size() > 0; }
	public boolean hasError() { return this.errors.size() > 0; }

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

	public void addWarning(String msg)
	{
		if (this.warnings == null) this.warnings = new ArrayList();
		this.warnings.add(msg);
	}

	public void addError(String msg)
	{
		if (this.errors == null) this.errors = new ArrayList();
		this.errors.add(msg);
	}

	public long startExport(ResultSet rs)
		throws IOException, SQLException, Exception
	{
		try
		{
			ResultSetMetaData meta = rs.getMetaData();
			ResultInfo rsInfo = new ResultInfo(meta, this.dbConn);
			ResultInfo info = null;
			if (this.currentJob != null)
			{
				info = currentJob.getResultInfo();
				for (int i=0; i < info.getColumnCount(); i++)
				{
					int colIndex = rsInfo.findColumn(info.getColumnName(i));
					if (colIndex > -1)
					{
						info.setColumnClassName(i, rsInfo.getColumnClassName(i));
					}
				}
			}
			else
			{
				info = rsInfo;
			}
			configureExportWriter(info);
			this.exportWriter.writeExport(rs, info);
		}
		catch (SQLException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataExporter.startExport()", "SQL Error", e);
			throw e;
		}
		finally
		{
			if (this.exportWriter != null) this.exportWriter.exportFinished();
			if (!jobsRunning) this.closeProgress();
			try { rs.close(); } catch (Throwable th) {}
		}
		long numRows = this.exportWriter.getNumberOfRecords();
		return numRows;
	}

	public long startExport(DataStore ds)
		throws IOException, SQLException, Exception
	{
		try
		{
			ResultInfo info = ds.getResultInfo();
			configureExportWriter(info);
			this.exportWriter.writeExport(ds);
		}
		catch (SQLException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataExporter.startExport()", "SQL Error", e);
			throw e;
		}
		finally
		{
			if (this.exportWriter != null) this.exportWriter.exportFinished();
			if (!jobsRunning) this.closeProgress();
		}
		long numRows = this.exportWriter.getNumberOfRecords();
		return numRows;
	}

	/**
	 *	Export a table to an external file.
	 */
	private void configureExportWriter(ResultInfo info)
		throws IOException, SQLException, Exception
	{
		if (!jobsRunning)
		{
			this.warnings.clear();
			this.errors.clear();
		}
		
		if (this.sqlTable != null)
		{
			info.setUpdateTable(new TableIdentifier(this.sqlTable));
		}

		if (this.encoding == null) this.encoding = Settings.getInstance().getDefaultDataEncoding();
		
		if (this.tableName != null)
		{
			this.exportWriter.setTableToUse(this.tableName);
		}

		BufferedWriter pw = null;
		final int buffSize = 64*1024;

		try
		{
			File f = new File(this.outputfile);
			this.fullOutputFileName = f.getAbsolutePath();
			String dir = f.getParent();
			// first try to open the file using the current encoding
			if (this.encoding != null)
			{
				try
				{
					OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f, this.append), EncodingUtil.cleanupEncoding(this.encoding));
					pw = new BufferedWriter(out, buffSize);
				}
				catch (UnsupportedEncodingException e)
				{
					pw = null;
					String msg = ResourceMgr.getString("ErrExportWrongEncoding");
					msg = StringUtil.replace(msg, "%encoding%", this.encoding);
					this.encoding = null;
					this.addWarning(msg);
				}
			}

			// if opening the file with an encoding failed, open the file
			// without encoding (thus using the default system encoding)
			if (pw == null)
			{
				pw = new BufferedWriter(new FileWriter(f,this.append), buffSize);
			}
			this.exportWriter.setOutput(pw);
			this.exportWriter.configureConverter();
		}
		catch (IOException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataExporter", "Error writing data file", e);
			throw e;
		}

		if (this.progressInterval > 0)
		{
			this.exportWriter.setRowMonitor(this.rowMonitor);
			this.exportWriter.setProgressInterval(this.progressInterval);
		}
		else if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			String msg = ResourceMgr.getString("MsgExportingData") + " " + this.fullOutputFileName;
			this.rowMonitor.setCurrentObject(msg, -1, -1);
			Thread.yield();
		}

		if (this.showProgressWindow)
		{
			if (this.progressPanel == null) createProgressPanel();
			this.exportWriter.setRowMonitor(this.progressPanel);
			this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolingRow"));
		}
	}

	public void closeProgress()
	{
		if (this.progressWindow != null)
		{
			this.progressWindow.setVisible(false);
			this.progressWindow.dispose();
			this.progressPanel = null;
		}
		if (this.rowMonitor != null)
		{
			this.rowMonitor.jobFinished();
		}
	}

	public boolean selectOutput(Window parent)
	{
		ExportFileDialog dialog = new ExportFileDialog(parent);
		dialog.setIncludeSqlInsert(true);
		dialog.setIncludeSqlUpdate(true);

		boolean result = dialog.selectOutput();
		if (result)
		{
			dialog.setExporterOptions(this);
		}
		return result;
	}

	public void exportTable(Window aParent, TableIdentifier table)
	{
		this.parentWindow = aParent;
		ResultInfo info = null;
		try
		{
			info = new ResultInfo(table, this.dbConn);
		}
		catch (SQLException e)
		{
			info = null;
		}
		ExportFileDialog dialog = new ExportFileDialog(aParent, info);

		dialog.setIncludeSqlInsert(true);
		dialog.setIncludeSqlUpdate(true);

		boolean result = dialog.selectOutput();
		boolean tableExport = false;
		
		if (result)
		{
			try
			{
				StringBuffer query = new StringBuffer(250);
				query.append("SELECT ");
				List cols = dialog.getColumnsToExport();
				if (cols != null)
				{
					for (int i=0; i < cols.size(); i++)
					{
						if (i > 0) query.append(", ");
						ColumnIdentifier col = (ColumnIdentifier)cols.get(i);
						query.append(col.getColumnName());
					}
					query.append(" FROM ");
					query.append(table.getTableExpression(this.dbConn));
					this.setSql(query.toString());
				}
				else
				{
					tableExport = true;
					this.addTableExportJob(dialog.getSelectedFilename(), table);
				}
				
				dialog.setExporterOptions(this);
				
				Frame parent = null;
				if (aParent instanceof Frame)
				{
					parent = (Frame)aParent;
				}
				// In order to initialize the resultInfo as accurate as 
				// possible, we use a table export when all columns are
				// selected. This way the column information will be retrieved
				// directly from the table definition and not from the 
				// ResultSetMetadata object which might not return 
				// the correct column types (e.g. in Postgres)
				// but for an XML export we want to have the types as 
				// exact as possible to enable creating the target table
				// later
				
				if (tableExport)
				{
					this.setShowProgressWindow(true);
					this.startExportJobs(parent);
				}
				else
				{
					this.startBackgroundThread();
					this.openProgressMonitor(parent, true);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("DataExporter.executeStatement()", "Could not export data", e);
				if (aParent != null)
				{
					WbSwingUtilities.showErrorMessage(aParent, ExceptionUtil.getDisplay(e));
				}
			}
		}
	}

	public void setOptions(ExportOptions options)
	{
		this.setEncoding(options.getEncoding());
		this.setDateFormat(options.getDateFormat());
		this.setTimestampFormat(options.getTimestampFormat());
		this.setEncoding(options.getEncoding());
		if (this.exportWriter != null) this.exportWriter.configureConverter();
	}

	public void setSqlOptions(SqlOptions sqlOptions)
	{
		if (sqlOptions.getCreateInsert())
		{
			this.setOutputTypeSqlInsert();
		}
		else if (sqlOptions.getCreateUpdate())
		{
			this.setOutputTypeSqlUpdate();
		}
		else if (sqlOptions.getCreateDeleteInsert())
		{
			this.setOutputTypeSqlDeleteInsert();
		}
		this.setIncludeCreateTable(sqlOptions.getCreateTable());
		this.setCommitEvery(sqlOptions.getCommitEvery());
		this.setTableName(sqlOptions.getAlternateUpdateTable());
		this.setKeyColumnsToUse(sqlOptions.getKeyColumns());
		this.exportWriter.configureConverter();
	}

	public void setXmlOptions(XmlOptions xmlOptions)
	{
		this.setOutputTypeXml();
		this.setUseCDATA(xmlOptions.getUseCDATA());
		this.setUseVerboseFormat(xmlOptions.getUseVerboseXml());
		this.exportWriter.configureConverter();
	}

	public void setHtmlOptions(HtmlOptions html)
	{
		this.setOutputTypeHtml();
		this.setCreateFullHtmlPage(html.getCreateFullPage());
		this.setHtmlTitle(html.getPageTitle());
		this.setEscapeHtml(html.getEscapeHtml());
		this.exportWriter.configureConverter();
	}

	public void setTextOptions(TextOptions text)
	{
		this.setOutputTypeText();
		this.setExportHeaders(text.getExportHeaders());
		this.setTextDelimiter(text.getTextDelimiter());
		this.setTextQuoteChar(text.getTextQuoteChar());
		this.setQuoteAlways(text.getQuoteAlways());
		this.setEscapeRange(text.getEscapeRange());
		this.setLineEnding(text.getLineEnding());
		this.exportWriter.configureConverter();
	}

	public boolean isIncludeCreateTable()
	{
		return includeCreateTable;
	}

	public void setIncludeCreateTable(boolean includeCreateTable)
	{
		this.includeCreateTable = includeCreateTable;
	}

	/**
	 * Getter for property keyColumnsToUse.
	 * @return Value of property keyColumnsToUse.
	 */
	public java.util.List getKeyColumnsToUse()
	{
		return keyColumnsToUse;
	}

	/**
	 * Setter for property keyColumnsToUse.
	 * @param keyColumnsToUse New value of property keyColumnsToUse.
	 */
	public void setKeyColumnsToUse(java.util.List keyColumnsToUse)
	{
		this.keyColumnsToUse = keyColumnsToUse;
	}

	/**
	 * Getter for property concatFunction.
	 * @return Value of property concatFunction.
	 */
	public java.lang.String getConcatFunction()
	{
		return concatFunction;
	}

	/**
	 * Setter for property concatFunction.
	 * @param func New value of property concatFunction.
	 */
	public void setConcatFunction(String func)
	{
		this.concatFunction = func;
		this.concatString = null;
	}

	public boolean getQuoteAlways()
	{
		return quoteAlways;
	}

	public void setQuoteAlways(boolean flag)
	{
		this.quoteAlways = flag;
	}

	public void setEscapeRange(CharacterRange range)
	{
		this.escapeRange = range;
	}

	public CharacterRange getEscapeRange()
	{
		return this.escapeRange;
	}

	public void setLineEnding(String ending)
	{
		if (ending != null) this.lineEnding = ending;
	}

	public String getLineEnding()
	{
		return this.lineEnding;
	}

	public boolean getUseVerboseFormat()
	{
		return verboseFormat;
	}

	public void setUseVerboseFormat(boolean flag)
	{
		this.verboseFormat = flag;
	}

	public boolean getWriteOracleControlFile()
	{
		return writeOracleControlFile;
	}
	
	public void setWriteOracleControlFile(boolean flag)
	{
		this.writeOracleControlFile = flag;
	}

}
