/*
 * TextFileParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
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

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CsvLineParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class TextFileParser
	implements RowDataProducer
{
	private String filename;
	private String tableName;
	private String encoding = "8859_1";
	private String delimiter = "\t";
	private String quoteChar = null;
	private boolean decodeUnicode = false;

	private int colCount = -1;
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

	private ValueConverter converter;
	private StringBuffer messages = new StringBuffer(100);

	/** Creates a new instance of TextFileParser */
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

	public void setColumns(List columnList)
		throws Exception
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
		if (aChar == null) return;
		this.quoteChar = aChar;
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

		this.receiver.setTargetTable(this.tableName, this.columns);

		Object value = null;
		this.rowData = new Object[this.colCount];
		int importRow = 0;

		CsvLineParser tok = new CsvLineParser(delimiter.charAt(0), (quoteChar == null ? 0 : quoteChar.charAt(0)));

		try
		{
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

				this.clearRowData();
				importRow ++;

				tok.setLine(line);

				for (int i=0; i < this.colCount; i++)
				{
					try
					{
						if (tok.hasNext())
						{
							value = tok.getNext();
							if (this.decodeUnicode && SqlUtil.isCharacterType(this.columns[i].getDataType()))
							{
								value = StringUtil.decodeUnicode((String)value);
							}
							rowData[i] = converter.convertValue(value, this.columns[i].getDataType());
							if (this.emptyStringIsNull && SqlUtil.isCharacterType(this.columns[i].getDataType()))
							{
								String s = (String)rowData[i];
								if (s != null && s.length() == 0) rowData[i] = null;
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
					this.receiver.processRow(rowData);
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

	private void clearRowData()
	{
		// this is nearly as fast as using System.arrayCopy()
		// with a blank array...
		for (int i=0; i < this.colCount; i++)
		{
			this.rowData[i] = null;
		}
	}

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
	}

	private void readColumnDefinitions(List cols)
		throws Exception
	{
		try
		{
			ArrayList myCols = new ArrayList(cols);
			this.colCount = myCols.size();
			this.columns = new ColumnIdentifier[this.colCount];

			for (int i=0; i < this.colCount; i++)
			{
				String colname = ((String)myCols.get(i)).trim();
				this.columns[i] = new ColumnIdentifier(colname);
				myCols.set(i, colname.toUpperCase());
			}
			DbMetadata meta = this.connection.getMetadata();
			List colIds = meta.getTableColumns(new TableIdentifier(this.tableName));
			int tableCols = colIds.size();

			for (int i=0; i < tableCols; i++)
			{
				ColumnIdentifier id = (ColumnIdentifier)colIds.get(i);
				String column = id.getColumnName().toUpperCase();
				int index = myCols.indexOf(column);
				if (index >= 0)
				{
					this.columns[index].setDataType(id.getDataType());
				}
			}
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