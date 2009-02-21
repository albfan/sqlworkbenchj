/*
 * TextFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.resource.Settings;
import workbench.util.ExceptionUtil;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CsvLineParser;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import workbench.db.importer.modifier.ImportValueModifier;
import workbench.util.FixedLengthLineParser;
import workbench.util.LineParser;
import workbench.util.QuoteEscapeType;
import workbench.util.WbFile;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TextFileParser
	implements RowDataProducer, ImportFileParser
{
	private File inputFile;
	private ImportFileLister sourceFiles;
	
	private File baseDir;
	private String tableName;
	private String encoding = null;
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean decodeUnicode = false;
	private boolean enableMultiLineMode = true;
	
	// this indicates an import of several files from a single
	// directory into one table
	private boolean multiFileImport = false;

	private int colCount = -1;
	private int importColCount = -1;

	private List<ColumnIdentifier> columns;

	// When importing a file with fixed column widths
	// each entry in this array defines the width of the corresponding
	// column in the columns array
	private int[] columnWidthMap;

	// for each column from columns
	// the value for the respective index
	// defines its real index (in rowData)
	// if the value is -1 then the column
	// will not be imported
	private int[] columnMap;

	private Object[] rowData;

	private boolean withHeader = true;
	private boolean cancelImport = false;
	private boolean regularStop = false;
	private boolean emptyStringIsNull = false;
	private boolean trimValues = false;

	private RowDataReceiver receiver;
	private boolean abortOnError = false;
	private WbConnection connection;

	private JobErrorHandler errorHandler;
	private List<ColumnIdentifier> pendingImportColumns;
	private ValueConverter converter = new ValueConverter();
	private MessageBuffer messages = new MessageBuffer();
	private boolean hasErrors = false;
	private boolean hasWarnings = false;


	// If a filter for the input file is defined
	// this will hold the regular expressions per column
	private Pattern[] columnFilter;
	private Pattern lineFilter;
	private String targetSchema;
	private boolean blobsAreFilenames = true;
	private boolean clobsAreFilenames = false;

	private ImportFileHandler fileHandler = new ImportFileHandler();
	private String currentLine;
	private QuoteEscapeType quoteEscape;
	private ImportValueModifier valueModifier;

	public TextFileParser()
	{
		// raise an error during import if the date or timestamps cannot be parsed
		boolean checkBuiltInFormats = Settings.getInstance().getBoolProperty("workbench.import.text.dateformat.checkbuiltin", false);
		converter.setCheckBuiltInFormats(checkBuiltInFormats);
	}

	public TextFileParser(File aFile)
	{
		this();
		this.inputFile = aFile;
	}

	public ImportFileHandler getFileHandler()
	{
		return this.fileHandler;
	}

	public void setValueModifier(ImportValueModifier mod)
	{
		this.valueModifier = mod;
	}

	public void setEnableMultilineRecords(boolean flag)
	{
		this.enableMultiLineMode = flag;
	}

	public void setTargetSchema(String schema)
	{
		this.targetSchema = schema;
	}

	public void setReceiver(RowDataReceiver rec)
	{
		this.receiver = rec;
	}

	public void setInputFile(File file)
	{
		this.sourceFiles = null;
		this.inputFile = file;
	}

	/**
	 * Enables or disables multi-file import. If multi file
	 * import is enabled, all files that are defined will be imported into the same
	 * table defined by {@link #setTableName(java.lang.String) }
	 *
	 * @param flag
	 * @see #setSourceFiles(workbench.db.importer.ImportFileLister)
	 * @see #setTableName(java.lang.String) 
	 */
	public void setMultiFileImport(boolean flag)
	{
		this.multiFileImport = flag;
	}

	public boolean isMultiFileImport() 
	{
		return this.multiFileImport;
	}
	
	public void setSourceFiles(ImportFileLister source)
	{
		this.inputFile = null;
		this.sourceFiles = source;
	}

	public void setTableName(String aName)
	{
		this.tableName = aName;
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.quoteEscape = type;
	}

	public QuoteEscapeType getQuoteEscaping()
	{
		return this.quoteEscape;
	}

	public boolean hasErrors() { return this.hasErrors; }
	public boolean hasWarnings() { return this.hasWarnings; }

	public void importAllColumns()
	{
		this.columnMap = new int[this.colCount];
		for (int i=0; i < this.colCount; i++) this.columnMap[i] = i;
		this.importColCount = this.colCount;
	}

	public String getSourceFilename()
	{
		if (this.inputFile == null) return null;
		return this.inputFile.getAbsolutePath();
	}

	public void setLineFilter(String regex)
	{
		try
		{
			this.lineFilter = Pattern.compile(regex);
		}
		catch (Exception e)
		{
			this.lineFilter = null;
			String msg = ResourceMgr.getString("ErrImportBadRegex");
			msg = StringUtil.replace(msg, "%regex%", regex);
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.hasWarnings = true;
			LogMgr.logError("TextFileParser.addColumnFilter()", "Error compiling regular expression " + regex, e);
		}
	}

	public String getLastRecord()
	{
		return this.currentLine;
	}

	protected boolean hasColumnFilter()
	{
		if (this.columnFilter == null) return false;
		for (int i=0; i < this.columnFilter.length; i++)
		{
			if (this.columnFilter[i] != null) return true;
		}
		return false;
	}

	public void addColumnFilter(String colname, String regex)
	{
		int index = this.getColumnIndex(colname);
		if (index == -1) return;
		if (this.columnFilter == null) this.columnFilter = new Pattern[this.colCount];
		try
		{
			Pattern p = Pattern.compile(regex);
			this.columnFilter[index] = p;
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.addColumnFilter()", "Error compiling regular expression " + regex + " for column " + colname, e);
			String msg = ResourceMgr.getString("ErrImportBadRegex");
			msg = StringUtil.replace(msg, "%regex%", regex);
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.hasWarnings = true;
			this.columnFilter[index] = null;
		}
	}

	public void setTreatClobAsFilenames(boolean flag)
	{
		this.clobsAreFilenames = flag;
	}

	public void setTreatBlobsAsFilenames(boolean flag)
	{
		this.blobsAreFilenames = flag;
	}

	public void setValueConverter(ValueConverter convert)
	{
		this.converter = convert;
	}

	public void setImportColumnNames(List<String> columnList)
		throws IllegalArgumentException
	{
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(columnList.size());
		for (String colname : columnList)
		{
			ColumnIdentifier col = new ColumnIdentifier(colname);
			if (!colname.equals(RowDataProducer.SKIP_INDICATOR) && cols.contains(col))
			{
				String msg = ResourceMgr.getFormattedString("ErrImpDupColumn", colname);
				this.messages.append(msg);
				throw new IllegalArgumentException("Duplicate column " + colname);
			}
			cols.add(col);
		}
		setImportColumns(cols);
	}

	/**
	 * Define the columns that should be imported.
	 * If the list is empty or null, then all columns will be imported
	 * @param columnList the columns to be imported
	 */
	public void setImportColumns(List<ColumnIdentifier> columnList)
	{
		if (columnList == null)
		{
			this.importAllColumns();
			return;
		}

		int count = columnList.size();
		if (count == 0)
		{
			this.importAllColumns();
			return;
		}

		if (this.columns == null)
		{
			// store the list so that when the columns
			// are retrieved or defined later, the real columns to be imported
			// can be defined
			this.pendingImportColumns = columnList;
		}
		else
		{
			this.pendingImportColumns = null;
			checkPendingImportColumns(columnList);
		}
	}

	private void removeInvalidColumns(List cols)
		throws IllegalArgumentException
	{
		Iterator itr = cols.iterator();
		while (itr.hasNext())
		{
			Object o = itr.next();
			String columnName = o.toString();
			int index = this.getColumnIndex(columnName);
			if (index == -1)
			{
				itr.remove();
				String msg = ResourceMgr.getString("ErrImpColNotFound");
				this.messages.append(StringUtil.replace(msg, "%colname%", columnName) + "\n");
				throw new IllegalArgumentException("Column [" + columnName + "] not found");
			}
		}
	}
	/**
	 * Retain only those columns in the defined source file columns
	 * that are in the passed list
	 */
	private void checkPendingImportColumns(List<ColumnIdentifier> colIds)
		throws IllegalArgumentException
	{
		if (colIds == null || colIds.size() == 0) return;

		removeInvalidColumns(colIds);

		int count = colIds.size();
		if (count == 0)
		{
			this.messages.append(ResourceMgr.getString("ErrImpInvalidColDef") + "\n");
			this.hasErrors = true;
			throw new IllegalArgumentException("At least one import column must be defined");
		}

		this.columnMap = new int[this.colCount];
		for (int i=0; i < this.colCount; i++) this.columnMap[i] = -1;
		this.importColCount = 0;

		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = colIds.get(i);
			int index = this.getColumnIndex(col.getColumnName());
			if (index > -1)
			{
				this.columnMap[index] = i;
				this.importColCount ++;
			}
			else
			{
				String msg = ResourceMgr.getString("ErrImpColNotFound");
				this.messages.append(StringUtil.replace(msg, "%colname%", col.getColumnName()) + "\n");
				this.hasErrors = true;
				throw new IllegalArgumentException("Column [" + col.getColumnName() + "] not found!");
			}
		}
	}

	private List<ColumnIdentifier> getColumnsToImport()
	{
		if (this.columnMap == null) return this.columns;
		if (this.importColCount == this.colCount) return this.columns;
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(this.importColCount);
		int col = 0;
		for (int i=0; i < this.colCount; i++)
		{
			if (this.columnMap[i] != -1)
			{
				result.add(this.columns.get(i));
				col++;
			}
		}
		return result;
	}

	private ColumnIdentifier getColumn(int index)
	{
		if (columns == null) return null;
		if (index < columns.size())
		{
			return columns.get(index);
		}
		return null;
	}
	/**
	 * Return the index of the specified column
	 * in the import file.
	 *
	 * @param colName the column to search for
	 * @return the index of the named column or -1 if the column was not found
	 */
	private int getColumnIndex(String colName)
	{
		if (colName == null) return -1;
		if (this.colCount < 1) return -1;
		if (this.columns == null) return -1;
		for (int i=0; i < this.colCount; i++)
		{
			ColumnIdentifier col = getColumn(i);
			if (col != null && colName.equalsIgnoreCase(col.getColumnName())) return i;
		}
		return -1;
	}

	/**
	 * Define the columns in the input file.
	 * @param columnList the list of columns present in the input file
	 * @throws SQLException if the columns could not be verified
	 *         in the DB or the target table does not exist
	 */
	public void setColumns(List<ColumnIdentifier> columnList)
		throws SQLException
	{
		setColumns(columnList, false);
	}

	public void setColumns(List<ColumnIdentifier> columnList, boolean checkTargetTable)
		throws SQLException
	{
		if (columnList == null || columnList.size()  == 0) return;

		if (checkTargetTable) checkTargetTable();

		if (this.connection != null && this.tableName != null)
		{
			this.readColumnDefinition(columnList);
			checkPendingImportColumns(this.pendingImportColumns);
		}
		else
		{
			this.colCount = columnList.size();
			this.columns = new ArrayList<ColumnIdentifier>(columnList);
			this.importAllColumns();
		}
	}

	/**
	 * Define the width for each column.
	 * This will reset a delimiter defined using setDelimiter()
	 */
	public void setColumnWidths(Map<ColumnIdentifier, Integer> widthMapping)
	{
		if (widthMapping == null)
		{
			return;
		}
		if (this.columns == null)
		{
			throw new IllegalArgumentException("No columns defined!");
		}

		this.delimiter = null;
		this.columnWidthMap = new int[this.columns.size()];
		for (int i = 0; i < columns.size(); i++)
		{
			Integer width = widthMapping.get(getColumn(i));
			if (width != null)
			{
				this.columnWidthMap[i] = width.intValue();
			}
		}
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	public String getEncoding()
	{
		return (this.encoding == null ? Settings.getInstance().getDefaultDataEncoding() : this.encoding);
	}

	public void setEncoding(String enc)
	{
		if (enc == null) return;
		this.encoding = enc;
	}

	public MessageBuffer getMessages()
	{
		return this.messages;
	}

	public void setAbortOnError(boolean flag)
	{
		this.abortOnError = flag;
	}

	public void setDelimiter(String delimit)
	{
		if (delimit == null) return;
		this.delimiter = delimit;
		if ("\\t".equals(this.delimiter))
		{
			this.delimiter = "\t";
		}
	}

	public void stop()
	{
		LogMgr.logDebug("TextFileParser.stop()", "Stopping import");
		this.cancelImport = true;
		this.regularStop = true;
	}

	public boolean isCancelled()
	{
		return this.cancelImport;
	}

	public void cancel()
	{
		LogMgr.logDebug("TextFileParser.cancel()", "Cancelling import");
		this.cancelImport = true;
		this.regularStop = false;
	}

	public void setContainsHeader(boolean aFlag)
	{
		this.withHeader = aFlag;
	}

	public void setQuoteChar(String aChar)
	{
		if (StringUtil.isNonBlank(aChar))
		{
			this.quoteChar = aChar;
		}
		else
		{
			this.quoteChar = null;
		}
	}

	public void start()
		throws Exception
	{
		this.receiver.setTableCount(-1); // clear multi-table flag in receiver
		this.receiver.setCurrentTable(-1);

		try
		{
			if (this.sourceFiles != null)
				processDirectory();
			else
				processOneFile();
		}
		finally
		{
			if (this.sourceFiles != null)
			{
				this.receiver.endMultiTable();
			}
			
			if (this.cancelImport && !regularStop)
			{
				this.receiver.importCancelled();
			}
			else
			{
				this.receiver.importFinished();
			}
			try { this.fileHandler.done(); } catch (Throwable th) {}
		}
	}

	private void processDirectory()
		throws Exception
	{
		if (this.sourceFiles == null) throw new IllegalStateException("Cannot process source directory without FileNameSorter");

		this.sourceFiles.setTableNameResolver(new DefaultTablenameResolver());
    if (!sourceFiles.containsFiles())
    {
			String msg = ResourceMgr.getFormattedString("ErrImpNoFiles", sourceFiles.getExtension(), sourceFiles.getDirectory());
			this.messages.append(msg);
			this.hasErrors = true;
      throw new SQLException("No files with extension '" + sourceFiles.getExtension() + "' in directory " + sourceFiles.getDirectory());
    }
		
		List<WbFile> toProcess = null;
		try
		{
			toProcess = sourceFiles.getFiles();
		}
		catch (CycleErrorException e)
		{
			cancelImport = true;
			LogMgr.logError("TextFileParser.processDirectory()", "Error when checking dependencies", e);
			throw e;
		}

		// The receiver only needs to pre-process the full table list
		// if checkDependencies is turned on, otherwise a possible
		// table delete can be done during the single table import
		this.receiver.setTableList(sourceFiles.getTableList());

		int count = toProcess == null ? 0 : toProcess.size();
		if (count == 0)
		{
			String msg = ResourceMgr.getFormattedString("ErrImpNoMatch", sourceFiles.getDirectory());
			this.messages.append(msg);
			this.hasErrors = true;
      throw new SQLException("No matching tables found for files in directory " + sourceFiles.getDirectory());
		}
    
		this.receiver.setTableCount(count);
		if (!multiFileImport)
		{
			this.receiver.beginMultiTable();
		}

		int currentFile = 0;

		for (WbFile f : toProcess)
		{
			if (this.cancelImport)
			{
				break;
			}

			try
			{
				currentFile++;
				this.receiver.setCurrentTable(currentFile);
				if (!multiFileImport)
				{
					// Only reset the import columns and table if multiple files
					// are imported into multiple tables
					// if multifileimport is true, then all files are imported into the same table!
					TableIdentifier tbl = sourceFiles.getTableForFile(f);
					this.tableName = tbl.getTableExpression();
					
					this.columns = null;
					this.colCount = 0;
					this.columnMap = null;
				}
				this.inputFile = f;
				this.processOneFile();
			}
			catch (Exception e)
			{
				this.hasErrors = true;
				this.receiver.tableImportError();
				if (this.abortOnError) throw e;
			}
		}
		
	}

	private void setupFileHandler()
		throws IOException
	{
		this.fileHandler.setMainFile(this.inputFile, this.getEncoding());
	}

	private void processOneFile()
		throws Exception
	{
		this.cancelImport = false;
		this.regularStop = false;

		if (this.inputFile.isAbsolute())
		{
			this.baseDir = this.inputFile.getParentFile();
		}
		if (baseDir == null) this.baseDir = new File(".");

		setupFileHandler();

		// If no header is available in the file and no columns have been
		// specified by the user (i.e. columns is not yet set up)
		// then we assume all columns from the table are present in the input file
		if (!this.withHeader && columns == null)
		{
			this.setColumns(this.getColumnsFromTargetTable(), true);
		}

		BufferedReader in = this.fileHandler.getMainFileReader();

		String lineEnding = StringUtil.LINE_TERMINATOR;
		if (enableMultiLineMode)
		{
			try
			{
				lineEnding = FileUtil.getLineEnding(in);
				if (lineEnding == null)
				{
					// this can happen if only a single line (without a line terminator) is present
					lineEnding = StringUtil.LINE_TERMINATOR;
				}
			}
			catch (IOException io)
			{
				LogMgr.logError("TextFileParser.processOneFile()", "Could not read line ending from file. Multi-line mode disabled!", io);
				this.messages.append(ResourceMgr.getString("ErrNoMultiLine") + "\n");
				enableMultiLineMode = false;
			}
			LogMgr.logInfo("TextFileParser.processOneFile()", "Using line ending: " + lineEnding.replace("\\r", "\\\\r").replaceAll("\\n", "\\\\n"));
			// now that we have already used the Reader supplied by the fileHandler,
			// we have to close and re-open the ZIP archive in order to make sure we start at the beginning
			// as we cannot rely on mark() and reset() to be available for the ZIP archives.
			in.close();
			setupFileHandler();
			in = this.fileHandler.getMainFileReader();
		}

		currentLine = null;
		long lineNumber = 0;

		try
		{
			currentLine = in.readLine();
			lineNumber ++;
			if (this.withHeader)
			{
				if (currentLine == null) throw new IOException("Could not read header line!");
				if (this.columns == null) this.readColumns(currentLine);
				currentLine = in.readLine();
			}
		}
		catch (EOFException eof)
		{
			currentLine = null;
		}
		catch (IOException e)
		{
			LogMgr.logWarning("TextFileParser.processOneFile()", "Error reading input file " + inputFile.getAbsolutePath(), e);
			FileUtil.closeQuitely(in);
			throw e;
		}
		catch (SQLException e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Column definition could not be read.", e);
			FileUtil.closeQuitely(in);
			throw e;
		}

		if (this.colCount <= 0)
		{
			throw new Exception("Cannot import file without a column definition");
		}


		List<ColumnIdentifier> cols = this.getColumnsToImport();
		try
		{
			this.receiver.setTargetTable(getTargetTable(), cols);
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Error setting target table", e);
			throw e;
		}

		this.rowData = new Object[this.importColCount];
		int importRow = 0;

		char quoteCharToUse = (quoteChar == null ? 0 : quoteChar.charAt(0));
		LineParser tok = null;

		if (this.columnWidthMap != null)
		{
			tok = new FixedLengthLineParser(this.columnWidthMap);
		}
		else
		{
			CsvLineParser csv = new CsvLineParser(delimiter.charAt(0), quoteCharToUse);
			csv.setReturnEmptyStrings(true);
			csv.setQuoteEscaping(this.quoteEscape);
			tok = csv;
		}

		tok.setTrimValues(this.trimValues);

		try
		{
			boolean includeLine = true;
			boolean hasColumnFilter = this.hasColumnFilter();
			boolean hasLineFilter = this.lineFilter != null;

			while (currentLine != null)
			{
				if (this.cancelImport) break;

				// silently ignore empty lines...
				if (StringUtil.isEmptyString(currentLine))
				{
					try
					{
						currentLine = in.readLine();
					}
					catch (IOException e)
					{
						LogMgr.logError("TextFileParser.processOneFile()", "Error reading source file", e);
						currentLine = null;
					}
					continue;
				}

				if (enableMultiLineMode && StringUtil.hasOpenQuotes(currentLine, quoteCharToUse))
				{
					try
					{
						StringBuilder b = new StringBuilder(currentLine.length() * 2);
						b.append(currentLine);
						b.append(lineEnding);
						String nextLine = in.readLine();

						// if the next line is null, the file is finished
						// in that case we must not "continue" in order to
						// catch the EOF situation correctly!
						if (nextLine != null)
						{
							b.append(nextLine);
							currentLine = b.toString();
							continue;
						}
					}
					catch (IOException e)
					{
						LogMgr.logError("TextFileParser.processOneFile()", "Could not read next line for multi-line record", e);
					}
				}

				this.clearRowData();

				boolean processRow = receiver.shouldProcessNextRow();
				if (!processRow) receiver.nextRowSkipped();

				if (hasLineFilter && processRow)
				{
					Matcher m = this.lineFilter.matcher(currentLine);
					processRow = m.find();
				}

				importRow ++;

				if (!processRow)
				{
					try
					{
						currentLine = in.readLine();
					}
					catch (IOException e)
					{
						LogMgr.logError("TextFileParser.processOneFile()", "Error reading source file", e);
						currentLine = null;
					}
					continue;
				}

				tok.setLine(currentLine);
				includeLine = true;
				int targetIndex = -1;

				for (int i=0; i < this.colCount; i++)
				{
					String value = null;
					try
					{
						if (tok.hasNext())
						{
							value = tok.getNext();
							ColumnIdentifier col = getColumn(i);
							if (col == null) continue;
							targetIndex = this.columnMap[i];
							if (targetIndex == -1) continue;

							int colType = col.getDataType();

							if (hasColumnFilter && this.columnFilter[i] != null)
							{
								if (value == null)
								{
									includeLine = false;
									break;
								}
								Matcher m = this.columnFilter[i].matcher(value);
								if (!m.matches())
								{
									includeLine = false;
									break;
								}
							}

							if (this.valueModifier != null)
							{
								value = valueModifier.modifyValue(col, value);
							}

							if (SqlUtil.isCharacterType(colType))
							{
								if (clobsAreFilenames && value != null && SqlUtil.isClobType(colType))
								{
									File cfile = new File(value);
									if (!cfile.isAbsolute())
									{
										cfile = new File(this.baseDir, value);
									}
									rowData[targetIndex] = cfile;
								}
								else
								{
									if (this.decodeUnicode)
									{
										value = StringUtil.decodeUnicode(value);
									}
									if (this.emptyStringIsNull && StringUtil.isEmptyString(value))
									{
										value = null;
									}
									rowData[targetIndex] = value;
								}
							}
							else if (blobsAreFilenames && !StringUtil.isEmptyString(value) && SqlUtil.isBlobType(colType) )
							{
								File bfile = new File(value.trim());
								if (!bfile.isAbsolute())
								{
									bfile = new File(this.baseDir, value.trim());
								}
								rowData[targetIndex] = bfile;
							}
							else
							{
								rowData[targetIndex] = converter.convertValue(value, colType);
							}
						}

					}
					catch (Exception e)
					{
						if (targetIndex != -1) rowData[targetIndex] = null;
						String msg = ResourceMgr.getString("ErrTextfileImport");
						msg = msg.replace("%row%", Integer.toString(importRow));
						msg = msg.replace("%col%", (getColumn(i) == null ? "n/a" : getColumn(i).getColumnName()));
						msg = msg.replace("%value%", (value == null ? "(NULL)" : value));
						msg = msg.replace("%msg%", e.getClass().getName() + ": " + ExceptionUtil.getDisplay(e, false));
						this.messages.append(msg);
						this.messages.appendNewLine();
						if (this.abortOnError)
						{
							this.hasErrors = true;
							this.cancelImport = true;
							throw e;
						}
						this.hasWarnings = true;
						LogMgr.logWarning("TextFileParser.processOneFile()", msg, e);
						if (this.errorHandler != null)
						{
							int choice = errorHandler.getActionOnError(importRow, getColumn(i).getColumnName(), (value == null ? "(NULL)" : value), ExceptionUtil.getDisplay(e, false));
							if (choice == JobErrorHandler.JOB_ABORT) throw e;
							if (choice == JobErrorHandler.JOB_IGNORE_ALL)
							{
								this.abortOnError = false;
							}
						}
						this.receiver.recordRejected(currentLine, lineNumber, e);
						includeLine = false;
					}
				}

				if (this.cancelImport) break;

				try
				{
					if (includeLine) this.receiver.processRow(rowData);
				}
				catch (Exception e)
				{
					this.hasErrors = true;
					this.cancelImport = true;
					// processRow() will only throw an exception if abortOnError is true
					// so we can always re-throw the exception here.
					LogMgr.logError("TextFileParser.processOneFile()", "Error sending line " + importRow, e);
					throw e;
				}

				try
				{
					currentLine = in.readLine();
				}
				catch (IOException e)
				{
					LogMgr.logError("TextFileParser.processOneFile()", "Error reading source file", e);
					currentLine = null;
				}
			}
		}
		finally
		{
			FileUtil.closeQuitely(in);
			// do not close the ImportFileHandler here, because the DataImporter
			// might still need the references to the ZIP archives if running
			// in batch mode. So the fileHandler is closed after sending the finishImport()
			// to the DataImporter
		}

	}

	private void clearRowData()
	{
		for (int i=0; i < this.importColCount; i++)
		{
			this.rowData[i] = null;
		}
	}

	/**
	 * 	Retrieve the column definitions from the header line
	 */
	private void readColumns(String headerLine)
		throws Exception
	{
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		WbStringTokenizer tok = new WbStringTokenizer(delimiter.charAt(0), this.quoteChar, false);
		tok.setDelimiterNeedsWhitspace(false);
		tok.setSourceString(headerLine);
		while (tok.hasMoreTokens())
		{
			String column = tok.nextToken();
			cols.add(new ColumnIdentifier(column));
		}
		this.readColumnDefinition(cols);
		if (this.pendingImportColumns != null)
		{
			checkPendingImportColumns(this.pendingImportColumns);
			this.pendingImportColumns = null;
		}
	}

	/**
	 * Return the column names found in the input file.
	 * The identifiers will only have a name but
	 * no data type assigned as this information is not available in a text file.
	 * If the input file does not contain a header row, the columns
	 * will be named Column1, Column2, ...
	 *
	 * @return the columns defined in the input file
	 */
	public List<ColumnIdentifier> getColumnsFromFile()
	{
		BufferedReader in = null;
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		try
		{
			// Make sure the file handler is initialized as this can be called from
			// the outside as well.
			setupFileHandler();
			in = this.fileHandler.getMainFileReader();
			String firstLine = in.readLine();
			WbStringTokenizer tok = new WbStringTokenizer(delimiter.charAt(0), this.quoteChar, false);
			tok.setSourceString(firstLine);
			int i = 1;
			while (tok.hasMoreTokens())
			{
				String column = tok.nextToken();
				if (column == null) continue;
				String name = null;
				if (this.withHeader)
				{
					name = column.toUpperCase();
				}
				else
				{
					name = "Column" + i;
				}
				ColumnIdentifier c = new ColumnIdentifier(name);
				cols.add(c);
				i++;
			}
		}
		catch (Exception e)
		{
			this.hasErrors = true;
			LogMgr.logError("TextFileParser.getColumnsFromFile()", "Error when reading columns", e);
		}
		finally
		{
			this.fileHandler.done();
		}
		return cols;
	}

	protected void checkTargetTable()
		throws SQLException
	{
		TableIdentifier tbl = getTargetTable();

		if (!this.connection.getMetadata().tableExists(tbl))
		{
			String msg = ResourceMgr.getFormattedString("ErrImportTableNotFound", tbl.getTableExpression());
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.columns = null;
			this.hasErrors = true;
			throw new SQLException("Table " + tbl.getTableExpression() + " not found!");
		}
	}

	public void setupFileColumns()
		throws SQLException, IOException
	{
		List<ColumnIdentifier> cols = null;

		checkTargetTable();

		if (this.withHeader)
		{
			cols = this.getColumnsFromFile();
		}
		else
		{
			cols = this.getColumnsFromTargetTable();
		}
		this.setColumns(cols, false);
	}

	private List<ColumnIdentifier> getColumnsFromTargetTable()
		throws SQLException
	{
		return this.connection.getMetadata().getTableColumns(getTargetTable());
	}

	private TableIdentifier getTargetTable()
	{
		if (this.tableName == null) return null;
		TableIdentifier targetTable = new TableIdentifier(this.tableName);
		targetTable.setPreserveQuotes(true);
		if (this.targetSchema != null)
		{
			targetTable.setSchema(this.targetSchema);
		}
		if (this.connection != null)
		{
			targetTable.adjustCase(this.connection);
			if (targetTable.getSchema() == null)
			{
				targetTable.setSchema(this.connection.getCurrentSchema());
			}
		}
		return targetTable;
	}

	/**
	 * 	Read the column definitions from the database.
	 * 	@param cols a List of column names (String)
	 */
	private void readColumnDefinition(List<ColumnIdentifier> cols)
		throws SQLException
	{
		try
		{
			this.colCount = cols.size();
			this.columns = new ArrayList<ColumnIdentifier>(colCount);
			ArrayList<ColumnIdentifier> realCols = new ArrayList<ColumnIdentifier>();
			boolean partialImport = false;
			DbMetadata meta = this.connection.getMetadata();
			TableDefinition def = meta.getTableDefinition(getTargetTable());
			TableIdentifier targetTable = def.getTable();
			List<ColumnIdentifier> tableCols = def.getColumns();
			int numTableCols = tableCols.size();

			// Should not happen, but just to make sure ;)
			if (numTableCols == 0)
			{
				messages.append(ResourceMgr.getFormattedString("ErrImportTableNotFound", targetTable.getTableExpression()));
				throw new SQLException("Table " + targetTable.getTableExpression() + " not found!");
			}

			for (int i=0; i < cols.size(); i++)
			{
				ColumnIdentifier col = cols.get(i);
				String colname = col.getColumnName();
				if (colname.toLowerCase().startsWith(RowDataProducer.SKIP_INDICATOR) )
				{
					partialImport = true;
					this.columns.add(null);
				}
				else
				{
					int index = tableCols.indexOf(col);
					if (index > -1)
					{
						col = tableCols.get(index);
						this.columns.add(col);
						realCols.add(col);
					}
					else
					{
						if (this.pendingImportColumns == null || this.pendingImportColumns.contains(colname))
						{
							if (this.abortOnError)
							{
								String msg = ResourceMgr.getString("ErrImportColumnNotFound");
								msg = StringUtil.replace(msg, "%column%", colname);
								msg = StringUtil.replace(msg, "%table%", this.tableName);
								this.messages.append(msg);
								this.messages.appendNewLine();
								this.hasErrors = true;
								throw new SQLException(msg);
							}
							else
							{
								String msg = ResourceMgr.getString("ErrImportColumnIgnored");
								msg = StringUtil.replace(msg, "%column%", colname);
								msg = StringUtil.replace(msg, "%table%", this.tableName);
								LogMgr.logWarning("TextFileParser.readColumns()", msg);
								this.hasWarnings = true;
								this.messages.append(msg);
								this.messages.appendNewLine();
							}
						}
						partialImport = true;
					}
				}
			}

			if (realCols.size() == 0)
			{
				String msg = ResourceMgr.getString("ErrImportNoColumns");
				msg = StringUtil.replace(msg, "%table%", this.tableName);
				this.hasErrors = true;
				this.messages.append(msg);
				this.messages.appendNewLine();
				throw new SQLException("No column matched in import file");
			}

			// reset mapping
			this.importAllColumns();

			if (partialImport)
			{
				// only if we found at least one column to ignore, we
				// need to set the real column list
				this.setImportColumns(realCols);
			}

		}
		catch (SQLException e)
		{
			this.hasErrors = true;
			throw e;
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.readColumnDefinition()", "Error when reading column definition", e);
			this.colCount = -1;
			this.columns = null;
		}
	}

	public int getColumnCount()
	{
		return this.colCount;
	}

	/**
	 *	Returns the column list as a comma separated string
	 *  that can be used for the WbImport command
	 */
	public String getColumns()
	{
		StringBuilder result = new StringBuilder(this.colCount * 10);

		if (this.columnMap == null || this.importColCount == this.colCount)
		{
			for (int i=0; i < this.colCount; i++)
			{
				if (i > 0) result.append(',');
				result.append(getColumn(i).getColumnName());
			}
		}
		else
		{
			for (int i=0; i < this.colCount; i++)
			{
				if (i > 0) result.append(',');
				if (this.columnMap[i] != -1)
				{
					result.append(getColumn(i).getColumnName());
				}
				else
				{
					result.append(RowDataProducer.SKIP_INDICATOR);
				}
			}
		}
		return result.toString();
	}

	/**
	 * Getter for property emptyStringIsNull.
	 * @return Value of property emptyStringIsNull.
	 */
	public boolean isEmptyStringIsNull()
	{
		return emptyStringIsNull;
	}

	/**
	 * Setter for property emptyStringIsNull.
	 * @param flag New value of property emptyStringIsNull.
	 */
	public void setEmptyStringIsNull(boolean flag)
	{
		this.emptyStringIsNull = flag;
	}

	public void setDecodeUnicode(boolean flag)
	{
		this.decodeUnicode = flag;
	}

	public boolean getDecodeUnicode()
	{
		return this.decodeUnicode;
	}

	public boolean isTrimValues()
	{
		return trimValues;
	}

	public void setTrimValues(boolean trim)
	{
		this.trimValues = trim;
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
		this.errorHandler = handler;
	}

}
