/*
 * DataExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JDialog;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.Committer;
import workbench.interfaces.ErrorReporter;
import workbench.interfaces.ProgressReporter;
import workbench.storage.SqlLiteralFormatter;
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
import workbench.storage.SqlLiteralFormatter;
import workbench.util.CharacterRange;
import workbench.util.EncodingUtil;
import workbench.util.MessageBuffer;
import workbench.util.NumberStringCache;
import workbench.util.QuoteEscapeType;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;


/**
 *
 * @author  support@sql-workbench.net
 */
public class DataExporter
	implements Interruptable, ErrorReporter, ProgressReporter, Committer
{
	/**
	 * Use a DBMS specific literals for BLOBs in SQL statements.
	 * @see #setBlobMode(String)
	 */
	public static final String BLOB_MODE_LITERAL = "dbms";
	
	/**
	 * Use ANSI literals for BLOBs in SQL statements.
	 * @see #setBlobMode(String)
	 */
	public static final String BLOB_MODE_ANSI = "ansi";
	
	/**
	 * Generate WB Specific {$blobfile=...} statements
	 * @see #setBlobMode(String)
	 */
	public static final String BLOB_MODE_FILE = "file";
	
	/**
	 * Export to SQL statements.
	 */
	public static final int EXPORT_SQL = 1;
	
	/**
	 * Export to plain text.
	 */
	public static final int EXPORT_TXT = 2;
	
	/**
	 * Export to XML file (WB specific).
	 */
	public static final int EXPORT_XML = 3;
	
	
	/**
	 * Export to HTML.
	 */
	public static final int EXPORT_HTML = 4;

	private WbConnection dbConn;
	private String sql;
	private String htmlTitle = null;
	
	// When compressing the output this holds the name of the archive.
	private String realOutputfile;
	
	private String outputfile;
	private String xsltFile = null;
	private String transformOutputFile = null;
	private int exportType;
	private boolean exportHeaders;
	private boolean includeCreateTable = false;
	private boolean continueOnError = true;

	private int sqlType = SqlRowDataConverter.SQL_INSERT;
	private boolean useCDATA = false;
	private CharacterRange escapeRange = null;
	private String lineEnding = "\n";
	private String tableName;
	private String sqlTable;
	private String encoding;
	private List<ColumnIdentifier> columnsToExport;

	private boolean clobAsFile = false;
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean quoteAlways = false;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";
	private String concatFunction = null;
	private int commitEvery = 0;

	private SimpleDateFormat dateFormatter = null;
	private	SimpleDateFormat dateTimeFormatter = null;
	private DecimalFormat numberFormatter = null;

	private boolean cleancr = false;
	private boolean append = false;
	private boolean escapeHtml = true;
	private boolean createFullHtmlPage = true;
	private boolean verboseFormat = true;

	private boolean showProgressWindow = false;
	private int progressInterval = ProgressReporter.DEFAULT_PROGRESS_INTERVAL;

	private ProgressPanel progressPanel;
	private JDialog progressWindow;
	private ExportJobEntry currentJob;
	private boolean cancelJobs = false;
	//private int pendingJobs = 0;
	private boolean jobsRunning = false;
	private RowActionMonitor rowMonitor;

	private List keyColumnsToUse;
	private String dateLiteralType = null;
	
	// The columns to be used for generating blob file names
	private List<String> blobIdCols;

	private MessageBuffer warnings = new MessageBuffer();
	private MessageBuffer errors = new MessageBuffer();
	private List<ExportJobEntry> jobQueue;
	private ExportWriter exportWriter;
	private Window parentWindow;
	private int tablesExported;
	private long totalRows;
	
	private boolean writeOracleControlFile = false;
	private boolean writeBcpFormatFile = false;
	private boolean compressOutput = false;

	private ZipOutputStream zipArchive;
	private ZipEntry zipEntry;

	private String blobMode = null;
	private QuoteEscapeType quoteEscape = QuoteEscapeType.none;

	/**
	 * Create a DataExporter for the specified connection.
	 */
	public DataExporter(WbConnection con)
	{
		this.dbConn = con;
		this.setExportHeaders(Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false));
	}

	protected void createProgressPanel()
	{
		progressPanel = new ProgressPanel(this);
		this.progressPanel.setFilename(this.outputfile);
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolStart"));
	}
	/**
	 * Open the progress monitor window.
	 * @param parent the window acting as the parent for the progress monitor
	 */
	protected void openProgressMonitor(Frame parent)
	{
		if (this.progressPanel == null) createProgressPanel();

		this.progressWindow = new JDialog(parent, true);
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setTitle(ResourceMgr.getString("MsgSpoolWindowTitle"));
		
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

	/**
	 * Define the format for date and timestamp literals
	 * when writing SQL statements. 
	 * 
	 * Valid values are <tt>jdbc,ansi,dbms</tt>
	 * 
	 * dbms selects the format approriate for the current dbms.
	 * It is the same as passing null
	 * 
	 * @param type the literal format to use
	 * @see workbench.storage.SqlLiteralFormatter#setProduct(String)
	 */
	public void setDateLiteralType(String type)
	{
		if (SqlLiteralFormatter.DBMS_DATE_LITERAL_TYPE.equalsIgnoreCase(type) || type == null)
		{
			this.dateLiteralType = SqlLiteralFormatter.DBMS_DATE_LITERAL_TYPE;
		}
		else
		{
			this.dateLiteralType = type.trim().toLowerCase();
		}
	}
  
	/**
	 * Return the type of date literals to be created when generating
	 * SQL statements. 
	 * @return the date literal type
	 * @see workbench.db.exporter.SqlExportWriter#configureConverter()
	 * @see workbench.storage.SqlLiteralFormatter
	 */
	public String getDateLiteralType()
  {
      return dateLiteralType;
  }
  
	/**
	 * Define how blobs should be handled during export.
	 * Modes allowed are 
	 * <ul>
	 *	<li>BLOB_MODE_LITERAL</li>
	 *  <li>BLOB_MODE_ANSI</li>
	 *  <li>BLOB_MODE_FILE</li>
	 * </ul>
	 * @param type the blob mode to be used. 
	 *        null means no special treatment (toString() will be called)
	 * @see #BLOB_MODE_LITERAL
	 * @see #BLOB_MODE_ANSI
	 * @see #BLOB_MODE_FILE
	 */
	public void setBlobMode(String type)
	{
		if (type == null || type.equalsIgnoreCase("none"))
		{
			this.blobMode = null;
		}
		else if (BLOB_MODE_LITERAL.equalsIgnoreCase(type) 
			|| BLOB_MODE_ANSI.equalsIgnoreCase(type) 
			|| BLOB_MODE_FILE.equalsIgnoreCase(type))
		{
			this.blobMode = type;
		}
		else
		{
			String msg = ResourceMgr.getString("ErrExpInvalidBlobType");
			msg = StringUtil.replace(msg, "%paramvalue%", type);
			this.addWarning(msg);
		}
	}
	
	/**
	 * Returns the currently selected mode for BLOB literals.
	 * @return the current type or null, if nothing was selected
	 */
	public String getBlobMode()
	{
		return this.blobMode;
	}
	
	public void setWriteClobAsFile(boolean flag) { this.clobAsFile = flag; }
	public boolean getWriteClobAsFile() { return clobAsFile; }
	
	public boolean getCompressOutput() { return this.compressOutput; }
	public void setCompressOutput(boolean flag) { this.compressOutput = flag; }
	
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
			this.jobQueue = new LinkedList<ExportJobEntry>();
		}
		ExportJobEntry job = new ExportJobEntry(anOutputfile, table, this.dbConn);
		this.jobQueue.add(job);
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.quoteEscape = type;
	}
	
	public QuoteEscapeType getQuoteEscaping()
	{
		return this.quoteEscape;
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
			this.addWarning(ResourceMgr.getString("MsgExportCancelled"));
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
	 * Define the columns whose values should be used
	 * for creating the blob files during export
	 * These columns must define a unique key!
	 */
	public void setBlobIdColumns(List<String> columns)
	{
		this.blobIdCols = columns;
	}
	
	List<String> getBlobIdColumns()
	{
		return blobIdCols;
	}
	
	/**
	 * Define the columns that should be exported
	 * This is only respected for the export of a DataStore, not
	 * for exporting a ResultSet
	 * 
	 * @param columns the columns to be exported
	 * @see #startExport(workbench.storage.DataStore)
	 */
	public void setColumnsToExport(List<ColumnIdentifier> columns)
	{
		this.columnsToExport = columns;
	}

	public List<ColumnIdentifier> getColumnsToExport()
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

	public void setContinueOnError(boolean aFlag) { this.continueOnError = aFlag; }

	/**
	 * Do not write any COMMITs to generated SQL scripts
	 */
	public void commitNothing()
	{
		this.commitEvery = Committer.NO_COMMIT_FLAG;
	}
	/**
	 * Set the number of statements after which to add a commit to
	 * generated SQL scripts. 
	 * @param count the number of statements after which a COMMIT should be added
	 */
	public void setCommitEvery(int count) 
	{ 
		this.commitEvery = count; 
	}
		
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
	 * 
	 * @param interval the new progress interval
	 */
	public void setReportInterval(int interval)
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
				this.addWarning(ResourceMgr.getFormattedString("MsgIllegalDateFormatIgnored", this.dateFormat));
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
				this.addWarning(ResourceMgr.getFormattedString("MsgIllegalDateFormatIgnored", this.dateTimeFormat));
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
		if (this.outputfile == null) return;
	}

	public String getOutputFilename() { return this.outputfile; }
	public String getFullOutputFilename() 
	{ 
		return this.realOutputfile; 
	}

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
			if (tables.size() == 1)
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
					openProgressMonitor(parent);
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
			this.currentJob = this.jobQueue.get(i);
			
			this.setOutputFilename(this.currentJob.getOutputFile());
			if (this.progressPanel != null)
			{
				this.progressPanel.setFilename(this.outputfile);
				this.progressPanel.setRowInfo(0);
				this.progressWindow.pack();
			}
			if (this.rowMonitor != null && this.currentJob.getTableName() != null)
			{
				StringBuilder msg = new StringBuilder(80);
				msg.append(this.currentJob.getTableName());
				msg.append(" [");
				msg.append(NumberStringCache.getNumberString(i+1));
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
				if (!this.continueOnError)
				{
					break;
				}
			}
			if (this.cancelJobs) break;
		}
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
			this.addError(ResourceMgr.getString("ErrExportExecute"));
			this.addError(ExceptionUtil.getDisplay(e));
			LogMgr.logError("DataExporter.startExport()", "Could not execute SQL statement: " + (currentJob != null ? currentJob.getQuerySql() : this.sql) + ", Error: " + ExceptionUtil.getDisplay(e), e);
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
			SqlUtil.closeAll(rs, stmt);
			if (!jobsRunning) this.closeProgress();
			if (busyControl) this.dbConn.setBusy(false);
		}
		return rows;
	}

	public boolean isSuccess() { return this.errors.getLength() == 0; }
	public boolean hasWarning() { return this.warnings.getLength() > 0; }
	public boolean hasError() { return this.errors.getLength() > 0; }

	public StringBuilder getErrors()
	{
		// this will clear the internal buffer of the errors!
		return this.errors.getBuffer();
	}

	public StringBuilder getWarnings()
	{
		// this will clear the internal buffer of the warnings!
		return this.warnings.getBuffer();
	}

	public void addWarning(String msg)
	{
		this.warnings.append(msg);
		this.warnings.appendNewLine();
	}

	public void addError(String msg)
	{
		this.errors.append(msg);
		this.errors.appendNewLine();
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
				// Some JDBC drivers to not report the column's data types
				// correctly through ResultSet.getMetaData(), so we are
				// using the table information returned by DatabaseMetaData
				// instead (if this is a table export)
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
			this.addError(e.getMessage());
			LogMgr.logError("DataExporter.startExport()", "SQL Error", e);
			throw e;
		}
		finally
		{
			exportFinished();
			try { rs.clearWarnings(); } catch (Throwable th) {}
			try { rs.close(); } catch (Throwable th) {}
		}
		long numRows = this.exportWriter.getNumberOfRecords();
		LogMgr.logInfo("DataExporter.startExport()", "Exported " + numRows + " rows to " + this.outputfile);
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
			this.addError(e.getMessage());
			LogMgr.logError("DataExporter.startExport()", "SQL Error", e);
			throw e;
		}
		finally
		{
			exportFinished();
		}
		long numRows = this.exportWriter.getNumberOfRecords();
		return numRows;
	}

	private void exportFinished()
	{
		if (this.exportWriter != null) this.exportWriter.exportFinished();
		if (this.zipArchive != null) 
		{
			try 
			{ 
				this.zipArchive.close();
				this.zipArchive = null;
				this.zipEntry = null;
			} 
			catch (Exception e) 
			{
				LogMgr.logError("DataExporter.exportFinished()", "Error closing ZIP archive", e);
			}
		}
		if (!jobsRunning) this.closeProgress();
	}
	
	/**
	 *	Export a table to an external file.
	 */
	private void configureExportWriter(ResultInfo info)
		throws IOException, SQLException, Exception
	{
		if (this.sqlTable != null)
		{
			info.setUpdateTable(new TableIdentifier(this.sqlTable));
		}

		if (this.encoding == null) this.encoding = Settings.getInstance().getDefaultDataEncoding();
		
		if (this.tableName != null)
		{
			this.exportWriter.setTableToUse(this.tableName);
		}

		try
		{
			WbFile f = new WbFile(this.outputfile);
			
			OutputStream out = null;
			if (this.getCompressOutput())
			{
				WbFile wf = new WbFile(f);
				String baseName = wf.getFileName();
				String dir = wf.getParent();
				File zipfile = new File(dir, baseName + ".zip");
				OutputStream zout = new FileOutputStream(zipfile);
				this.zipArchive = new ZipOutputStream(zout);
				this.zipArchive.setLevel(9);
				this.zipEntry = new ZipEntry(wf.getName());
				this.zipArchive.putNextEntry(zipEntry);
				out = this.zipArchive;
				this.realOutputfile = zipfile.getCanonicalPath();
			}
			else
			{
				out = new FileOutputStream(f, append);
				this.realOutputfile = f.getCanonicalPath();
			}
			Writer w = EncodingUtil.createWriter(out, this.encoding);
			
			this.exportWriter.setOutput(w);
			this.exportWriter.configureConverter();
		}
		catch (IOException e)
		{
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
			String msg = ResourceMgr.getString("MsgExportingData") + " " + this.realOutputfile;
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
		dialog.setQuerySql(this.sql, this.dbConn);
		dialog.setIncludeSqlInsert(true);
		//dialog.setIncludeSqlUpdate(true);

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
				StringBuilder query = new StringBuilder(250);
				query.append("SELECT ");
				List<ColumnIdentifier> cols = dialog.getColumnsToExport();
				if (cols != null)
				{
					boolean first = true;
					for (ColumnIdentifier col : cols)
					{
						if (!first) query.append(", ");
						else first = false;
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
					this.openProgressMonitor(parent);
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
		this.setDateLiteralType(sqlOptions.getDateLiteralType());
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
	public List getKeyColumnsToUse()
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
	public String getConcatFunction()
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

	public boolean getWriteBcpFormatFile()
	{
		return writeBcpFormatFile;
	}
	
	public void setWriteBcpFormatFile(boolean flag)
	{
		this.writeBcpFormatFile = flag;
	}
	
}
