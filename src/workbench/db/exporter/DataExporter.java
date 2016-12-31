/*
 * DataExporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import workbench.interfaces.Committer;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.ErrorReporter;
import workbench.interfaces.InterruptableJob;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.dialogs.export.ExportOptions;
import workbench.gui.dialogs.export.HtmlOptions;
import workbench.gui.dialogs.export.SpreadSheetOptions;
import workbench.gui.dialogs.export.SqlOptions;
import workbench.gui.dialogs.export.TextOptions;
import workbench.gui.dialogs.export.XmlOptions;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.SqlLiteralFormatter;

import workbench.util.CharacterEscapeType;
import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.MessageBuffer;
import workbench.util.QuoteEscapeType;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;
import workbench.util.WbFile;
import workbench.util.WbNumberFormatter;
import workbench.util.WbThread;

/**
 * A class to export data from the database into an external file.
 *
 * This class provides the runtime environment to export data. It uses different ExportWriters
 * and RowDataConverters to produce the actual output files.
 *
 * Data can be exported in two ways: either a complete export of a table using
 * {@link #addTableExportJob(java.io.File, workbench.db.TableIdentifier) } or by
 * specifying a SQL query where the generated ResultSet should be exported
 * using {@link #addQueryJob(java.lang.String, workbench.util.WbFile, java.lang.String)  }
 *
 * @author  Thomas Kellerer
 */
