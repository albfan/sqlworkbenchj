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
import java.util.List;
import java.util.Map;

import workbench.db.ColumnIdentifier;
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
	private String targetSchema;
	private TableDefinition targetTable;
	private String encoding = null;
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean decodeUnicode = false;
	private boolean enableMultiLineMode;

	// this indicates an import of several files from a single
	// directory into one table
	private boolean multiFileImport = false;

	private List<ImportFileColumn> importColumns;

	private boolean withHeader = true;
	private boolean cancelImport = false;
	private boolean regularStop = false;
	private boolean emptyStringIsNull = false;
	private boolean trimValues = false;

	private RowDataReceiver receiver;
	private boolean abortOnError = false;
	private WbConnection connection;

	private JobErrorHandler errorHandler;

	private ValueConverter converter = new ValueConverter();
	private MessageBuffer messages = new MessageBuffer();
	private boolean hasErrors = false;
	private boolean hasWarnings = false;

	private Pattern lineFilter;
	private boolean blobsAreFilenames = true;
	private boolean clobsAreFilenames = false;
	private boolean fixedWidthImport = false;

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
		this.targetTable = null;
		this.importColumns = null;
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.quoteEscape = type;
	}

	public QuoteEscapeType getQuoteEscaping()
	{
		return this.quoteEscape;
	}

	public boolean hasErrors()
	{
		return this.hasErrors;
	}

	public boolean hasWarnings()
	{
		return this.hasWarnings;
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

	public void addColumnFilter(String colname, String regex)
	{
		int index = this.getColumnIndex(colname);
		if (index == -1) return;

		try
		{
			Pattern p = Pattern.compile(regex);
			importColumns.get(index).setColumnFilter(p);
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.addColumnFilter()", "Error compiling regular expression " + regex + " for column " + colname, e);
			String msg = ResourceMgr.getString("ErrImportBadRegex");
			msg = StringUtil.replace(msg, "%regex%", regex);
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.hasWarnings = true;
			importColumns.get(index).setColumnFilter(null);
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

	public void retainColumns(List<ColumnIdentifier> columnList)
		throws IllegalArgumentException
	{
		if (this.importColumns == null || importColumns.size() == 0)
		{
			throw new IllegalStateException("Must set file columns first");
		}
		for (ImportFileColumn impCol : importColumns)
		{
			if (impCol == null) continue;
			boolean keep = columnList.contains(impCol.getColumn());
			if (!keep)
			{
				impCol.setTargetIndex(-1);
			}
		}

		// renumber the target index
		int index = 0;
		for (ImportFileColumn impCol : importColumns)
		{
			if (impCol.getTargetIndex() != -1)
			{
				impCol.setTargetIndex(index);
				index++;
			}
		}
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
		return this.importColumns.indexOf(colName);
	}

	/**
	 * Define the columns in the input file.
	 * If a column name equals RowDataProducer.SKIP_INDICATOR
	 * then the column will not be imported.
	 * @param columnList the list of columns present in the input file
	 * @throws SQLException if the columns could not be verified
	 *         in the DB or the target table does not exist
	 */
	public void setColumns(List<ColumnIdentifier> columnList)
		throws SQLException
	{
		// When using the TextFileParser to import into a DataStore
		// no target table is defined, so this is an expected situation
		TableDefinition target = getTargetTable();
		List<ColumnIdentifier> tableCols = null;
		if (target != null)
		{
		 tableCols = target.getColumns();
		}

		importColumns = ImportFileColumn.createList();

		int colCount = 0;

		try
		{
			for (ColumnIdentifier sourceCol : columnList)
			{
				if (sourceCol.getColumnName().equalsIgnoreCase(RowDataProducer.SKIP_INDICATOR))
				{
					importColumns.add(ImportFileColumn.SKIP_COLUMN);
					continue;
				}
				int index = (tableCols != null ? tableCols.indexOf(sourceCol) : 0);
				if (index < 0)
				{
					if (this.abortOnError)
					{
						String msg = ResourceMgr.getString("ErrImportColumnNotFound");
						msg = StringUtil.replace(msg, "%column%", sourceCol.getColumnName());
						msg = StringUtil.replace(msg, "%table%", this.tableName);
						this.messages.append(msg);
						this.messages.appendNewLine();
						this.hasErrors = true;
						throw new SQLException(msg);
					}
					else
					{
						String msg = ResourceMgr.getString("ErrImportColumnIgnored");
						msg = StringUtil.replace(msg, "%column%", sourceCol.getColumnName());
						msg = StringUtil.replace(msg, "%table%", this.tableName);
						LogMgr.logWarning("TextFileParser.setColumns()", msg);
						this.hasWarnings = true;
						this.messages.append(msg);
						this.messages.appendNewLine();
					}
				}
				else
				{
					ColumnIdentifier col = (tableCols != null ? tableCols.get(index) : sourceCol);
					ImportFileColumn importCol = new ImportFileColumn(col);
					importCol.setTargetIndex(colCount);
					importColumns.add(importCol);
					colCount ++;
				}
			}
		}
		catch (SQLException e)
		{
			this.hasErrors = true;
			throw e;
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.setColumns()", "Error when setting column definition", e);
			this.importColumns = null;
		}

		if (colCount == 0)
		{
			String msg = ResourceMgr.getString("ErrImportNoColumns");
			msg = StringUtil.replace(msg, "%table%", this.tableName);
			this.hasErrors = true;
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.importColumns = null;
			throw new SQLException("No column matched in import file");
		}
	}

	/**
	 * Define the width for each column.
	 * This will reset a delimiter defined using setDelimiter()
	 */
	public void setColumnWidths(Map<ColumnIdentifier, Integer> widthMapping)
	{
		if (widthMapping == null) return;

		if (this.importColumns == null)
		{
			throw new IllegalArgumentException("No columns defined!");
		}

		this.delimiter = null;
		for (Map.Entry<ColumnIdentifier, Integer> entry : widthMapping.entrySet())
		{
			int index = this.importColumns.indexOf(entry.getKey());
			if (index != -1)
			{
				ImportFileColumn col = importColumns.get(index);
				if (col != null)
				{
					col.setDataWidth(entry.getValue().intValue());
				}
			}
		}
		fixedWidthImport = true;
	}

	protected List<Integer> getColumnWidths()
	{
		if (this.importColumns == null) return null;

		if (!fixedWidthImport) return null;
		List<Integer> result = new ArrayList<Integer>();
		for (ImportFileColumn col : importColumns)
		{
			if (col.getDataWidth() > -1)
			{
				result.add(Integer.valueOf(col.getDataWidth()));
			}
		}
		return result;

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
					setTableName(tbl.getTableExpression());
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

	protected List<ColumnIdentifier> getColumnsToImport()
	{
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>();
		for (ImportFileColumn col : importColumns)
		{
			if (col.getTargetIndex() >= 0)
			{
				result.add(col.getColumn());
			}
		}
		return result;
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
		if (!this.withHeader && importColumns == null)
		{
			this.setColumns(getTargetTable().getColumns());
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
				if (this.importColumns == null) this.readColumns(currentLine);
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

		if (this.importColumns == null || importColumns.size() == 0)
		{
			throw new Exception("Cannot import file without a column definition");
		}

		List<ColumnIdentifier> columnsToImport = getColumnsToImport();
		try
		{
			// The target table might be null if an import is done into a DataStore
			this.receiver.setTargetTable((targetTable != null ? targetTable.getTable() : null), columnsToImport);
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Error setting target table", e);
			throw e;
		}

		Object[] rowData = new Object[columnsToImport.size()];

		int importRow = 0;

		char quoteCharToUse = (quoteChar == null ? 0 : quoteChar.charAt(0));
		LineParser tok = null;

		if (fixedWidthImport)
		{
			tok = new FixedLengthLineParser(getColumnWidths());
		}
		else
		{
			CsvLineParser csv = new CsvLineParser(delimiter.charAt(0), quoteCharToUse);
			csv.setReturnEmptyStrings(true);
			csv.setQuoteEscaping(this.quoteEscape);
			tok = csv;
		}

		tok.setTrimValues(this.trimValues);
		int sourceCount = importColumns.size();

		try
		{
			boolean includeLine = true;
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

				List<String> lineValues = getLineValues(tok, currentLine);

				includeLine = true;
				int targetIndex = -1;

				for (int sourceIndex=0; sourceIndex < sourceCount; sourceIndex++)
				{
					ImportFileColumn fileCol = importColumns.get(sourceIndex);
					if (fileCol == null) continue;

					targetIndex = fileCol.getTargetIndex();
					if (targetIndex == -1) continue;

					String value = lineValues.get(sourceIndex);
					try
					{
						ColumnIdentifier col = fileCol.getColumn();
						int colType = col.getDataType();

						if (fileCol.getColumnFilter() != null)
						{
							if (value == null)
							{
								includeLine = false;
								break;
							}
							Matcher m = fileCol.getColumnFilter().matcher(value);
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
					catch (Exception e)
					{
						if (targetIndex != -1) rowData[targetIndex] = null;
						String msg = ResourceMgr.getString("ErrTextfileImport");
						msg = msg.replace("%row%", Integer.toString(importRow));
						msg = msg.replace("%col%", (fileCol == null ? "n/a" : fileCol.getColumn().getColumnName()));
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
							int choice = errorHandler.getActionOnError(importRow, fileCol.getColumn().getColumnName(), (value == null ? "(NULL)" : value), ExceptionUtil.getDisplay(e, false));
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


	protected List<String> getLineValues(LineParser parser, String line)
	{
		List<String> result = new ArrayList<String>();
		parser.setLine(line);
		while (parser.hasNext())
		{
			result.add(parser.getNext());
		}
		return result;
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
		this.setColumns(cols);
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

	public void checkTargetTable()
		throws SQLException
	{
		TableDefinition def = getTargetTable();

		if (def == null || def.getColumns() == null || def.getColumns().size() == 0)
		{
			TableIdentifier tbl = createTargetTableId();
			String msg = ResourceMgr.getFormattedString("ErrImportTableNotFound", tbl.getTableExpression());
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.importColumns = null;
			this.hasErrors = true;
			throw new SQLException("Table " + tbl.getTableExpression() + " not found!");
		}
	}

	public void setupFileColumns()
		throws SQLException, IOException
	{
		List<ColumnIdentifier> cols = null;

		if (this.withHeader)
		{
			cols = this.getColumnsFromFile();
		}
		else
		{
			TableDefinition def = getTargetTable();
			cols = def.getColumns();
		}
		this.setColumns(cols);
	}

	private TableIdentifier createTargetTableId()
	{
		TableIdentifier table = new TableIdentifier(this.tableName);
		table.setPreserveQuotes(true);
		if (this.targetSchema != null)
		{
			table.setSchema(this.targetSchema);
		}
		if (this.connection != null)
		{
			table.adjustCase(this.connection);
			if (table.getSchema() == null)
			{
				table.setSchema(this.connection.getCurrentSchema());
			}
		}
		return table;
	}

	protected TableDefinition getTargetTable()
		throws SQLException
	{
		if (this.tableName == null) return null;
		if (this.targetTable != null) return targetTable;
		TableIdentifier table = createTargetTableId();

		targetTable = connection.getMetadata().getTableDefinition(table);

		return targetTable;
	}

	public int getColumnCount()
	{
		return importColumns.size();
	}

	List<ImportFileColumn> getImportColumns()
	{
		return importColumns;
	}
	
	/**
	 *	Returns the column list as a comma separated string
	 *  that can be used for the WbImport command
	 */
	public String getColumns()
	{
		StringBuilder result = new StringBuilder();

		int colCount = 0;
		for (ImportFileColumn col : importColumns)
		{
			if (colCount > 0) result.append(',');
			if (col != null && col.getTargetIndex() != -1)
			{
				result.append(col.getColumn().getColumnName());
			}
			else
			{
				result.append(RowDataProducer.SKIP_INDICATOR);
			}
			colCount ++;
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
