/*
 * TextFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CsvLineParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * @author  info@sql-workbench.net
 */
public class TextFileParser
	implements RowDataProducer, ImportFileParser
{
	private String filename;
	private String tableName;
	private String encoding = "8859_1";
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean decodeUnicode = false;

	private int colCount = -1;
	private int importColCount = -1;

	private ColumnIdentifier[] columns;
	private Object[] rowData;

	private boolean withHeader = true;
	private boolean cancelImport = false;
	private boolean emptyStringIsNull = false;

	private RowDataReceiver receiver;
	private String dateFormat;
	private String timestampFormat;
	private char decimalChar = '.';
	private boolean abortOnError = false;
	private WbConnection connection;

	// for each column from columns
	// the value for the respective index
	// defines its real index
	// if the value is -1 then the column
	// will not be imported
	private int[] columnMap;
	private List pendingImportColumns;
	private ValueConverter converter;
	private StringBuffer messages = new StringBuffer(100);

	// If a filter for the input file is defined
	// this will hold the regular expressions per column
	private Pattern[] columnFilter;
	private Pattern lineFilter;

	public TextFileParser(String aFile)
	{
		this.filename = aFile;
	}

	public void setReceiver(RowDataReceiver rec)
	{
		this.receiver = rec;
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

	/**
	 * 	Define the columns that should be imported.
	 * 	If this is not defined, then all columns will be imported
	 */
	public void setImportColumns(List columnList)
	{
		if (this.columns == null && this.withHeader)
		{
			// store the list so that when the columns
			// are retrieved from the header row, the import columns
			// can be defined
			this.pendingImportColumns = columnList;
			return;
		}

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

		this.columnMap = new int[this.colCount];
		for (int i=0; i < this.colCount; i++) this.columnMap[i] = -1;
		this.importColCount = 0;

		for (int i=0; i < count; i++)
		{
			Object o = columnList.get(i);
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
	{
		if (columnList == null || columnList.size()  == 0) return;
		this.readColumnDefinitions(columnList);
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

	private boolean doCancel()
	{
		Thread.yield();
		return this.cancelImport;
	}

	public void cancel()
	{
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
		this.cancelImport = false;
		File f = new File(this.filename);
		long fileSize = f.length();

		InputStream inStream = new FileInputStream(f);
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream, this.encoding),1024*256);

		this.converter = new ValueConverter(this.dateFormat, this.timestampFormat);
		this.converter.setDecimalCharacter(this.decimalChar);

		String line;
		int col;
		int row;

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
			LogMgr.logWarning("TextFileParser.start()", "Error reading input file " + f.getAbsolutePath(), e);
			throw e;
		}

		if (this.colCount <= 0)
		{
			throw new Exception("Cannot import file without a column definition");
		}

		ColumnIdentifier[] cols = this.getColumnsToImport();
		this.receiver.setTargetTable(this.tableName, cols);

		String value = null;
		this.rowData = new Object[this.importColCount];
		int importRow = 0;

		CsvLineParser tok = new CsvLineParser(delimiter.charAt(0), (quoteChar == null ? 0 : quoteChar.charAt(0)));
		tok.setReturnEmptyStrings(true);

		try
		{
			boolean includeLine = true;
			boolean hasColumnFilter = this.hasColumnFilter();
			boolean hasLineFilter = this.lineFilter != null;

			while (line != null)
			{
				if (this.doCancel()) break;

				// silently ignore empty lines...
				if (line.trim().length() == 0)
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

				// Build row data
				for (int i=0; i < this.colCount; i++)
				{
					try
					{
						if (tok.hasNext())
						{
							value = tok.getNext();

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

							int targetIndex = this.columnMap[i];
							if (targetIndex == -1) continue;

							if (this.decodeUnicode && SqlUtil.isCharacterType(this.columns[i].getDataType()))
							{
								value = StringUtil.decodeUnicode((String)value);
							}
							rowData[targetIndex] = converter.convertValue(value, this.columns[i].getDataType());

							if (this.emptyStringIsNull && SqlUtil.isCharacterType(this.columns[i].getDataType()))
							{
								String s = (String)rowData[targetIndex];
								if (s != null && s.length() == 0) rowData[targetIndex] = null;
							}
						}
					}
					catch (Exception e)
					{
						rowData[i] = null;
						String msg = ResourceMgr.getString("ErrorTextfileImport");
						msg = msg.replaceAll("%row%", Integer.toString(importRow + 1));
						msg = msg.replaceAll("%col%", this.columns[i].getColumnName());
						msg = msg.replaceAll("%value%", (value == null ? "(NULL)" : value.toString()));
						msg = msg.replaceAll("%msg%", e.getClass().getName() + ": " + ExceptionUtil.getDisplay(e, false));
						LogMgr.logWarning("TextFileParser.start()",msg, e);
						this.messages.append(msg);
						this.messages.append("\n");
						if (this.abortOnError) throw e;
					}
				}

				if (this.doCancel()) break;

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

				if (this.doCancel()) break;
			}

			if (!this.cancelImport)
			{
				this.receiver.importFinished();
			}
			else
			{
				this.receiver.importCancelled();
			}
		}
		finally
		{
			try { in.close(); } catch (IOException e) {}
		}

	}

	/*
	private int mapInputColumnIndex(int i)
	{
		if (this.columnMap == null) return i;
		return this.columnMap[i];
	}
	*/

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
		tok.setSourceString(headerLine);
		while (tok.hasMoreTokens())
		{
			String column = tok.nextToken();
			cols.add(column.toUpperCase());
		}
		this.readColumnDefinitions(cols);
		if (this.pendingImportColumns != null)
		{
			this.setImportColumns(this.pendingImportColumns);
			this.pendingImportColumns = null;
		}
	}
	
	/**
	 *	Return a list of ColumnIdentifier objects determined
	 *	by the input file. The identifiers will only have a name
	 *  not data type assigned. 
	 *  If the input file does not contain a header row, the columns
	 *  will be named Column1, Column2, ...
	 */
	public List getColumnsFromFile()
	{
		BufferedReader in = null;
		List cols = new ArrayList();
		try
		{
			File f = new File(this.filename);
			InputStream inStream = new FileInputStream(f);
			in = new BufferedReader(new InputStreamReader(inStream, this.encoding));
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

	/**
	 * 	Read the column definitions from the database.
	 * 	@param cols a List of column names (String)
	 */
	private void readColumnDefinitions(List cols)
	{
		try
		{
			ArrayList myCols = new ArrayList(cols);
			this.colCount = myCols.size();
			this.columns = new ColumnIdentifier[this.colCount];

			boolean skipPresent = false;
			ArrayList realCols = new ArrayList(this.colCount);

			for (int i=0; i < this.colCount; i++)
			{
				String colname = ((String)myCols.get(i)).trim();
				if (colname.toLowerCase().startsWith(RowDataProducer.SKIP_INDICATOR))
				{
					this.columns[i] = null;
					skipPresent = true;
				}
				else
				{
					this.columns[i] = new ColumnIdentifier(colname);
					realCols.add(colname);
				}
				myCols.set(i, colname.toUpperCase());
			}
			DbMetadata meta = this.connection.getMetadata();
			ColumnIdentifier[] colIds = meta.getColumnIdentifiers(new TableIdentifier(this.tableName));
			int tableCols = colIds.length;

			for (int i=0; i < tableCols; i++)
			{
				ColumnIdentifier id = colIds[i];
				String column = id.getColumnName().toUpperCase();
				int index = myCols.indexOf(column);
				if (index >= 0 && this.columns[index] != null)
				{
					this.columns[index].setDataType(id.getDataType());
				}
			}
			// reset mapping
			this.importAllColumns();
			if (skipPresent) this.setImportColumns(realCols);
		}
		catch (Exception e)
		{
			LogMgr.logError("TextFileParser.readColumnDefinition()", "Error when reading column definition", e);
			this.colCount = -1;
			this.columns = null;
		}

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

}