public class DataExporter
  implements InterruptableJob, ErrorReporter, ProgressReporter, Committer,
             ExportOptions, TextOptions
{

  private WbConnection dbConn;
  private String pageTitle;

  // When compressing the output this holds the name of the archive.
  private WbFile realOutputfile;

  private WbFile outputfile;
  private String xsltFile;
  private Map<String, String> xsltParameters;

  private String transformOutputFile;
  private ExportType exportType = ExportType.TEXT;
  private String mergeType;
  private boolean exportHeaders;
  private String rowIndexColumnName;
  private boolean includeCreateTable;
  private boolean continueOnError;
  private boolean useCDATA;
  private String xmlVersion;
  private CharacterRange escapeRange;
  private String lineEnding = "\n";
  private String tableName;
  private String encoding;
  private List<ColumnIdentifier> columnsToExport;
  private boolean clobAsFile;
  private String delimiter = "\t";
  private String quoteChar;
  private boolean quoteAlways;
  private String chrFunc;
  private String concatString = "||";
  private String concatFunction;
  private String filenameColumn;
  private int commitEvery = 0;
  private boolean useSchemaInSql;

  private boolean includeIdentityCols;
  private boolean includeReadOnlyCols;

  private InfinityLiterals infinityLiterals = InfinityLiterals.PG_LITERALS;

  private String dateFormat;
  private String timeFormat;
  private String dateTimeFormat;
  private SimpleDateFormat timeFormatter;
  private WbDateFormatter dateFormatter;
  private WbDateFormatter dateTimeFormatter;

  private WbNumberFormatter numberFormatter;
  private boolean append;
  private int targetSheetIndex; // for XLS and XLSX only
  private String targetSheetName; // for XLS and XLSX only
  private boolean escapeHtml = true;
  private String htmlHeading;
  private String htmlTrailer;
  private boolean createFullHtmlPage = true;
  private boolean verboseFormat = true;
  private int progressInterval = ProgressReporter.DEFAULT_PROGRESS_INTERVAL;
  private boolean cancelJobs;
  private RowActionMonitor rowMonitor;
  private List<String> keyColumnsToUse;
  private String dateLiteralType;

  // The columns to be used for generating blob file names
  private List<String> blobIdCols;

  private MessageBuffer warnings = new MessageBuffer();
  private MessageBuffer errors = new MessageBuffer();
  private List<ExportJobEntry> jobQueue;
  private ExportWriter exportWriter;
  private int tablesExported;
  private long totalRows;
  private Set<ControlFileFormat> controlFiles = EnumSet.noneOf(ControlFileFormat.class);
  private boolean compressOutput;
  private List<DbExecutionListener> listener = new ArrayList<>();
  private ExportDataModifier modifier;
  private boolean includeColumnComments;
  private String nullString;

  private int maxBlobFilesPerDir = -1;
  private boolean trimCharData;
  private boolean quoteHeader;
  private boolean quoteNulls;

  private Point dataOffset;


  /**
   * Toggles an additional sheet for Spreedsheet exports
   */
  private boolean appendInfoSheet;

  /**
   * Enables/Disables automatic sizing of spreadsheet columns
   */
  private boolean optimizeCols;

  /**
   * Enables an auto-filter for ODS and Excel XML exports
   */
  private boolean enableAutoFilter;

  /**
   * Enables the "freeze" of the header row for spreadsheet exports
   */
  private boolean enableFixedHeader;

  /**
   * Should the ExportWriter create an output file, even if the result set for the export is empty?
   */
  private boolean writeEmptyResults = true;

  private ZipOutputStream zipArchive;
  private ZipEntry zipEntry;
  private BlobMode blobMode;
  private QuoteEscapeType quoteEscape = QuoteEscapeType.none;
  private CharacterEscapeType escapeType = CharacterEscapeType.unicode;

  /**
   * Create a DataExporter for the specified connection.
   *
   * @param con The connection on which this Exporter should work on
   */
  public DataExporter(WbConnection con)
  {
    this.dbConn = con;
    this.jobQueue = new ArrayList<>();
    this.useSchemaInSql = Settings.getInstance().getIncludeOwnerInSqlExport();
    this.numberFormatter = Settings.getInstance().createDefaultDecimalFormatter(0);
    this.setExportHeaders(Settings.getInstance().getBoolProperty("workbench.export.text.default.header", false));
    if (con != null)
    {
      ConnectionProfile profile = con.getProfile();
      if (profile != null)
      {
        this.trimCharData = profile.getTrimCharData();
      }
    }
  }

  @Override
  public void setEscapeType(CharacterEscapeType type)
  {
    this.escapeType = type;
  }

  @Override
  public CharacterEscapeType getEscapeType()
  {
    return escapeType;
  }

  public boolean getQuoteHeader()
  {
    return quoteHeader;
  }

  public void setQuoteHeader(boolean flag)
  {
    this.quoteHeader = flag;
  }

  public boolean getQuoteNulls()
  {
    return quoteNulls;
  }

  public void setQuoteNulls(boolean flag)
  {
    this.quoteNulls = flag;
  }

  public boolean getIncludeIdentityCols()
  {
    return includeIdentityCols;
  }

  public void setIncludeIdentityCols(boolean flag)
  {
    this.includeIdentityCols = flag;
  }

  public boolean getIncludeReadOnlyCols()
  {
    return includeReadOnlyCols;
  }

  public void setIncludeReadOnlyCols(boolean flag)
  {
    this.includeReadOnlyCols = flag;
  }

  public boolean getTrimCharData()
  {
    return trimCharData;
  }

  public void setTrimCharData(boolean trim)
  {
    this.trimCharData = trim;
  }


  public String getMergeType()
  {
    return mergeType;
  }

  public void setMergeType(String type)
  {
    this.mergeType = type;
  }

  public boolean getEnableFixedHeader()
  {
    return enableFixedHeader;
  }

  public Point getSpreadSheetOffset()
  {
    return dataOffset;
  }

  public void setSpreadSheetOffset(Point location)
  {
    dataOffset = location;
  }

  @Override
  public String getNullString()
  {
    return nullString;
  }

  @Override
  public void setNullString(String nullValue)
  {
    this.nullString = nullValue;
  }

  public void setEnableFixedHeader(boolean flag)
  {
    this.enableFixedHeader = flag;
  }

  public boolean getEnableAutoFilter()
  {
    return enableAutoFilter;
  }

  public void setEnableAutoFilter(boolean flag)
  {
    this.enableAutoFilter = flag;
  }

  public boolean getAppendInfoSheet()
  {
    return appendInfoSheet;
  }

  public void setAppendInfoSheet(boolean flag)
  {
    this.appendInfoSheet = flag;
  }

  public boolean getOptimizeSpreadsheetColumns()
  {
    return optimizeCols;
  }

  public void setOptimizeSpreadsheetColumns(boolean flag)
  {
    this.optimizeCols = flag;
  }

  public void setIncludeColumnComments(boolean flag)
  {
    this.includeColumnComments = flag;
  }

  public boolean getIncludeColumnComments()
  {
    return includeColumnComments;
  }

  public String getRowIndexColumnName()
  {
    return rowIndexColumnName;
  }

  public ExportDataModifier getDataModifier()
  {
    return modifier;
  }

  public void setDataModifier(ExportDataModifier mod)
  {
    this.modifier = mod;
  }

  public void setRowIndexColumnName(String colnme)
  {
    this.rowIndexColumnName = colnme;
  }

  public void addExecutionListener(DbExecutionListener l)
  {
    this.listener.add(l);
  }

  public boolean getUseSchemaInSql()
  {
    return useSchemaInSql;
  }

  public void setUseSchemaInSql(boolean flag)
  {
    this.useSchemaInSql = flag;
  }

  public void setInfinityLiterals(InfinityLiterals literals)
  {
    this.infinityLiterals = literals;
  }

  public InfinityLiterals getInfinityLiterals()
  {
    return infinityLiterals;
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
   * @see workbench.storage.SqlLiteralFormatter#setProduct(workbench.db.WbConnection)
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
      this.filenameColumn = null;
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
  public void setBlobMode(BlobMode type)
  {
    this.blobMode = type;
  }

  /**
   * Returns the currently selected mode for BLOB literals.
   * @return the current type or null, if nothing was selected
   */
  @Override
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

  public void setWriteClobAsFile(boolean flag)
  {
    this.clobAsFile = flag;
  }

  public boolean getWriteClobAsFile()
  {
    return clobAsFile;
  }

  public boolean getCompressOutput()
  {
    return this.compressOutput;
  }

  public void setCompressOutput(boolean flag)
  {
    this.compressOutput = flag;
  }

  public void addTableExportJob(File anOutputfile, TableIdentifier table)
    throws SQLException
  {
    addTableExportJob(anOutputfile, table, null);
  }

  public void addTableExportJob(File anOutputfile, TableIdentifier table, String where)
    throws SQLException
  {
    ExportJobEntry job = new ExportJobEntry(anOutputfile, table, where, this.dbConn);
    this.jobQueue.add(job);
  }

  public void setQuoteEscaping(QuoteEscapeType type)
  {
    this.quoteEscape = type;
  }

  @Override
  public QuoteEscapeType getQuoteEscaping()
  {
    return this.quoteEscape;
  }

  public WbConnection getConnection()
  {
    return this.dbConn;
  }

  @Override
  public boolean confirmCancel()
  {
    return true;
  }

  @Override
  public void cancelCurrent()
  {
    if (this.exportWriter != null)
    {
      this.exportWriter.cancel();
    }
  }

  @Override
  public void cancelExecution()
  {
    this.cancelJobs = true;
    cancelCurrent();
    this.addWarning(ResourceMgr.getString("MsgExportCancelled"));
  }

  public void setTableName(String aTablename)
  {
    this.tableName = aTablename;
  }

  public String getTableName()
  {
    return this.tableName;
  }

  /**
   * Return the table name to be used for this export.
   *
   * @return the table name or an empty string if no table can be identified.
   */
  public String getTableNameToUse()
  {
    if (this.tableName != null) return this.tableName;
    ResultInfo resultInfo = exportWriter.getConverter().getResultInfo();
    if (resultInfo != null)
    {
      TableIdentifier tbl = resultInfo.getUpdateTable();
      if (tbl != null)
      {
        return tbl.getTableName();
      }
    }
    return "";
  }

  @Override
  public void setEncoding(String enc)
  {
    this.encoding = enc;
  }

  @Override
  public String getEncoding()
  {
    return this.encoding;
  }

  public void setRowMonitor(RowActionMonitor monitor)
  {
    this.rowMonitor = monitor;
  }

  public void setMaxLobFilesPerDirectory(int maxFiles)
  {
    maxBlobFilesPerDir = maxFiles;
  }

  public int getMaxLobFilesPerDirectory()
  {
    return maxBlobFilesPerDir;
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

  public List<ColumnIdentifier> getColumnsToExport()
  {
    return this.columnsToExport;
  }

  public void setUseCDATA(boolean flag)
  {
    this.useCDATA = flag;
  }

  public boolean getUseCDATA()
  {
    return this.useCDATA;
  }

  public void setAppendToFile(boolean aFlag)
  {
    this.append = aFlag;
  }

  public boolean getAppendToFile()
  {
    return this.append;
  }

  public int getTargetSheetIndex()
  {
    return this.targetSheetIndex;
  }

  public void setTargetSheetIndex(int index)
  {
    this.targetSheetIndex = index;
  }

  public String getTargetSheetName()
  {
    return targetSheetName;
  }

  public void setTargetSheetName(String name)
  {
    this.targetSheetName = name;
  }

  public void setContinueOnError(boolean aFlag)
  {
    this.continueOnError = aFlag;
  }

  /**
   * Do not write any COMMITs to generated SQL scripts
   */
  @Override
  public void commitNothing()
  {
    this.commitEvery = Committer.NO_COMMIT_FLAG;
  }

  /**
   * Set the number of statements after which to add a commit to
   * generated SQL scripts.
   * @param count the number of statements after which a COMMIT should be added
   */
  @Override
  public void setCommitEvery(int count)
  {
    this.commitEvery = count;
  }

  public int getCommitEvery()
  {
    return this.commitEvery;
  }

  public ExportType getExportType()
  {
    return exportType;
  }

  public String getTypeDisplay()
  {
    if (exportType == null)
    {
      return "";
    }
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
  @Override
  public void setReportInterval(int interval)
  {
    if (interval <= 0)
    {
      this.progressInterval = 0;
    }
    else
    {
      this.progressInterval = interval;
    }
  }

  public void setXsltParameters(Map<String, String> params)
  {
    this.xsltParameters = params == null ? new HashMap<>(0) : params;
  }

  public Map<String, String> getXsltParameters()
  {
    return xsltParameters;
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

  /**
   * Set if the (text) export should contain header row.
   */
  @Override
  public final void setExportHeaders(boolean aFlag)
  {
    this.exportHeaders = aFlag;
  }

  /**
   * Returns if the (text) export contains a header row.
   * @return true, if the header row should be written
   */
  @Override
  public boolean getExportHeaders()
  {
    return this.exportHeaders;
  }

  public void setHtmlHeading(String heading)
  {
    this.htmlHeading = heading;
  }

  public String getHtmlHeading()
  {
    return this.htmlHeading;
  }

  public void setHtmlTrailer(String html)
  {
    this.htmlTrailer = html;
  }

  public String getHtmlTrailer()
  {
    return this.htmlTrailer;
  }

  public void setCreateFullHtmlPage(boolean aFlag)
  {
    this.createFullHtmlPage = aFlag;
  }

  public boolean getCreateFullHtmlPage()
  {
    return this.createFullHtmlPage;
  }

  public void setEscapeHtml(boolean aFlag)
  {
    this.escapeHtml = aFlag;
  }

  public boolean getEscapeHtml()
  {
    return this.escapeHtml;
  }

  @Override
  public void setTextDelimiter(String aDelimiter)
  {
    if (StringUtil.isNonBlank(aDelimiter))
    {
      this.delimiter = aDelimiter;
    }
  }

  @Override
  public String getTextDelimiter()
  {
    return this.delimiter;
  }

  @Override
  public void setTextQuoteChar(String aQuote)
  {
    this.quoteChar = aQuote;
  }

  @Override
  public String getTextQuoteChar()
  {
    return this.quoteChar;
  }


  public void setTimeFormat(String aFormat)
  {
    timeFormat = StringUtil.isBlank(aFormat) ? null : aFormat;
    try
    {
      timeFormatter = new SimpleDateFormat(timeFormat == null ? Settings.getInstance().getDefaultTimeFormat() : timeFormat);
    }
    catch (IllegalArgumentException i)
    {
      this.addWarning(ResourceMgr.getFormattedString("MsgIllegalDateFormatIgnored", timeFormat));
      timeFormatter = null;
    }
  }

  public SimpleDateFormat getTimeFormatter()
  {
    return timeFormatter;
  }

  @Override
  public void setDateFormat(String aFormat)
  {
    dateFormat = StringUtil.isBlank(aFormat) ? null : aFormat;
    try
    {
      dateFormatter = new WbDateFormatter(this.dateFormat == null ? Settings.getInstance().getDefaultDateFormat() : dateFormat);
    }
    catch (IllegalArgumentException i)
    {
      this.addWarning(ResourceMgr.getFormattedString("MsgIllegalDateFormatIgnored", this.dateFormat));
      dateFormatter = null;
    }
  }

  public WbDateFormatter getDateFormatter()
  {
    return dateFormatter;
  }

  @Override
  public String getDateFormat()
  {
    return this.dateFormat;
  }

  @Override
  public void setTimestampFormat(String aFormat)
  {
    dateTimeFormat = StringUtil.isBlank(aFormat) ? null : aFormat;
    try
    {
      dateTimeFormatter = new WbDateFormatter(dateTimeFormat == null ? Settings.getInstance().getDefaultTimestampFormat() : dateTimeFormat);
    }
    catch (Exception e)
    {
      this.addWarning(ResourceMgr.getFormattedString("MsgIllegalDateFormatIgnored", this.dateTimeFormat));
      dateTimeFormatter = null;
    }
  }

  @Override
  public String getTimestampFormat()
  {
    return dateTimeFormat;
  }

  public WbDateFormatter getTimestampFormatter()
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
      case SQL_DELETE:
      case SQL_MERGE:
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
        this.exportWriter = new XlsxExportWriter(this);
        break;
      case XLSM:
        this.exportWriter = new XlsXMLExportWriter(this);
        break;
      case ODS:
        this.exportWriter = new OdsExportWriter(this);
        break;
      case JSON:
        this.exportWriter = new JsonExportWriter(this);
        break;
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

  /**
   * Returns the name of "real" outputfile. If the output is compressed,
   * this is the name of the non-compressed file.
   */
  public WbFile getOutputFile()
  {
    return outputfile;
  }

  public String getFullOutputFilename()
  {
    if (this.realOutputfile == null)
    {
      return null;
    }
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

  @Override
  public void setDecimalSymbol(String symbol)
  {
    if (StringUtil.isNonBlank(symbol))
    {
      numberFormatter = new WbNumberFormatter(symbol.charAt(0));
    }
  }

  public void setDecimalDigits(int digits, String symbol, boolean fixedLength)
  {
    if (StringUtil.isNonBlank(symbol))
    {
      this.numberFormatter = new WbNumberFormatter(digits, symbol.charAt(0), fixedLength);
    }
  }

  public WbNumberFormatter getDecimalFormatter()
  {
    return this.numberFormatter;
  }

  @Override
  public String getDecimalSymbol()
  {
    if (numberFormatter == null)
    {
      return null;
    }
    return Character.toString(numberFormatter.getDecimalSymbol());
  }

  public void addQueryJob(String query, WbFile outputFile, String where)
  {
    ExportJobEntry entry = new ExportJobEntry(outputFile, query, where, getConnection());
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
      @Override
      public void run()
      {
        try
        {
          runJobs();
        }
        catch (Throwable th)
        {
        }
      }
    };
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  /**
   * Start the export. This will execute all job definitions and
   * write the results to the specified file(s)
   *
   * @return the number of rows exported
   * @throws java.io.IOException if the output file could not be written
   * @throws java.sql.SQLException if an error occurred during DB access
   *
   * @see #addQueryJob(java.lang.String, workbench.util.WbFile, java.lang.String)
   * @see #addTableExportJob(java.io.File, workbench.db.TableIdentifier)
   */
  public long startExport()
    throws IOException, SQLException
  {
    runJobs();
    return totalRows;
  }

  public void runJobs()
  {
    if (this.jobQueue == null)
    {
      return;
    }
    int count = this.jobQueue.size();

    this.cancelJobs = false;
    this.tablesExported = 0;
    this.totalRows = 0;

    if (this.exportWriter == null)
    {
      this.createExportWriter();
    }

    fireExecutionStart();

    try
    {
      for (int i = 0; i < count; i++)
      {
        ExportJobEntry job = this.jobQueue.get(i);

        if (this.rowMonitor != null && job.getTable() != null)
        {
          this.rowMonitor.setCurrentObject(job.getTable().getTableName(), i + 1, count);
        }

        if (append && exportWriter.managesOutput() && (this.pageTitle == null || count > 1))
        {
          this.pageTitle = job.getTable().getTableExpression(this.dbConn);
        }

        try
        {
          totalRows += runJob(job);
          this.tablesExported++;
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
        if (this.cancelJobs)
        {
          break;
        }
      }
      if (this.rowMonitor != null)
      {
        this.rowMonitor.jobFinished();
      }
    }
    finally
    {
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

      if (!this.dbConn.getAutoCommit() && dbConn.selectStartsTransaction())
      {
        dbConn.rollbackSilently();
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      if (busyControl)
      {
        this.dbConn.setBusy(false);
      }
    }
    return rows;
  }

  public boolean isSuccess()
  {
    return this.errors.getLength() == 0;
  }

  public boolean hasWarning()
  {
    return this.warnings.getLength() > 0;
  }

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

  @Override
  public void addWarning(String msg)
  {
    this.warnings.append(msg);
    this.warnings.appendNewLine();
  }

  @Override
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

  protected long startExport(ResultSet rs, ResultInfo info, String generatingSql)
    throws IOException, SQLException, Exception
  {
    try
    {
      ResultSetMetaData meta = rs.getMetaData();
      ResultInfo rsInfo = new ResultInfo(meta, this.dbConn);
      if (info != null)
      {
        // Some JDBC drivers do not report the column's data types
        // correctly through ResultSet.getMetaData(), so we are
        // using the table information returned by DatabaseMetaData
        // instead (if this is a table export)
        for (int i = 0; i < info.getColumnCount(); i++)
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
    finally
    {
      exportFinished();
      SqlUtil.closeResult(rs);
    }
    long numRows = this.exportWriter.getNumberOfRecords();
    String msg = "Exported " + numRows + " rows to " + this.outputfile;
    String details = exportWriter.getConverter().getTargetFileDetails();
    if (details != null)
    {
      msg += " (" + details + ")";
    }
    LogMgr.logInfo("DataExporter.startExport()", msg);
    return numRows;
  }

  public long startExport(WbFile output, DataStore ds, List<ColumnIdentifier> columnsToExport)
    throws IOException, SQLException, Exception
  {
    try
    {
      this.outputfile = output;
      configureExportWriter();
      this.exportWriter.exportStarting();
      this.exportWriter.writeExport(ds, columnsToExport);
    }
    catch (Exception e)
    {
      this.addError(ExceptionUtil.getDisplay(e));
      LogMgr.logError("DataExporter.startExport()", "Could not export data", e);
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

//    if (!jobsRunning) this.closeProgress();
  }

  /**
   *  Export a table to an external file.
   */
  private void configureExportWriter()
    throws IOException, SQLException, Exception
  {
    if (this.encoding == null)
    {
      this.encoding = Settings.getInstance().getDefaultDataEncoding();
    }

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
          if (realOutputfile.equals(outputfile))
          {
            throw new IOException(ResourceMgr.getString("ErrExportWrongZip"));
          }
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
    this.setNullString(options.getNullString());
    if (this.exportWriter != null)
    {
      this.exportWriter.configureConverter();
    }
  }

  public void setSqlOptions(SqlOptions sqlOptions)
  {
    this.setOutputType(sqlOptions.getExportType());
    this.setMergeType(sqlOptions.getMergeType());
    this.setIncludeCreateTable(sqlOptions.getCreateTable());
    this.setCommitEvery(sqlOptions.getCommitEvery());
    this.setTableName(sqlOptions.getAlternateUpdateTable());
    this.setKeyColumnsToUse(sqlOptions.getKeyColumns());
    this.setDateLiteralType(sqlOptions.getDateLiteralType());
    this.setBlobMode(sqlOptions.getBlobMode());
    this.setIncludeIdentityCols(!sqlOptions.ignoreIdentityColumns());
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
    setQuoteEscaping(text.getQuoteEscaping());
    controlFiles.clear();
    addControlFileFormats(text.getControlFiles());
  }

  public void setXlsXOptions(SpreadSheetOptions xlsOptions)
  {
    if (xlsOptions != null)
    {
      setOutputType(ExportType.XLSX);
      setSpreadsheetOptions(xlsOptions);
    }
  }

  public void setXlsMOptions(SpreadSheetOptions xlsMOptions)
  {
    if (xlsMOptions != null)
    {
      setOutputType(ExportType.XLSM);
      setSpreadsheetOptions(xlsMOptions);
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

  public void setSpreadsheetOptions(SpreadSheetOptions options)
  {
    setPageTitle(options.getPageTitle());
    setExportHeaders(options.getExportHeaders());
    setEnableAutoFilter(options.getCreateAutoFilter());
    setEnableFixedHeader(options.getCreateFixedHeaders());
    setAppendInfoSheet(options.getCreateInfoSheet());
    setOptimizeSpreadsheetColumns(options.getOptimizeColumns());
    exportWriter.configureConverter();
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
    if (keyColumnsToUse == null) return null;
    return Collections.unmodifiableList(keyColumnsToUse);
  }

  /**
   * Setter for property keyColumnsToUse.
   * @param keyCols New value of property keyColumnsToUse.
   */
  public void setKeyColumnsToUse(List<String> keyCols)
  {
    if (keyCols == null)
    {
      this.keyColumnsToUse = null;
    }
    else
    {
      this.keyColumnsToUse = new ArrayList<>(keyCols);
    }
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

  @Override
  public boolean getQuoteAlways()
  {
    return quoteAlways;
  }

  @Override
  public void setQuoteAlways(boolean flag)
  {
    this.quoteAlways = flag;
  }

  @Override
  public void setEscapeRange(CharacterRange range)
  {
    this.escapeRange = range;
  }

  @Override
  public CharacterRange getEscapeRange()
  {
    return this.escapeRange;
  }

  @Override
  public void setLineEnding(String ending)
  {
    if (ending != null)
    {
      this.lineEnding = ending;
    }
  }

  @Override
  public String getLineEnding()
  {
    return this.lineEnding;
  }

  @Override
  public Set<ControlFileFormat> getControlFiles()
  {
    return Collections.unmodifiableSet(controlFiles);
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
    if (formats == null)
    {
      return;
    }
    this.controlFiles.addAll(formats);
  }

  public boolean isContainerExport()
  {
    return this.exportType == ExportType.XLS || this.exportType == ExportType.XLSX;
  }

  public static boolean isContainerType(String exportType)
  {
    return CollectionUtil.caseInsensitiveSet("xls", "xlsx").contains(exportType);
  }
}
