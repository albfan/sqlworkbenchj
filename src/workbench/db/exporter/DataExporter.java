/*
 * DataExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.Committer;
import workbench.interfaces.ErrorReporter;
import workbench.interfaces.ProgressReporter;
import workbench.util.ExceptionUtil;
import workbench.gui.dialogs.export.ExportOptions;
import workbench.gui.dialogs.export.HtmlOptions;
import workbench.gui.dialogs.export.SpreadSheetOptions;
import workbench.gui.dialogs.export.SqlOptions;
import workbench.gui.dialogs.export.TextOptions;
import workbench.gui.dialogs.export.XmlOptions;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.InterruptableJob;
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
	implements InterruptableJob, ErrorReporter, ProgressReporter, Committer
{
	private WbConnection dbConn;
	private String pageTitle = null;

	// When compressing the output this holds the name of the archive.
	private WbFile realOutputfile;
	private WbFile outputfile;

	private String xsltFile = null;
	private String transformOutputFile = null;
	private ExportType exportType = ExportType.TEXT;
	private boolean exportHeaders;
	private String rowIndexColumnName = null;
	private boolean includeCreateTable = false;
	private boolean continueOnError = true;

	private boolean useCDATA = false;
	private String xmlVersion;
	private CharacterRange escapeRange = null;
	private String lineEnding = "\n";
	private String tableName;
	private String encoding;
	private List<ColumnIdentifier> columnsToExport;

	private boolean clobAsFile = false;
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean quoteAlways = false;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private String chrFunc = null;
	private String concatString = "||";
	private String concatFunction = null;
	private String filenameColumn = null;
	private int commitEvery = 0;

	private SimpleDateFormat dateFormatter = null;
	private	SimpleDateFormat dateTimeFormatter = null;
	private DecimalFormat numberFormatter = null;

	private boolean append = false;
	private boolean escapeHtml = true;
	private boolean createFullHtmlPage = true;
	private boolean verboseFormat = true;

	private int progressInterval = ProgressReporter.DEFAULT_PROGRESS_INTERVAL;

	private boolean cancelJobs = false;
	private RowActionMonitor rowMonitor;

	private List<String> keyColumnsToUse;
	private String dateLiteralType = null;

	// The columns to be used for generating blob file names
	private List<String> blobIdCols;

	private MessageBuffer warnings = new MessageBuffer();
	private MessageBuffer errors = new MessageBuffer();
	private List<ExportJobEntry> jobQueue;
	private boolean jobsRunning;
	private ExportWriter exportWriter;

	private int tablesExported;
	private long totalRows;

	private Set<ControlFileFormat> controlFiles = new HashSet<ControlFileFormat>();
	private boolean compressOutput = false;
	private List<DbExecutionListener> listener = new ArrayList<DbExecutionListener>();

	/**
	 * Should the ExportWriter create an output file, even if the result set
	 * for the export is empty?
	 */
	private boolean writeEmptyResults = true;

	private ZipOutputStream zipArchive;
	private ZipEntry zipEntry;

	private BlobMode blobMode = null;
	private QuoteEscapeType quoteEscape = QuoteEscapeType.none;

	/**
	 * Create a DataExporter for the specified connection.
	 *
	 * @param con The connection on which this Exporter should work on
	 */
	public DataExporter(WbConnection con)
	{
		this.dbConn = con;
		this.setExportHeaders(Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false));
	}

	public String getRowIndexColumnName()
	{
		return rowIndexColumnName;
	}

	public void setRowIndexColumnName(String colnme)
	{
		this.rowIndexColumnName = colnme;
	}

	public void addExecutionListener(DbExecutionListener l)
	{
		this.listener.add(l);
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

	public String getFilenameColumn()
	{
		return this.filenameColumn;
	}

	public void setFilenameColumn(String colname)
	{
		if (StringUtil.isBlank(colname))
		{
			this.filenameColumn  = null;
		}
		else
		{
			this.filenameColumn = colname.trim();
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
	 *
	 * @param type the blob mode to be used.
	 *        null means no special treatment (toString() will be called)
	 * @see workbench.db.exporter.BlobMode#getMode(java.lang.String)
	 */
	public void setBlobMode(String type)
	{
		this.blobMode = BlobMode.getMode(type);
		if (this.blobMode == null)
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
	public BlobMode getBlobMode()
	{
		return this.blobMode;
	}

	public void setWriteEmptyResults(boolean flag)
	{
		this.writeEmptyResults = flag;
	}
	public boolean writeEmptyResults()
	{
		return this.writeEmptyResults;
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

	public void addTableExportJob(File anOutputfile, TableIdentifier table)
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
		return true;
	}

	public void cancelCurrent()
	{
		if (this.exportWriter != null)
		{
			this.exportWriter.cancel();
		}
	}

	public void cancelExecution()
	{
		this.cancelJobs = true;
		cancelCurrent();
		this.addWarning(ResourceMgr.getString("MsgExportCancelled"));
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
	 *
	 * @param columns the ID columns to be used for the filename generation
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

	public ExportType getExportType()
	{
		return exportType;
	}

	public String getTypeDisplay()
	{
		if (exportType == null) return "";
		return exportType.toString();
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

	public void setXsltTransformation(String xsltFileName)
	{
		this.xsltFile = xsltFileName;
	}

	public String getXsltTransformation()
	{
		return this.xsltFile;
	}

	public void setXsltTransformationOutput(String aFilename)
	{
		this.transformOutputFile = aFilename;
	}

	public String getXsltTransformationOutput()
	{
		return this.transformOutputFile;
	}

	public void setExportHeaders(boolean aFlag)
	{
		this.exportHeaders = aFlag;
	}

	public boolean getExportHeaders()
	{
		return this.exportHeaders;
	}

	public void setCreateFullHtmlPage(boolean aFlag) { this.createFullHtmlPage = aFlag; }
	public boolean getCreateFullHtmlPage() { return this.createFullHtmlPage; }

	public void setEscapeHtml(boolean aFlag) { this.escapeHtml = aFlag; }
	public boolean getEscapeHtml() { return this.escapeHtml; }

	public void setTextDelimiter(String aDelimiter)
	{
		if (StringUtil.isNonBlank(aDelimiter)) this.delimiter = aDelimiter;
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

	public String getTimestampFormat()
	{
		return this.dateTimeFormat;
	}

	public SimpleDateFormat getTimestampFormatter()
	{
		return this.dateTimeFormatter;
	}

	private void createExportWriter()
	{
		switch (this.exportType)
		{
			case HTML:
				this.exportWriter = new HtmlExportWriter(this);
				break;
			case SQL_INSERT:
			case SQL_UPDATE:
			case SQL_DELETE_INSERT:
				this.exportWriter = new SqlExportWriter(this);
				break;
			case TEXT:
				this.exportWriter = new TextExportWriter(this);
				break;
			case XML:
				this.exportWriter = new XmlExportWriter(this);
				break;
			case XLS:
				this.exportWriter = new XlsExportWriter(this);
				break;
			case XLSX:
				this.exportWriter = new XlsXMLExportWriter(this);
				break;
			case ODS:
				this.exportWriter = new OdsExportWriter(this);
		}
	}

	public void setPageTitle(String aTitle)
	{
		this.pageTitle = aTitle;
	}

	public String getPageTitle()
	{
		return this.pageTitle;
	}

	public void setOutputType(ExportType type)
	{
		this.exportType = type;
		createExportWriter();
	}

	public String getFullOutputFilename()
	{
		if (this.realOutputfile == null) return null;
		return this.realOutputfile.getFullPath();
	}

	public void setConcatString(String aConcatString)
	{
		if (aConcatString == null)
		{
			return;
		}
		this.concatString = aConcatString;
		this.concatFunction = null;
	}

	public String getConcatString()
	{
		return this.concatString;
	}

	public void setChrFunction(String aFunc)
	{
		this.chrFunc = aFunc;
	}

	public String getChrFunction()
	{
		return this.chrFunc;
	}

	public void setDecimalSymbol(String aSymbol)
	{
		if (StringUtil.isNonBlank(aSymbol))
		{
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(aSymbol.charAt(0));
			numberFormatter = new DecimalFormat("0.#", symbols);
			numberFormatter.setGroupingUsed(false);
			numberFormatter.setMaximumFractionDigits(999);
		}
		else
		{
			numberFormatter = Settings.getInstance().createDefaultDecimalFormatter();
		}
	}

	public DecimalFormat getDecimalFormatter()
	{
		return this.numberFormatter;
	}

	
	public void addQueryJob(String query, WbFile outputFile)
	{
		ExportJobEntry entry = new ExportJobEntry(outputFile, query);
		this.jobQueue.add(entry);
	}

	public int getNumberExportedTables()
	{
		return this.tablesExported;
	}

	public long getTotalRows()
	{
		return this.totalRows;
	}

	public void setCurrentRow(int currentRow)
	{
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentRow(currentRow, -1);
		}
	}

	protected void fireExecutionStart()
	{
		for (DbExecutionListener l : listener)
		{
			l.executionStart(this.dbConn, this);
		}
	}

	protected void fireExecutionEnd()
	{
		for (DbExecutionListener l : listener)
		{
			l.executionEnd(this.dbConn, this);
		}
	}

	public void startBackgroundExport()
	{
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


	/**
	 * Start the export. This will execute the defined query
	 * and then write the result into the outputfile
	 *
	 * @return the number of rows exported
	 * @throws java.io.IOException if the output file could not be written
	 * @throws java.sql.SQLException if an error occurred during DB access
	 *
	 */
	public long startExport()
		throws IOException, SQLException
	{
		runJobs();
		return totalRows;
	}

	public void runJobs()
	{
		if (this.jobQueue == null) return;
		int count = this.jobQueue.size();

		this.cancelJobs = false;
		this.tablesExported = 0;
		this.totalRows = 0;

		if (this.exportWriter == null)
		{
			this.createExportWriter();
		}

		this.jobsRunning = true;

		fireExecutionStart();

		try
		{
			for (int i=0; i < count; i++)
			{
				ExportJobEntry job = this.jobQueue.get(i);

				if (this.rowMonitor != null && job.getTable() != null)
				{
					this.rowMonitor.setCurrentObject(job.getTable().getTableName(), i+1, count);
				}

				try
				{
					totalRows += runJob(job);
					this.tablesExported ++;
				}
				catch (Throwable th)
				{
					LogMgr.logError("DataExporter.runJobs()", "Error exporting data for [" + job.getQuerySql() + "] to file: " + this.outputfile, th);
					this.addError(th.getMessage());
					if (!this.continueOnError)
					{
						break;
					}
				}
				if (this.cancelJobs) break;
			}
		}
		finally
		{
			jobsRunning = false;
			fireExecutionEnd();
		}
	}

	private long runJob(ExportJobEntry job)
		throws IOException, SQLException
	{
		Statement stmt = this.dbConn.createStatementForQuery();
		ResultSet rs = null;
		long rows = 0;
		boolean busyControl = false;
		try
		{
			outputfile = job.getOutputFile();

			if (!this.dbConn.isBusy())
			{
				// only set the busy flag if the caller did not already do this!
				this.dbConn.setBusy(true);
				busyControl = true;
			}

			stmt.execute(job.getQuerySql());

			rs = stmt.getResultSet();
			rows = startExport(rs, job.getResultInfo(), job.getQuerySql());
		}
		catch (Exception e)
		{
			this.addError(ResourceMgr.getString("ErrExportExecute"));
			this.addError(ExceptionUtil.getDisplay(e));
			LogMgr.logError("DataExporter.startExport()", "Could not execute SQL statement: " + job.getQuerySql() + ", Error: " + ExceptionUtil.getDisplay(e), e);
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
			if (busyControl) this.dbConn.setBusy(false);
		}
		return rows;
	}

	public boolean isSuccess() { return this.errors.getLength() == 0; }
	public boolean hasWarning() { return this.warnings.getLength() > 0; }
	public boolean hasError() { return this.errors.getLength() > 0; }

	public CharSequence getErrors()
	{
		// this will clear the internal buffer of the errors!
		return this.errors.getBuffer();
	}

	public CharSequence getWarnings()
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

	public long exportResultSet(WbFile output, ResultSet rs, String generatingSql)
		throws IOException, SQLException, Exception
	{
		this.outputfile = output;
		return startExport(rs, null, generatingSql);
	}

	protected long startExport(ResultSet rs, ExportJobEntry job)
		throws IOException, SQLException, Exception
	{
		return startExport(rs, job.getResultInfo(), job.getQuerySql());
	}

	protected long startExport(ResultSet rs, ResultInfo info, String generatingSql)
		throws IOException, SQLException, Exception
	{
		try
		{
			ResultSetMetaData meta = rs.getMetaData();
			ResultInfo rsInfo = new ResultInfo(meta, this.dbConn);
			if (info != null)
			{
				// Some JDBC drivers to not report the column's data types
				// correctly through ResultSet.getMetaData(), so we are
				// using the table information returned by DatabaseMetaData
				// instead (if this is a table export)
				for (int i=0; i < info.getColumnCount(); i++)
				{
					int colIndex = rsInfo.findColumn(info.getColumnName(i));
					if (colIndex > -1)
					{
						info.setColumnClassName(i, rsInfo.getColumnClassName(i));
					}
				}
			}
			configureExportWriter();
			this.exportWriter.exportStarting();
			this.exportWriter.writeExport(rs, info == null ? rsInfo : info, generatingSql);
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

	public long startExport(WbFile output, DataStore ds)
		throws IOException, SQLException, Exception
	{
		try
		{
			this.outputfile = output;
			configureExportWriter();
			this.exportWriter.exportStarting();
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
		long rowsWritten = -1;
		if (this.exportWriter != null)
		{
			rowsWritten = this.exportWriter.exportFinished();
		}

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

		if (rowsWritten <= 0 && !writeEmptyResults && !append)
		{
			WbFile f = new WbFile(this.realOutputfile);
			LogMgr.logDebug("DataExporter.exportFinished()", "Empty result written. Deleting outputfile " + realOutputfile);
			f.delete();
		}

//		if (!jobsRunning) this.closeProgress();
	}

	/**
	 *	Export a table to an external file.
	 */
	private void configureExportWriter()
		throws IOException, SQLException, Exception
	{
		if (this.encoding == null) this.encoding = Settings.getInstance().getDefaultDataEncoding();

		if (this.tableName != null)
		{
			this.exportWriter.setTableToUse(this.tableName);
		}

		try
		{
			exportWriter.setOutputFile(outputfile);
			if (exportWriter.managesOutput())
			{
				realOutputfile = outputfile;
			}
			else
			{
				OutputStream out = null;
				if (this.getCompressOutput())
				{
					String baseName = outputfile.getFileName();
					String dir = outputfile.getParent();
					this.realOutputfile = new WbFile(dir, baseName + ".zip");
					OutputStream zout = new FileOutputStream(realOutputfile);
					this.zipArchive = new ZipOutputStream(zout);
					this.zipArchive.setLevel(9);
					this.zipEntry = new ZipEntry(outputfile.getName());
					this.zipArchive.putNextEntry(zipEntry);
					out = this.zipArchive;
				}
				else
				{
					out = new FileOutputStream(outputfile, append);
					this.realOutputfile = outputfile;
				}
				Writer w = EncodingUtil.createWriter(out, this.encoding);

				this.exportWriter.setOutputWriter(w);
			}
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
			this.setOutputType(ExportType.SQL_INSERT);
		}
		else if (sqlOptions.getCreateUpdate())
		{
			this.setOutputType(ExportType.SQL_UPDATE);
		}
		else if (sqlOptions.getCreateDeleteInsert())
		{
			this.setOutputType(ExportType.SQL_DELETE_INSERT);
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
		this.setOutputType(ExportType.XML);
		this.setUseCDATA(xmlOptions.getUseCDATA());
		this.setUseVerboseFormat(xmlOptions.getUseVerboseXml());
		this.exportWriter.configureConverter();
	}

	public void setHtmlOptions(HtmlOptions html)
	{
		this.setOutputType(ExportType.HTML);
		this.setCreateFullHtmlPage(html.getCreateFullPage());
		this.setPageTitle(html.getPageTitle());
		this.setEscapeHtml(html.getEscapeHtml());
		this.exportWriter.configureConverter();
	}

	public void setTextOptions(TextOptions text)
	{
		this.setOutputType(ExportType.TEXT);
		this.setExportHeaders(text.getExportHeaders());
		this.setTextDelimiter(text.getTextDelimiter());
		this.setTextQuoteChar(text.getTextQuoteChar());
		this.setQuoteAlways(text.getQuoteAlways());
		this.setEscapeRange(text.getEscapeRange());
		this.setDecimalSymbol(text.getDecimalSymbol());
		this.setLineEnding(text.getLineEnding());
		this.exportWriter.configureConverter();
	}

	public void setXlsXOptions(SpreadSheetOptions xlsOptions)
	{
		if (xlsOptions != null)
		{
			setOutputType(ExportType.XLSX);
			setSpreadsheetOptions(xlsOptions);
		}
	}

	public void setXlsOptions(SpreadSheetOptions xlsOptions)
	{
		if (xlsOptions != null)
		{
			setOutputType(ExportType.XLS);
			setSpreadsheetOptions(xlsOptions);
		}
	}

	public void setOdsOptions(SpreadSheetOptions odsOptions)
	{
		setOutputType(ExportType.ODS);
		setSpreadsheetOptions(odsOptions);
	}

	public void setSpreadsheetOptions(SpreadSheetOptions odsOptions)
	{
		this.setPageTitle(odsOptions.getPageTitle());
		this.setExportHeaders(odsOptions.getExportHeaders());
		this.exportWriter.configureConverter();
	}

	public boolean isIncludeCreateTable()
	{
		return includeCreateTable;
	}

	public void setIncludeCreateTable(boolean flag)
	{
		this.includeCreateTable = flag;
	}

	/**
	 * Getter for property keyColumnsToUse.
	 * @return Value of property keyColumnsToUse.
	 */
	public List<String> getKeyColumnsToUse()
	{
		return keyColumnsToUse;
	}

	/**
	 * Setter for property keyColumnsToUse.
	 * @param keyColumnsToUse New value of property keyColumnsToUse.
	 */
	public void setKeyColumnsToUse(List<String> keyCols)
	{
		this.keyColumnsToUse = keyCols;
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

	public void setXMLVersion(String version)
	{
		xmlVersion = version;
	}

	public String getXMLVersion()
	{
		return xmlVersion;
	}

	public boolean getUseVerboseFormat()
	{
		return verboseFormat;
	}

	public void setUseVerboseFormat(boolean flag)
	{
		this.verboseFormat = flag;
	}

	public Set<ControlFileFormat> getControlFileFormats()
	{
		return Collections.unmodifiableSet(controlFiles);
	}

	public void addControlFileFormat(ControlFileFormat format)
	{
		this.controlFiles.add(format);
	}

	public void addControlFileFormats(Set<ControlFileFormat> formats)
	{
		if (formats == null) return;
		this.controlFiles.addAll(formats);
	}

}
