/*
 * TextFileParser.java
 *
 * Created on November 22, 2003, 3:04 PM
 */

package workbench.db.importer;

import java.io.BufferedReader;
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
import workbench.storage.DataStore;
import workbench.util.CsvLineParser;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TextFileParser
	implements RowDataProducer
{
	private String filename;
	private String tableName;
	private String encoding = "8859_1";
	private String delimiter = "\t";
	private String quoteChar = "\"";

	private int colCount = -1;
	private ColumnIdentifier[] columns;
	private Object[] rowData;

	private boolean withHeader = true;
	private boolean cancelImport = false;
	private RowDataReceiver receiver;
	private String dateFormat;
	private String timestampFormat;
	private char decimalChar;

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
	
	public void setDelimiter(String delimit)
	{
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
			if (this.withHeader && this.columns == null)
			{
				this.readColumns(line);
			}
			line = in.readLine();
		}
		catch (IOException e)
		{
			line = null;
		}

		if (this.colCount <= 0)
		{
			throw new Exception("Cannot import file without a column definition");
		}

		this.receiver.setTargetTable(this.tableName, this.columns);

		Object value = null;
		this.rowData = new Object[this.colCount];
		int importRow = 0;
		CsvLineParser tok = new CsvLineParser(delimiter.charAt(0), '"');

		while (line != null)
		{
			if (this.doCancel()) break;

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
						rowData[i] = converter.convertValue(value, this.columns[i].getDataType());
					}
				}
				catch (Exception e)
				{
					LogMgr.logWarning("TextFileParser.start()","Error in line=" + importRow + "reading col=" + i + ",value=" + value, e);
					rowData[i] = null;
					String msg = ResourceMgr.getString("ErrorTextfileImport");
					msg = msg.replaceAll("%row%", Integer.toString(importRow + 1));
					//msg = msg.replaceAll("%col%", this.columns[i].getName());
					msg = msg.replaceAll("%value%", (value == null ? "" : value.toString()));
					msg = msg.replaceAll("%msg%", e.getClass().getName() + ": " + ExceptionUtil.getDisplay(e, false));
					this.messages.append(msg);
					this.messages.append("\n");
				}
			}

			if (this.doCancel()) break;

			this.receiver.processRow(rowData);

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

		try { in.close(); } catch (IOException e) {}

		if (!this.cancelImport)
		{
			this.receiver.importFinished();
		}
		else
		{
			this.receiver.importCancelled();
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
				String colname = (String)myCols.get(i);
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
					this.columns[i].setDataType(id.getDataType());
				}
			}
		}
		catch (Exception e)
		{
			this.colCount = -1;
			this.columns = null;
		}
	}


}