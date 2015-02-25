/*
 * TextFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.importer;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.interfaces.JobErrorHandler;
import workbench.interfaces.TabularDataParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.exporter.BlobMode;

import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.CsvLineParser;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.FixedLengthLineParser;
import workbench.util.LineParser;
import workbench.util.QuoteEscapeType;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  Thomas Kellerer
 */
public class TextFileParser
	extends AbstractImportFileParser
	implements TextImportOptions, TabularDataParser
{
	private File baseDir;
	private String delimiter = "\t";
	private String quoteChar;
	private boolean decodeUnicode;
	private boolean enableMultiLineMode;

	private boolean withHeader = true;
	private boolean emptyStringIsNull;
	private boolean alwaysQuoted;
	private boolean illegalDateIsNull;

	private Pattern lineFilter;

	private boolean clobsAreFilenames;
	private boolean fixedWidthImport;

	private String currentLine;
	private QuoteEscapeType quoteEscape;

	private StreamImporter streamImporter;
	private String nullString;

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

	@Override
	public String getNullString()
	{
		return nullString;
	}

	@Override
	public void setNullString(String value)
	{
		nullString = value;
	}

	public void setEnableMultilineRecords(boolean flag)
	{
		this.enableMultiLineMode = flag;
	}

	@Override
	public QuoteEscapeType getQuoteEscaping()
	{
		return quoteEscape;
	}

	@Override
	public boolean getQuoteAlways()
	{
		return alwaysQuoted;
	}

	public void setAlwaysQuoted(boolean flag)
	{
		this.alwaysQuoted = flag;
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.quoteEscape = type;
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

	@Override
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

	public void setIllegalDateIsNull(boolean flag)
	{
		this.illegalDateIsNull = flag;
	}

	public void setBlobMode(BlobMode mode)
	{
		blobDecoder.setBlobMode(mode);
	}

	public void setTreatClobAsFilenames(boolean flag)
	{
		this.clobsAreFilenames = flag;
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

	@Override
	public void setColumns(List<ColumnIdentifier> columnList)
		throws SQLException
	{
		setColumns(columnList, null);
	}

	/**
	 * Define the columns in the input file.
	 *
	 * If a column name equals RowDataProducer.SKIP_INDICATOR
	 * then the column will not be imported.
	 *
	 * @param fileColumns the list of columns present in the input file
	 * @param columnsToImport the list of columns to import, if null all columns are imported
	 *
	 * @throws SQLException if the columns could not be verified
	 *         in the DB or the target table does not exist
	 */
	@Override
	public void setColumns(List<ColumnIdentifier> fileColumns, List<ColumnIdentifier> columnsToImport)
		throws SQLException
	{
		TableDefinition target = getTargetTable();
		List<ColumnIdentifier> tableCols = null;

		if (target == null)
		{
			// When using the TextFileParser to import into a DataStore
			// no target table is defined, so this is an expected situation and we simply
			// pretend the file columns are the target columns
			tableCols = new ArrayList<>(fileColumns);
		}
		else
		{
			tableCols = target.getColumns();
		}

		importColumns = ImportFileColumn.createList();

		int colCount = 0;
		if (columnsToImport == null)
		{
			columnsToImport = Collections.emptyList();
		}

    List<String> warnings = new ArrayList<>();

		try
		{
			for (ColumnIdentifier sourceCol : fileColumns)
			{
				boolean ignoreColumn = sourceCol.getColumnName().equalsIgnoreCase(RowDataProducer.SKIP_INDICATOR);
				if (!ignoreColumn && !columnsToImport.isEmpty())
				{
					ignoreColumn = !columnsToImport.contains(sourceCol);
				}

				int index = tableCols.indexOf(sourceCol);

				if (!ignoreColumn && index < 0)
				{
					if (this.abortOnError && !ignoreMissingColumns)
					{
						String msg = ResourceMgr.getFormattedString("ErrImportColumnNotFound", sourceCol.getColumnName(), getSourceFilename(), this.tableName);
						this.messages.append(msg);
						this.messages.appendNewLine();
						this.hasErrors = true;
						throw new SQLException(msg);
					}
					else
					{
						String msg = ResourceMgr.getFormattedString("ErrImportColumnIgnored", sourceCol.getColumnName(), getSourceFilename(), this.tableName);
						LogMgr.logWarning("TextFileParser.setColumns()", msg);
						this.hasWarnings = true;
            warnings.add(msg);
						ignoreColumn = true;
					}
				}

				if (ignoreColumn)
				{
					ImportFileColumn col = new ImportFileColumn(sourceCol);
					col.setTargetIndex(-1);
					importColumns.add(col);
				}
				else
				{
					ColumnIdentifier col = tableCols.get(index);
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

    if (warnings.size() > 0)
    {
      this.messages.appendNewLine();
      for (String warn : warnings)
      {
        this.messages.append(warn);
        this.messages.appendNewLine();
      }
    }
		if (colCount == 0)
		{
			String msg = ResourceMgr.getFormattedString("ErrImportNoColumns", tableName, getSourceFilename());
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

	/**
	 * Return the column value from the input file for each column
	 * passed in to the function.
	 * @param inputFileIndexes the index of each column in the input file
	 * @return for each column index the value in the inputfile
	 */
	@Override
	public Map<Integer, Object> getInputColumnValues(Collection<Integer> inputFileIndexes)
	{
		if (currentRowValues == null) return null;
		if (inputFileIndexes == null) return null;

		Map<Integer, Object> result = new HashMap<>(inputFileIndexes.size());
		for (Integer index : inputFileIndexes)
		{
			if (index > 0 && index <= currentRowValues.size())
			{
				result.put(index, currentRowValues.get(index - 1));
			}
		}
		return result;
	}

	protected List<Integer> getColumnWidths()
	{
		if (this.importColumns == null) return null;

		if (!fixedWidthImport) return null;
		List<Integer> result = new ArrayList<>();
		for (ImportFileColumn col : importColumns)
		{
			if (col.getDataWidth() > -1)
			{
				result.add(Integer.valueOf(col.getDataWidth()));
			}
		}
		return result;

	}

	@Override
	public String getTextDelimiter()
	{
		return this.delimiter;
	}

	@Override
	public void setTextDelimiter(String delimit)
	{
		if (StringUtil.isEmptyString(delimit)) return;
		this.delimiter = delimit;
		if (this.delimiter.contains("\\t"))
		{
			this.delimiter = delimiter.replace("\\t", "\t");
		}
	}

	@Override
	public boolean getContainsHeader()
	{
		return withHeader;
	}

	@Override
	public void setContainsHeader(boolean aFlag)
	{
		this.withHeader = aFlag;
	}

	@Override
	public String getTextQuoteChar()
	{
		return quoteChar;
	}


	@Override
	public void setTextQuoteChar(String aChar)
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

	@Override
	public String getLastRecord()
	{
		return currentLine;
	}

	public void setStreamImporter(StreamImporter importer)
	{
		this.streamImporter = importer;
	}

	protected void sendCompleteFile(List<ColumnIdentifier> columns, String encoding)
		throws Exception
	{
		try
		{
			Reader in = this.fileHandler.getMainFileReader();
			streamImporter.setup(targetTable.getTable(), columns, in, this, encoding);
			receiver.processFile(streamImporter);
		}
		finally
		{
			fileHandler.done();
		}
	}

	@Override
	protected void processOneFile()
		throws Exception
	{
		if (this.inputFile.isAbsolute())
		{
			this.baseDir = this.inputFile.getParentFile();
		}
		if (baseDir == null) this.baseDir = new File(".");

		blobDecoder.setBaseDir(baseDir);

		setupFileHandler();

		// If no header is available in the file and no columns have been
		// specified by the user (i.e. columns is not yet set up)
		// then we assume all columns from the table are present in the input file
		if (!this.withHeader && importColumns == null)
		{
			this.setColumns(getTargetTable().getColumns(), null);
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
				this.messages.append(ResourceMgr.getString("ErrNoMultiLine"));
				enableMultiLineMode = false;
			}
			LogMgr.logInfo("TextFileParser.processOneFile()", "Using line ending: " + StringUtil.escapeText(lineEnding, CharacterRange.RANGE_CONTROL));
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
			FileUtil.closeQuietely(in);
			throw e;
		}
		catch (SQLException e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Column definition could not be read.", e);
			FileUtil.closeQuietely(in);
			throw e;
		}

		if (CollectionUtil.isEmpty(importColumns))
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

		if (this.streamImporter != null)
		{
			// need to reset the stream to the beginning
			FileUtil.closeQuietely(in);
			sendCompleteFile(columnsToImport, fileHandler.getEncoding());
			return;
		}

		final Object[] rowData = new Object[columnsToImport.size()];

		int importRow = 0;

		char quoteCharToUse = (quoteChar == null ? 0 : quoteChar.charAt(0));
		LineParser tok = null;

		if (fixedWidthImport)
		{
			tok = new FixedLengthLineParser(getColumnWidths());
		}
		else
		{
			CsvLineParser csv = new CsvLineParser(delimiter, quoteCharToUse);
			csv.setReturnEmptyStrings(true);
			csv.setUnquotedEmptyStringIsNull(alwaysQuoted);
			csv.setQuoteEscaping(this.quoteEscape);
			tok = csv;
		}

		converter.setIllegalDateIsNull(illegalDateIsNull);

		tok.setTrimValues(this.trimValues);
		int sourceCount = importColumns.size();

		try
		{
			boolean includeLine = true;
			boolean hasLineFilter = this.lineFilter != null;

			while (currentLine != null)
			{
				if (cancelImport) break;

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

				if (enableMultiLineMode && StringUtil.hasOpenQuotes(currentLine, quoteCharToUse, quoteEscape))
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
					Matcher m = lineFilter.matcher(currentLine);
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

				currentRowValues = getLineValues(tok, currentLine);

				includeLine = true;
				int targetIndex = -1;

				Arrays.fill(rowData, null);
				for (int sourceIndex=0; sourceIndex < sourceCount; sourceIndex++)
				{
					ImportFileColumn fileCol = importColumns.get(sourceIndex);
					if (fileCol == null) continue;

					targetIndex = fileCol.getTargetIndex();
					if (targetIndex == -1) continue;

					if (sourceIndex >= currentRowValues.size())
					{
						// Log this warning only once
						if (importRow == 1)
						{
							LogMgr.logWarning("TextFileParser.processOneFile()", "Ignoring column with index=" + (sourceIndex + 1) + " because the import file has fewer columns");
						}
						continue;
					}
					String value = currentRowValues.get(sourceIndex);

					try
					{
						ColumnIdentifier col = fileCol.getColumn();
						int colType = col.getDataType();
						String dbmsType = col.getDbmsType();

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

						if (valueModifier != null)
						{
							value = valueModifier.modifyValue(col, value);
						}

						if (SqlUtil.isCharacterType(colType) || SqlUtil.isXMLType(colType))
						{
							if (clobsAreFilenames && value != null && (SqlUtil.isClobType(colType, dbmsType, connection.getDbSettings()) || SqlUtil.isXMLType(colType) ))
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
						else if (SqlUtil.isBlobType(colType) )
						{
							rowData[targetIndex] = blobDecoder.decodeBlob(value);
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
					if (includeLine) receiver.processRow(rowData);
				}
				catch (Exception e)
				{
					if (cancelImport)
					{
						LogMgr.logDebug("TextFileParser.processOneFile()", "Error sending line " + importRow, e);
					}
					else
					{
						hasErrors = true;
						cancelImport = true;
						// processRow() will only throw an exception if abortOnError is true
						// so we can always re-throw the exception here.
						LogMgr.logError("TextFileParser.processOneFile()", "Error sending line " + importRow, e);
						throw e;
					}
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

			filesProcessed.add(inputFile);
			if (!cancelImport)
			{
				receiver.tableImportFinished();
			}
		}
		finally
		{
			FileUtil.closeQuietely(in);
			fileHandler.done();
		}
	}

	@Override
	public void done()
	{
		if (fileHandler != null)
		{
			fileHandler.done();
		}
	}

	protected List<String> getLineValues(LineParser parser, String line)
	{
		List<String> result = new ArrayList<>(getColumnCount());
		parser.setLine(line);
		while (parser.hasNext())
		{
			String value = parser.getNext();
			if (this.nullString != null && value.equals(nullString))
			{
				value = null;
			}
			result.add(value);
		}
		return result;
	}

	/**
	 * 	Retrieve the column definitions from the header line
	 */
	private void readColumns(String headerLine)
		throws Exception
	{
		List<ColumnIdentifier> cols = new ArrayList<>();
		WbStringTokenizer tok = new WbStringTokenizer(delimiter, this.quoteChar, false);
		tok.setDelimiterNeedsWhitspace(false);
		tok.setSourceString(headerLine);
		while (tok.hasMoreTokens())
		{
			String column = tok.nextToken();
			cols.add(new ColumnIdentifier(column));
		}
		this.setColumns(cols, null);
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
	@Override
	public List<ColumnIdentifier> getColumnsFromFile()
	{
		BufferedReader in = null;
		List<ColumnIdentifier> cols = new ArrayList<>();
		try
		{
			// Make sure the file handler is initialized as this can be called from
			// the outside as well.
			setupFileHandler();
			in = this.fileHandler.getMainFileReader();
			String firstLine = in.readLine();
			WbStringTokenizer tok = new WbStringTokenizer(delimiter, this.quoteChar, false);
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

	@Override
	public void checkTargetTable()
		throws SQLException
	{
		TableDefinition def = getTargetTable();

		if (def == null || def.getColumns().isEmpty())
		{
			TableIdentifier tbl = createTargetTableId();
			String msg = ResourceMgr.getFormattedString("ErrTargetTableNotFound", tbl.getTableExpression());
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.importColumns = null;
			this.hasErrors = true;
			throw new SQLException("Table " + tbl.getTableExpression() + " not found!");
		}
	}

	@Override
	public void setupFileColumns(List<ColumnIdentifier> importColumns)
		throws SQLException, IOException
	{
		List<ColumnIdentifier> cols = null;

		if (this.withHeader)
		{
			cols = getColumnsFromFile();
		}
		else
		{
			TableDefinition def = getTargetTable();
			cols = def.getColumns();
		}
		setColumns(cols, importColumns);
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
	 * Setter for property emptyStringIsNull.
	 * @param flag New value of property emptyStringIsNull.
	 */
	public void setEmptyStringIsNull(boolean flag)
	{
		this.emptyStringIsNull = flag;
	}

	@Override
	public String getDecimalChar()
	{
		if (converter == null) return null;
		char c = converter.getDecimalCharacter();

		return new StringBuilder(1).append(c).toString();
	}

	@Override
	public void setDecimalChar(String delim)
	{
		if (converter != null && StringUtil.isNonBlank(delim))
		{
			converter.setDecimalCharacter(delim.charAt(0));
		}
	}

	@Override
	public boolean getDecode()
	{
		return this.decodeUnicode;
	}

	@Override
	public void setDecode(boolean flag)
	{
		this.decodeUnicode = flag;
	}


}
