/*
 * XmlDataFileParser.java
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import workbench.db.ColumnIdentifier;
import workbench.log.LogMgr;

/**
 *
 * @author  info@sql-workbench.net
 */
public class XmlDataFileParser
	extends DefaultHandler
	implements RowDataProducer
{
	private String inputFile;
	private String tableName = null;

	private int currentRowNumber = 1;
	private int colCount;

	private ColumnIdentifier[] columns;
	private String[] colClasses;
	private String[] colFormats;
	//private int[] colTypes;
	private String encoding = "UTF-8";
	
	private Object[] currentRow;
	private RowDataReceiver receiver;
	private boolean ignoreCurrentRow = false;
	
	/** Define the tags for which the characters surrounded by the 
	 *  tag should be collected
	 */
	private static final HashSet DATA_TAGS = new HashSet();
	static
	{
		DATA_TAGS.add("java-class");
		DATA_TAGS.add("java-sql-type");
		DATA_TAGS.add("data-format");
		DATA_TAGS.add("table-name");
		DATA_TAGS.add("column-name");
		DATA_TAGS.add("column-data");
		DATA_TAGS.add("column-count");
	}
	
	private int currentColIndex = 0;
	private long columnLongValue = 0;
	private boolean hasLongValue = false;
	private boolean isNull = false;
	private StringBuffer chars;
	private boolean keepRunning;

	private HashMap constructors = new HashMap();
	
	public XmlDataFileParser(String inputFile)
	{
		this.inputFile = inputFile;
	}

	public void setTableName(String aName)
	{
		this.tableName = aName;
	}
	
	public void start()
		throws Exception
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
			throw e;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}
	
	public void cancel()
	{
		this.keepRunning = false;
	}

	private void clearRowData()
	{
		if (this.currentRow == null)
		{
			this.currentRow = new Object[this.colCount];
		}
		for (int i=0; i < this.colCount; i++)
		{
			this.currentRow[i] = null;
		}
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
		
		if (qName.equals("row-data"))
		{
			this.clearRowData();
		}
		else if (qName.equals("column-def"))
		{
			try
			{
				String attrValue = attrs.getValue("index");
				this.currentColIndex = Integer.parseInt(attrValue);
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn index definition", e);
				throw new SAXException("Could not read columnn index");
			}
		}
		else if (qName.equals("column-data"))
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
			try
			{
				attrValue = attrs.getValue("index");
				this.currentColIndex = Integer.parseInt(attrValue);
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn index!", e);
				throw new SAXException("Could not read columnn index");
			}
			
			attrValue = attrs.getValue("null");
			this.isNull = "true".equals(attrValue);
		}
	}
	
	public void endElement(String namespaceURI, String sName, String qName)
		throws SAXException
	{
		if (!this.keepRunning) throw new ParsingInterruptedException();
		if (qName.equals("row-data"))
		{
			if (!this.ignoreCurrentRow)
			{
				this.sendRowData();
			}
			this.ignoreCurrentRow = false;
			this.clearRowData();
			this.currentRowNumber ++;
		}
		else if (qName.equals("wb-export"))
		{
			this.receiver.importFinished();
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
				this.sendTableDefinition();
			}
			catch (SQLException sql)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Error when setting target table", sql);
				throw new SAXException("Could not initialize target table");
			}
		}
		else if (qName.equals("column-data"))
		{
			this.buildColumnData();
		}
		else if (qName.equals("column-count"))
		{
			try
			{
				this.colCount = Integer.parseInt(this.chars.toString());
				this.columns = new ColumnIdentifier[this.colCount];
				this.colClasses = new String[this.colCount];
				this.colFormats = new String[this.colCount];
			}
			catch (Exception e)
			{
				throw new SAXException("Invalid column-count (" + this.chars + ")");
			}
		}
		else if (qName.equals("column-name"))
		{
			try
			{
				this.columns[this.currentColIndex] = new ColumnIdentifier(this.chars.toString());
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
				//this.colTypes[this.currentColIndex] = Integer.parseInt(this.chars.toString());
				this.columns[this.currentColIndex].setDataType(Integer.parseInt(this.chars.toString()));
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn type!", e);
				throw new SAXException("Could not read columnn type");
			}
		}
		else if (qName.equals("java-class"))
		{
			try
			{
				this.colClasses[this.currentColIndex] = this.chars.toString();
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn class name!", e);
				throw new SAXException("Could not read columnn name");
			}
		}
		
		// Stop collecting characters...
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
		this.currentRow[this.currentColIndex] = null;

		// the isNull flag will be set by the startElement method
		// as that is an attribute of the tag
		if (this.isNull)
		{
			return;
		}

		String value = this.chars.toString();
		
		switch (this.columns[this.currentColIndex].getDataType())
		{
			case Types.CHAR:
			case Types.VARCHAR:
				this.currentRow[this.currentColIndex] = value;
				break;
				
			case Types.TIME:
				if (hasLongValue)
				{
					this.currentRow[this.currentColIndex] = new java.sql.Time(this.columnLongValue);
				}
				break;
				
			case Types.BIGINT:
				try
				{
					if (value.trim().length() > 0)
					{
						this.currentRow[this.currentColIndex] = new BigInteger(value);
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not create BigInt value!", e);
					this.currentRow[this.currentColIndex] = null;
				}
				break;
				
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.TINYINT:
				try
				{
					if (value.trim().length() > 0)
					{
						this.currentRow[this.currentColIndex] = new Integer(value);
					}
				}
				catch (Exception e)
				{
					LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not create BigInt value!", e);
					this.currentRow[this.currentColIndex] = null;
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
						this.currentRow[this.currentColIndex] = null;
						d = null;
					}
				}
				
				if (d != null)
				{
					if (this.columns[this.currentColIndex].getDataType() == Types.TIMESTAMP)
					{
						this.currentRow[this.currentColIndex] = new java.sql.Timestamp(d.getTime());
					}
					else
					{
						this.currentRow[this.currentColIndex] = d;
					}
				}
				break;
				
			case Types.DECIMAL:
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.REAL:
				Object result = this.createNumericType(this.colClasses[this.currentColIndex], value);
				this.currentRow[this.currentColIndex] = result;
				break;
		}
		
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
		this.receiver.setTargetTable(this.tableName, this.columns);
	}
	
	private void sendRowData()
		throws SAXException
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
			}
		}
		if (!this.keepRunning) throw new ParsingInterruptedException();
	}
	
	public String getMessages()
	{
		return "";
	}
	
}
