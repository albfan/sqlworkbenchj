/*
 * XmlDataFileParser.java
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import workbench.db.ColumnIdentifier;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class XmlDataFileParser
	extends DefaultHandler
	implements RowDataProducer, ImportFileParser
{
	private String inputFile;
	private String tableName;
	
	private int currentRowNumber = 1;
	private int colCount;
	private int realColCount;

	private List columnsToImport;
	private ColumnIdentifier[] columns;
	private String[] colFormats;
	private String encoding = "UTF-8";

	private Object[] currentRow;
	private RowDataReceiver receiver;
	private boolean ignoreCurrentRow = false;
	private boolean abortOnError = false;
	private boolean verboseFormat = false;
	
	/** Define the tags for which the characters surrounded by the
	 *  tag should be collected
	 */
	private static final HashSet DATA_TAGS = new HashSet();
	static
	{
		DATA_TAGS.add("java-class");
		DATA_TAGS.add("java-sql-type");
		DATA_TAGS.add("dbms-data-type");
		DATA_TAGS.add("data-format");
		DATA_TAGS.add("table-name");
		DATA_TAGS.add("column-name");
		DATA_TAGS.add(XmlRowDataConverter.LONG_COLUMN_TAG);
		DATA_TAGS.add(XmlRowDataConverter.SHORT_COLUMN_TAG);
		DATA_TAGS.add("column-count");
	}

	private int currentColIndex = 0;
	private int realColIndex = 0;
	private boolean ignoreCurrentColumn = false;
	private long columnLongValue = 0;
	private boolean hasLongValue = false;
	private boolean isNull = false;
	private StringBuffer chars;
	private boolean keepRunning;
	private boolean readTableStructure;
	private String rowTag = XmlRowDataConverter.LONG_ROW_TAG;
	private String columnTag = XmlRowDataConverter.LONG_COLUMN_TAG;
	
	private HashMap constructors = new HashMap();

	public XmlDataFileParser(String inputFile)
	{
		this.inputFile = inputFile;
	}

	public void setColumns(String columnList)
	{
		if (columnList != null && columnList.trim().length() > 0)
		{
			WbStringTokenizer tok = new WbStringTokenizer(columnList, ",");
			this.columnsToImport = new ArrayList(15);
			while (tok.hasMoreTokens())
			{
				String col = tok.nextToken();
				if (col == null) continue;
				col = col.trim();
				if (col.length() == 0) continue;
				ColumnIdentifier ci = new ColumnIdentifier(col);
				this.columnsToImport.add(ci);
			}
		}
		else
		{
			this.columnsToImport = null;
		}
	}
	
	/**	 Define the columns to be imported
	 */
	public void setColumns(List columns)
	{
		if (columns != null && columns.size() > 0)
		{
			this.columnsToImport = new ArrayList();
			Iterator itr = columns.iterator();
			while (itr.hasNext())
			{
				Object o = itr.next();
				if (o == null) continue;
				if (o instanceof ColumnIdentifier)
				{
					ColumnIdentifier id = (ColumnIdentifier)o;
					if (!id.getColumnName().equals(RowDataProducer.SKIP_INDICATOR))
					{
						this.columnsToImport.add(id);
					}
				}
				else 
				{
					String colname = o.toString();
					if (!colname.equals(RowDataProducer.SKIP_INDICATOR))
					{
						ColumnIdentifier id = new ColumnIdentifier(colname);
						this.columnsToImport.add(id);
					}
				}
			}
		}
		else
		{
			this.columnsToImport = null;
		}
	}
	
	public void setTableName(String aName)
	{
		this.tableName = aName;
	}

	public List getColumnsFromFile()
	{
		this.parseTableStructure();
		if (this.columns == null) return Collections.EMPTY_LIST;
		ArrayList result = new ArrayList(this.columns.length);
		for (int i=0; i < this.columns.length; i++)
		{
			result.add(this.columns[i]);
		}
		return result;
	}
	
	public String getSourceFilename()
	{
		return this.inputFile;
	}
	
	public void setAbortOnError(boolean flag)
	{
		this.abortOnError = flag;
	}
	
	public void start()
		throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		InputStream in = null;
		try
		{
			SAXParser saxParser = factory.newSAXParser();
			in = new FileInputStream(this.inputFile);
			InputSource source = new InputSource(new BufferedReader(new InputStreamReader(in, this.encoding)));
			this.keepRunning = true;
			saxParser.parse(source, this);
			this.receiver.importFinished();
		}
		catch (ParsingInterruptedException e)
		{
			this.receiver.importCancelled();
			throw e;
		}
		catch (Exception e)
		{
			this.receiver.importCancelled();
			throw e;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}

	private void parseTableStructure()
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		InputStream in = null;
		try
		{
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			in = new FileInputStream(this.inputFile);
			InputSource source = new InputSource(new BufferedReader(new InputStreamReader(in, this.encoding)));
			this.readTableStructure = true;
			this.keepRunning = true;
			saxParser.parse(source, this);
		}
		catch (ParsingEndedException e)
		{
			// ignore this, this is thrown, when only the 
			// table definition is read
		}
		catch (Throwable e)
		{
			LogMgr.logError("XmlDataFileParser.parseTableStructure()", "Error reading table structure", e);
			this.columns = null;
			this.tableName = null;
		}
		finally
		{
			this.readTableStructure = false;
		}
	}
	
	public void cancel()
	{
		this.keepRunning = false;
	}

	private void clearRowData()
	{
		for (int i=0; i < this.realColCount; i++)
		{
			this.currentRow[i] = null;
		}
		this.currentColIndex = 0;
		this.realColIndex = 0;
	}

	public void setEncoding(String enc)
	{
		this.encoding = enc;
	}

	public void setReceiver(RowDataReceiver aReceiver)
	{
		this.receiver = aReceiver;
	}

	public void startDocument()
		throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}

	public void endDocument()
		throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}

	public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
		throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
		this.chars = null;
		if (DATA_TAGS.contains(qName))
		{
			this.chars = new StringBuffer();
		}

		if (qName.equals(this.rowTag))
		{
			// row definition ended, start a new row
			this.clearRowData();
		}
		else if (qName.equals(this.columnTag))
		{
			hasLongValue = false;
			String attrValue = attrs.getValue("longValue");
			if (attrValue != null)
			{
				try
				{
					columnLongValue = Long.parseLong(attrValue);
					hasLongValue = true;
				}
				catch (NumberFormatException e)
				{
					hasLongValue = false;
				}
			}
			attrValue = attrs.getValue("null");
			this.isNull = "true".equals(attrValue);
		}
	}

	public void endElement(String namespaceURI, String sName, String qName)
		throws SAXException
	{
		if (!this.keepRunning) throw new ParsingInterruptedException();
		if (qName.equals(this.rowTag))
		{
			if (!this.ignoreCurrentRow)
			{
				try
				{
					this.sendRowData();
				}
				catch (Exception e)
				{
					// don't need to log the error as sendRowData() has already done that.
					if (this.abortOnError) throw new ParsingInterruptedException();
				}
			}
			this.ignoreCurrentRow = false;
			this.clearRowData();
			this.currentRowNumber ++;
		}
		else if (qName.equals(this.columnTag))
		{
			this.buildColumnData();
			this.currentColIndex ++;
		}
		else if (qName.equals("column-def"))
		{
			this.currentColIndex ++;
		}
		else if (qName.equals("table-name"))
		{
			if (this.tableName == null)
			{
				this.tableName = new String(this.chars);
			}
		}
		else if (qName.equals("table-def"))
		{
			try
			{
				if (this.readTableStructure)
				{
					throw new ParsingEndedException();
				}
				this.sendTableDefinition();
			}
			catch (SQLException sql)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Error when setting target table", sql);
				throw new SAXException("Could not initialize target table");
			}
		}
		// The following tags are for meta-data
		else if (qName.equals("column-count"))
		{
			try
			{
				this.colCount = Integer.parseInt(this.chars.toString());
				this.columns = new ColumnIdentifier[this.colCount];
				this.colFormats = new String[this.colCount];
				if (this.columnsToImport == null)
				{
					this.realColCount = this.colCount;
				}
				else
				{
					this.realColCount = this.columnsToImport.size();
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Error when setting column count", e);
				throw new SAXException("Invalid column-count (" + this.chars + ")");
			}
		}
		else if (qName.equals("column-name"))
		{
			try
			{
				ColumnIdentifier col = new ColumnIdentifier(this.chars.toString());
				this.columns[this.currentColIndex] = col;
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn name!", e);
				throw new SAXException("Could not read columnn name");
			}
		}
		else if (qName.equals("java-sql-type"))
		{
			try
			{
				this.columns[this.currentColIndex].setDataType(Integer.parseInt(this.chars.toString()));
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn type!", e);
				throw new SAXException("Could not read columnn type");
			}
		}
		else if (qName.equals("dbms-data-type"))
		{
			try
			{
				this.columns[this.currentColIndex].setDbmsType(this.chars.toString());
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read dbms columnn type!", e);
				throw new SAXException("Could not read dbms columnn type");
			}
		}
		else if (qName.equals("java-class"))
		{
			try
			{
				this.columns[this.currentColIndex].setColumnClass(this.chars.toString());
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn class name!", e);
				throw new SAXException("Could not read columnn name");
			}
		}
		this.chars = null;
	}

	public void characters(char buf[], int offset, int len)
		throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
		if (chars != null)
		{
			this.chars.append(buf, offset, len);
		}
	}

	/**	Only implemented to have even more possibilities for cancelling the import */
	public void ignorableWhitespace(char[] ch,int start,int length)
    throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}
	public void processingInstruction(String target,String data)
		throws SAXException
	{
		Thread.yield();
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}

	public void error(SAXParseException e)
		throws SAXParseException
	{
		LogMgr.logError("XmlDataFileParser.error()", "Error in line = " + e.getLineNumber() + ",data-row=" + this.currentRowNumber + "\n", e);
		this.ignoreCurrentRow = true;
		if (!this.keepRunning) throw e;
	}

	public void fatalError(SAXParseException e)
		throws SAXParseException
	{
		LogMgr.logError("XmlDataFileParser.fatalError()", "Fatal error in line = " + e.getLineNumber() + ",data-row=" + this.currentRowNumber + "\n", e);
		this.ignoreCurrentRow = true;
		if (!this.keepRunning) throw e;
	}

	// dump warnings too
	public void warning(SAXParseException err)
		throws SAXParseException
	{
		System.out.println("** Warning: line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println("   " + err.getMessage());
		if (!this.keepRunning) throw err;
	}

	/**
	 *	Creates the approriate column data object and puts it
	 *	into rowData[currentColIndex]
	 */
	private void buildColumnData()
	{
		if (this.columnsToImport != null && !this.columnsToImport.contains(this.columns[this.currentColIndex])) return;
		this.currentRow[this.realColIndex] = null;

		// the isNull flag will be set by the startElement method
		// as that is an attribute of the tag
		if (this.isNull)
		{
			return;
		}

		String value = this.chars.toString();
		int type = this.columns[this.realColIndex].getDataType();
		switch (type)
		{
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				this.currentRow[this.realColIndex] = value;
				break;

			case Types.TIME:
				if (hasLongValue)
				{
					this.currentRow[this.realColIndex] = new java.sql.Time(this.columnLongValue);
				}
				break;

			case Types.BIGINT:
				try
				{
					if (value.trim().length() > 0)
					{
						this.currentRow[this.realColIndex] = new BigInteger(value);
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not create BigInt value!", e);
					this.currentRow[this.realColIndex] = null;
				}
				break;

			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.TINYINT:
				try
				{
					if (value.trim().length() > 0)
					{
						this.currentRow[this.realColIndex] = new Integer(value);
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not create BigInt value!", e);
					this.currentRow[this.realColIndex] = null;
				}
				break;

			case Types.DATE:
			case Types.TIMESTAMP:
				java.sql.Date d = null;
				if (this.hasLongValue)
				{
					d = new java.sql.Date(this.columnLongValue);
				}
				else
				{
					try
					{
						SimpleDateFormat sdf = new SimpleDateFormat();
						java.util.Date ud = sdf.parse(value);
						d = new java.sql.Date(d.getTime());
					}
					catch (Exception e)
					{
						LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not convert data value " + value + " using format " + this.colFormats[this.currentColIndex], e);
						this.currentRow[this.realColIndex] = null;
						d = null;
					}
				}

				if (d != null)
				{
					if (type == Types.TIMESTAMP)
					{
						this.currentRow[this.realColIndex] = new java.sql.Timestamp(d.getTime());
					}
					else
					{
						this.currentRow[this.realColIndex] = d;
					}
				}
				break;

			case Types.DECIMAL:
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.REAL:
				Object result = this.createNumericType(this.columns[this.currentColIndex].getColumnClass(), value);
				this.currentRow[this.realColIndex] = result;
				break;
		}
		this.realColIndex ++;
	}

	private Object createNumericType(String aClass, String aValue)
	{
		Object result = null;
		Constructor con = null;
		try
		{
			con = (Constructor)this.constructors.get(aClass);
			if (con == null)
			{
				Class cls = Class.forName(aClass);
				con = cls.getConstructor(new Class[] { String.class });
				this.constructors.put(aClass, con);
			}
			result = con.newInstance(new Object[] { aValue });
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlDataFileParser.createNumericType()", "Could not create instance of " + aClass, e);
			result = null;
		}
		return result;
	}

	private void sendTableDefinition()
		throws SQLException
	{
		if (this.columnsToImport == null)
		{
			this.receiver.setTargetTable(this.tableName, this.columns);
		}
		else
		{
			ColumnIdentifier[] cols = new ColumnIdentifier[this.realColCount];
			int index = 0;
			for (int i=0; i < this.colCount; i++)
			{
				if (this.columnsToImport.contains(this.columns[i]))
				{
					cols[index] = this.columns[i];
					index ++;
				}
			}
			this.receiver.setTargetTable(this.tableName, cols);
		}
		this.currentRow = new Object[this.realColCount];
	}

	private void sendRowData()
		throws SAXException, Exception
	{
		if (this.receiver != null)
		{
			try
			{
				this.receiver.processRow(this.currentRow);
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.sendRowData()", "Error when sending row data to receiver", e);
				if (this.abortOnError) throw e;
			}
		}
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}

	public String getMessages()
	{
		return "";
	}

	public boolean getUseVerboseFormat()
	{
		return verboseFormat;
	}

	public void setUseVerboseFormat(boolean flag)
	{
		this.verboseFormat = flag;
		if (this.verboseFormat)
		{
			rowTag = XmlRowDataConverter.LONG_ROW_TAG;
			columnTag = XmlRowDataConverter.LONG_COLUMN_TAG;
		}
		else
		{
			rowTag = XmlRowDataConverter.SHORT_ROW_TAG;
			columnTag = XmlRowDataConverter.SHORT_COLUMN_TAG;
		}
	}
	
}
