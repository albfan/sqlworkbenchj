/*
 * TextFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.util.ExceptionUtil;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CsvLineParser;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import workbench.util.WbFile;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TextFileParser
	implements RowDataProducer, ImportFileParser
{
	private String filename;
	private File baseDir;
	private String tableName;
	private String encoding = "ISO-8859-1";
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean decodeUnicode = false;
	private boolean enableMultiLineMode = true;
	
	private int colCount = -1;
	private int importColCount = -1;

	private ColumnIdentifier[] columns;
	// for each column from columns
	// the value for the respective index
	// defines its real index
	// if the value is -1 then the column
	// will not be imported
	private int[] columnMap;
	private Object[] rowData;

	private boolean withHeader = true;
	private boolean cancelImport = false;
	private boolean emptyStringIsNull = false;
	private boolean trimValues = false;

	private RowDataReceiver receiver;
	private String dateFormat;
	private String timestampFormat;
	private char decimalChar = '.';
	private boolean abortOnError = false;
	private WbConnection connection;
	private String sourceDir;
	private String extensionToUse;
	private JobErrorHandler errorHandler;
	private List pendingImportColumns;
	private ValueConverter converter;
	private StringBuffer messages;

	// If a filter for the input file is defined
	// this will hold the regular expressions per column
	private Pattern[] columnFilter;
	private Pattern lineFilter;
	private String targetSchema;
	private boolean blobsAreFilenames = true;

	private ImportFileHandler fileHandler = new ImportFileHandler();
	
	public TextFileParser()
	{
	}

	public TextFileParser(String aFile)
	{
		this.filename = aFile;
		this.sourceDir = null;
	}

	public ImportFileHandler getFileHandler()
	{
		return this.fileHandler;
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

	public void setInputFile(String file)
	{
		this.sourceDir = null;
		this.filename = file;
	}

	public void setSourceExtension(String ext)
	{
		if (ext != null && ext.trim().length() == 0) return;
		this.extensionToUse = ext;
	}

	public void setSourceDirectory(String dir)
	{
		this.sourceDir = dir;
		this.filename = null;
	}

	public void setTableName(String aName)
	{
		this.tableName = aName;
	}

	public void importAllColumns()
	{
		this.columnMap = new int[this.colCount];
		for (int i=0; i < this.colCount; i++) this.columnMap[i] = i;
		this.importColCount = this.colCount;
	}

	public String getSourceFilename()
	{
		return this.filename;
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
			LogMgr.logError("TextFileParser.addColumnFilter()", "Error compiling regular expression " + regex, e);
		}
	}

	public boolean hasColumnFilter()
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
			this.columnFilter[index] = null;
		}
	}

	public void setTreatBlobsAsFilenames(boolean flag)
	{
		this.blobsAreFilenames = flag;
	}
	
	/**
	 * 	Define the columns that should be imported.
	 * 	If the list is empty or null, then all columns will be imported
	 */
	public void setImportColumns(List columnList)
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
			this.pendingImportColumns = new ArrayList();
			for (int i=0; i < count; i++)
			{
				Object o = columnList.get(i);
				if (o instanceof String)
				{
					String colname = (String)columnList.get(i);
					pendingImportColumns.add(new ColumnIdentifier(colname));
				}
				else if (o instanceof ColumnIdentifier)
				{
					pendingImportColumns.add((ColumnIdentifier)o);
				}
					
			}
			return;
		}

		List colIds = new ArrayList(count);

		// If we have pending import columns,
		// then use only those that are defined there
		if (this.pendingImportColumns != null)
		{
			for (int i=0; i < count; i++)
			{
				Object o = columnList.get(i);
				if (o instanceof ColumnIdentifier)
				{
					colIds.add(o);
				}
				else
				{
					String columnName = o.toString();
					colIds.add(new ColumnIdentifier(columnName));
				}
			}
			colIds.retainAll(this.pendingImportColumns);
		}
		else
		{
			colIds = columnList;
		}

		this.columnMap = new int[this.colCount];
		for (int i=0; i < this.colCount; i++) this.columnMap[i] = -1;
		this.importColCount = 0;

		for (int i=0; i < count; i++)
		{
			// We use toString() so that either ColumnIds or Strings
			// can be put into the passed list
			Object o = colIds.get(i);
			String columnName = o.toString();
			int index = this.getColumnIndex(columnName);
			if (index > -1)
			{
				this.columnMap[index] = i;
				this.importColCount ++;
			}
		}
	}

	private ColumnIdentifier[] getColumnsToImport()
	{
		if (this.columnMap == null) return this.columns;
		if (this.importColCount == this.colCount) return this.columns;
		ColumnIdentifier[] result = new ColumnIdentifier[this.importColCount];
		int col = 0;
		for (int i=0; i < this.colCount; i++)
		{
			if (this.columnMap[i] != -1)
			{
				result[col] = this.columns[i];
				col++;
			}
		}
		return result;
	}
	/**
	 *	Return the index of the specified column
	 *  in the import file
	 */
	private int getColumnIndex(String colName)
	{
		if (colName == null) return -1;
		if (this.colCount < 1) return -1;
		if (this.columns == null) return -1;
		for (int i=0; i < this.colCount; i++)
		{
			if (this.columns[i] != null && colName.equalsIgnoreCase(this.columns[i].getColumnName())) return i;
		}
		return -1;
	}

	/**
	 * 	Define the columns in the input file.
	 */
	public void setColumns(List columnList)
		throws SQLException
	{
		if (columnList == null || columnList.size()  == 0) return;
		if (this.connection != null && this.tableName != null)
		{
			this.readColumnDefinition(columnList);
		}
		else
		{
			this.colCount = columnList.size();
			this.columns = new ColumnIdentifier[colCount];
			for (int i = 0; i < columns.length; i++)
			{
				this.columns[i] = (ColumnIdentifier)columnList.get(i);
			}
			this.importAllColumns();
		}
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}
	public void setEncoding(String enc)
	{
		if (enc == null) return;
		this.encoding = enc;
	}

	public String getMessages()
	{
		if (messages == null) return null;
		return this.messages.toString();
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

	public void cancel()
	{
		LogMgr.logDebug("TextFileParser.cancel()", "Cancelling import");
		this.cancelImport = true;
	}

	public void setDateFormat(String aFormat)
	{
		this.dateFormat = aFormat;
	}

	public void setTimeStampFormat(String aFormat)
	{
		this.timestampFormat = aFormat;
	}

	public void setContainsHeader(boolean aFlag)
	{
		this.withHeader = aFlag;
	}

	public void setQuoteChar(String aChar)
	{
		if (aChar != null && aChar.trim().length() > 0)
		{
			this.quoteChar = aChar;
		}
		else
		{
			this.quoteChar = null;
		}
	}

	public void setDecimalChar(String aChar)
	{
		if (aChar != null && aChar.trim().length() > 0)
		{
			this.decimalChar = aChar.trim().charAt(0);
		}
	}

	public void start()
		throws Exception
	{
		this.converter = new ValueConverter(this.dateFormat, this.timestampFormat);
		this.converter.setDecimalCharacter(this.decimalChar);
		this.receiver.setTableCount(-1); // clear multi-table flag in receiver
		this.receiver.setCurrentTable(-1);

		try
		{
			if (this.sourceDir != null)
				processDirectory();
			else
				processOneFile();
		}
		finally
		{
			if (this.cancelImport)
			{
				this.receiver.importCancelled();
			}
			else
			{
				this.receiver.importFinished();
			}
		}
	}

	private void processDirectory()
		throws Exception
	{
		File dir = new File(this.sourceDir);
		File[] files = dir.listFiles();
		Arrays.sort(files);
		int count = files.length;
		if (this.extensionToUse == null) this.extensionToUse = ".txt";

		List toProcess = new LinkedList();
		for (int i=0; i < count; i++)
		{
			if (files[i].getName().endsWith(this.extensionToUse) )
			{
				toProcess.add(files[i]);
			}
		}
		count = toProcess.size();
		this.receiver.setTableCount(count);
		int currentFile=0;
		Iterator itr = toProcess.iterator();
		while (itr.hasNext())
		{
			if (this.cancelImport)
			{
				break;
			}
			WbFile theFile = new WbFile((File)itr.next());
			try
			{
				currentFile++;
				this.messages = new StringBuffer();
				this.receiver.setCurrentTable(currentFile);
				this.filename = theFile.getAbsolutePath();
				this.tableName = theFile.getFileName();
				this.columns = null;
				this.colCount = 0;
				this.columnMap = null;
				this.processOneFile();
			}
			catch (Exception e)
			{
				this.receiver.tableImportError();
				if (this.abortOnError) throw e;
			}
		}
	}

	private void setupFileHandler()
		throws IOException
	{
		File f = new File(this.filename);
		this.fileHandler.setMainFile(f, this.encoding);
	}
	
	private void processOneFile()
		throws Exception
	{
		this.cancelImport = false;
		
		File f = new File(this.filename);
		
		if (f.isAbsolute())
		{
			this.baseDir = f.getParentFile();
		}
		if (baseDir == null) this.baseDir = new File(".");
		
		setupFileHandler();
		
		String line;

		if (!this.withHeader && this.sourceDir != null)
		{
			this.setColumns(this.getColumnsFromTargetTable());
		}
		
		BufferedReader in = this.fileHandler.getMainFileReader();

		String lineEnding = StringUtil.LINE_TERMINATOR;
		if (enableMultiLineMode)
		{
			try
			{
				lineEnding = FileUtil.getLineEnding(in);
			}
			catch (IOException io)
			{
				LogMgr.logError("TextFileParser.processOneFile()", "Could not read line ending from file. Multi-line mode disabled!", io);
				this.messages.append(ResourceMgr.getString("ErrNoMultiLine") + "\n");
				enableMultiLineMode = false;
			}
			LogMgr.logDebug("TextFileParser.processOneFile()", "Using line ending: " + lineEnding.replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n"));
			// now that we have already used the Reader supplied by the fileHandler, 
			// close and re-open that. To make sure we start at the beginning
			// as we cannot rely on mark() and reset() to be available for the ZIP archives
			// setupFileHandler will re-initialize the ImportFileHandler 
			// which in turn will take care of setting up the 
			in.close();
			setupFileHandler();
			in = this.fileHandler.getMainFileReader();
		}
		
		try
		{
			line = in.readLine();
			if (this.withHeader)
			{
				if (this.columns == null) this.readColumns(line);
				line = in.readLine();
			}
		}
		catch (EOFException eof)
		{
			line = null;
		}
		catch (IOException e)
		{
			LogMgr.logWarning("TextFileParser.processOneFile()", "Error reading input file " + f.getAbsolutePath(), e);
			try { in.close(); } catch (Throwable ignore) {}
			throw e;
		}
		catch (SQLException e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Column definition could not be read.", e);
			try { in.close(); } catch (Throwable ignore) {}
			throw e;
		}

		if (this.colCount <= 0)
		{
			throw new Exception("Cannot import file without a column definition");
		}

		ColumnIdentifier[] cols = this.getColumnsToImport();
		try
		{
			this.receiver.setTargetTable(this.tableName, cols);
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.processOneFile()", "Error setting target table", e);
			throw e;
		}

		String value = null;
		this.rowData = new Object[this.importColCount];
		int importRow = 0;

		char quoteCharToUse = (quoteChar == null ? '"' : quoteChar.charAt(0));
		CsvLineParser tok = new CsvLineParser(delimiter.charAt(0), quoteCharToUse);
		tok.setReturnEmptyStrings(true);
		tok.setTrimValues(this.trimValues);

		try
		{
			boolean includeLine = true;
			boolean hasColumnFilter = this.hasColumnFilter();
			boolean hasLineFilter = this.lineFilter != null;

			while (line != null)
			{
				if (this.cancelImport) break;

				// silently ignore empty lines...
				if (StringUtil.isEmptyString(line))
				{
					try
					{
						line = in.readLine();
					}
					catch (IOException e)
					{
						line = null;
					}
					continue;
				}

				if (enableMultiLineMode && StringUtil.hasOpenQuotes(line, quoteCharToUse))
				{
					try
					{
						StringBuffer b = new StringBuffer(line.length() * 2);
						b.append(line);
						b.append(lineEnding);
						String nextLine = in.readLine();
						if (nextLine != null) b.append(nextLine);
						line = b.toString();
						continue;
					}
					catch (IOException e)
					{
						LogMgr.logError("TextFileParser.processOneFile()", "Could not read next line for multi-line record", e);
					}
				}
				
				if (hasLineFilter)
				{
					Matcher m = this.lineFilter.matcher(line);
					if (!m.matches())
					{
						try
						{
							line = in.readLine();
						}
						catch (IOException e)
						{
							line = null;
						}
						continue;
					}
				}

				this.clearRowData();
				importRow ++;

				tok.setLine(line);
				includeLine = true;
				int targetIndex = -1;
				
				// Build row data
				for (int i=0; i < this.colCount; i++)
				{
					try
					{
						if (tok.hasNext())
						{
							value = tok.getNext();
							if (this.columns[i] == null) continue;
							targetIndex = this.columnMap[i];
							if (targetIndex == -1) continue;
							
							int colType = this.columns[i].getDataType();

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

							if (SqlUtil.isCharacterType(colType))
							{
								if (this.decodeUnicode)
								{
									value = StringUtil.decodeUnicode((String)value);
								}
								if (this.emptyStringIsNull && StringUtil.isEmptyString(value))
								{
									value = null;
								}
							}
							
							if (value != null && SqlUtil.isBlobType(colType) && blobsAreFilenames)
							{
								File bfile = new File(value);
								if (!bfile.isAbsolute())
								{
									bfile = new File(this.baseDir, value);
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
						msg = msg.replaceAll("%row%", Integer.toString(importRow + 1));
						msg = msg.replaceAll("%col%", (this.columns[i] == null ? "n/a" : this.columns[i].getColumnName()));
						msg = msg.replaceAll("%value%", (value == null ? "(NULL)" : value.toString()));
						msg = msg.replaceAll("%msg%", e.getClass().getName() + ": " + ExceptionUtil.getDisplay(e, false));
						LogMgr.logWarning("TextFileParser.start()",msg, e);
						if (this.messages == null) this.messages = new StringBuffer();
						this.messages.append(msg);
						this.messages.append("\n");
						if (this.abortOnError) throw e;
						if (this.errorHandler != null)
						{
							int choice = errorHandler.getActionOnError(importRow + 1, this.columns[i].getColumnName(), (value == null ? "(NULL)" : value.toString()), ExceptionUtil.getDisplay(e, false));
							if (choice == JobErrorHandler.JOB_ABORT) throw e;
							if (choice == JobErrorHandler.JOB_IGNORE_ALL) 
							{
								this.abortOnError = false;
//								this.ignoreAllErrors = true;
							}
						}
					}
				}

				if (this.cancelImport) break;

				try
				{
					if (includeLine) this.receiver.processRow(rowData);
				}
				catch (Exception e)
				{
					LogMgr.logError("TextFileParser.start()", "Error sending line " + importRow + ". Aborting...", e);
					throw e;
				}

				try
				{
					line = in.readLine();
				}
				catch (IOException e)
				{
					line = null;
				}

				if (this.cancelImport) break;
			}
		}
		finally
		{
			try { in.close(); } catch (IOException e) {}
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
		List cols = new ArrayList();
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
			this.setImportColumns(this.pendingImportColumns);
			this.pendingImportColumns = null;
		}
	}

	/**
	 *	Return a list of {@link workbench.db.ColumnIdentifier} objects determined
	 *	by the input file. The identifiers will only have a name but 
	 *  no data type assigned as this information is not available in a text file
	 *  If the input file does not contain a header row, the columns
	 *  will be named Column1, Column2, ...
	 */
	public List getColumnsFromFile()
	{
		BufferedReader in = null;
		List cols = new ArrayList();
		try
		{
			in = this.fileHandler.getMainFileReader();
			String firstLine = in.readLine();
			WbStringTokenizer tok = new WbStringTokenizer(delimiter.charAt(0), this.quoteChar, false);
			tok.setSourceString(firstLine);
			int i = 1;
			while (tok.hasMoreTokens())
			{
				String column = tok.nextToken();
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
			LogMgr.logError("TextFileParser.getColumnsFromFile()", "Error when reading columns", e);
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return cols;
	}

	
	public void setupFileColumns()
		throws SQLException, IOException
	{
	
		List cols = null;
		
		if (this.withHeader)
		{
			setupFileHandler();
			cols = this.getColumnsFromFile();
		}
		else
		{
			cols = this.getColumnsFromTargetTable();
		}
		this.setColumns(cols);
	}
	
	private List getColumnsFromTargetTable()
		throws SQLException
	{
		TableIdentifier tbl = new TableIdentifier(this.targetSchema, this.tableName);
		return this.connection.getMetadata().getTableColumns(tbl);
	}
	
	/**
	 * 	Read the column definitions from the database.
	 * 	@param cols a List of column names (String)
	 */
	private void readColumnDefinition(List cols)
		throws SQLException
	{
		if (this.messages == null) messages = new StringBuffer();
		try
		{
			this.colCount = cols.size();
			this.columns = new ColumnIdentifier[colCount];
			ArrayList realCols = new ArrayList();
			boolean partialImport = false;
			DbMetadata meta = this.connection.getMetadata();
			TableIdentifier targetTable = new TableIdentifier(this.tableName);
			if (this.targetSchema != null)
			{
				targetTable.setSchema(this.targetSchema);
			}
			targetTable.adjustCase(this.connection);
			if (targetTable.getSchema() == null)
			{
				targetTable.setSchema(meta.getCurrentSchema());
			}
			List tableCols = meta.getTableColumns(targetTable);
			int numTableCols = tableCols.size();

			if (numTableCols == 0)
			{
				String msg = ResourceMgr.getString("ErrImportTableNotFound").replaceAll("%table%", targetTable.getTableExpression());
				msg = StringUtil.replace(msg, "%filename%", this.filename);
				this.messages.append(msg + "\n\n");
				this.columns = null;
				throw new SQLException("Table " + targetTable.getTableExpression() + " not found!");
			}

			for (int i=0; i < cols.size(); i++)
			{
				ColumnIdentifier col = (ColumnIdentifier)cols.get(i);
				String colname = col.getColumnName();
				if (colname.toLowerCase().startsWith(RowDataProducer.SKIP_INDICATOR) )
				{
					partialImport = true;
					this.columns[i] = null;
				}
				else
				{
					int index = tableCols.indexOf(col);
					if (index > -1)
					{
						this.columns[i] = (ColumnIdentifier)tableCols.get(index);
						realCols.add(this.columns[i].getColumnName());
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
								this.messages.append(msg + "\n");
								throw new SQLException(msg);
							}
							else
							{
								String msg = ResourceMgr.getString("ErrImportColumnIgnored");
								msg = StringUtil.replace(msg, "%column%", colname);
								msg = StringUtil.replace(msg, "%table%", this.tableName);
								LogMgr.logWarning("TextFileParser.readColumns()", msg);
								this.messages.append(msg + "\n");
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
				this.messages.append(msg + "\n");
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
		StringBuffer result = new StringBuffer(this.colCount * 10);

		if (this.columnMap == null || this.importColCount == this.colCount)
		{
			for (int i=0; i < this.colCount; i++)
			{
				if (i > 0) result.append(',');
				result.append(this.columns[i].getColumnName());
			}
		}
		else
		{
			for (int i=0; i < this.colCount; i++)
			{
				if (i > 0) result.append(',');
				if (this.columnMap[i] != -1)
				{
					result.append(this.columns[i].getColumnName());
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
	 * @param emptyStringIsNull New value of property emptyStringIsNull.
	 */
	public void setEmptyStringIsNull(boolean emptyStringIsNull)
	{
		this.emptyStringIsNull = emptyStringIsNull;
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

	public void setTrimValues(boolean trimValues)
	{
		this.trimValues = trimValues;
	}

	public void setErrorHandler(JobErrorHandler handler)
	{
		this.errorHandler = handler;
	}

}
