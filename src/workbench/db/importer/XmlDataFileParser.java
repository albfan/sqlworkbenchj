/*
 * XmlDataFileParser.java
 *
 * Created on October 15, 2003, 11:59 PM
 */

package workbench.db.importer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import java.util.List;
import workbench.log.LogMgr;

/**
 *
 * @author  thomas
 */
public class XmlDataFileParser
extends DefaultHandler
{
	private String inputFile;
	private String tableName;

	private int colCount;
	private String[] colNames;
	private String[] colClasses;
	private String[] colFormats;
	private int[] colTypes;

	private Object[] currentRow;
	private RowDataReceiver receiver;

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
	
	public XmlDataFileParser(String inputFile)
	{
		this.inputFile = inputFile;
	}
	
	public void parse()
		throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try
		{
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(this.inputFile, this);
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlDataFileparser.parse()", "Error when parsing XML file", e);
			throw e;
		}
	}

	public void setRowDataReceiver(RowDataReceiver aReceiver)
	{
		this.receiver = aReceiver;
	}
	
	public void startDocument()
		throws SAXException
	{
	}
	
	public void endDocument()
		throws SAXException
	{
	}
	
	public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
		throws SAXException
	{
		this.chars = null;
		if (DATA_TAGS.contains(qName))
		{
			this.chars = new StringBuffer();
		}
		
		if (qName.equals("row-data"))
		{
			this.currentRow = new Object[this.colCount];
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
		if (qName.equals("row-data"))
		{
			this.sendRowData();
			this.currentRow = null;
		}
		else if (qName.equals("wb-export"))
		{
			this.receiver.importFinished();
		}
		else if (qName.equals("table-name"))
		{
			this.tableName = new String(this.chars);
		}
		else if (qName.equals("table-def"))
		{
			this.sendTableDefinition();
		}
		else if (qName.equals("column-data"))
		{
			this.buildColumnData();
		}
		else if (qName.equals("java-sql-type"))
		{
			try
			{
				this.colTypes[this.currentColIndex] = Integer.parseInt(this.chars.toString());
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn type!", e);
				throw new SAXException("Could not read columnn type");
			}
		}
		else if (qName.equals("column-name"))
		{
			try
			{
				this.colNames[this.currentColIndex] = this.chars.toString();
			}
			catch (Exception e)
			{
				LogMgr.logError("XmlDataFileParser.endElement()", "Could not read columnn name!", e);
				throw new SAXException("Could not read columnn name");
			}
		}
		else if (qName.equals("column-count"))
		{
			try
			{
				this.colCount = Integer.parseInt(this.chars.toString());
				this.colTypes = new int[this.colCount];
				this.colClasses = new String[this.colCount];
				this.colNames = new String[this.colCount];
				this.colFormats = new String[this.colCount];
			}
			catch (Exception e)
			{
				throw new SAXException("Invalid column-count (" + this.chars + ")");
			}
			finally
			{
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
		if (chars != null)
		{
			this.chars.append(buf, offset, len);
		}
	}
	
	// treat validation errors as fatal
	public void error(SAXParseException e)
		throws SAXParseException
	{
		LogMgr.logError("XmlDataFileParser.error()", "Received an error", e);
		throw e;
	}
	
	public void fatalError(SAXParseException e)
		throws SAXParseException
	{
		LogMgr.logError("XmlDataFileParser.fatalError()", "Received a fatal error!", e);
		throw e;
	}
	// dump warnings too
	public void warning(SAXParseException err)
		throws SAXParseException
	{
		System.out.println("** Warning, line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println("   " + err.getMessage());
	}
	
	

	private void buildColumnData()
	{
		this.currentRow[this.currentColIndex] = null;
		
		if (this.isNull)
		{
			return;
		}

		String value = this.chars.toString();
		
		switch (this.colTypes[this.currentColIndex])
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
				if (this.hasLongValue)
				{
					this.currentRow[this.currentColIndex] = new java.sql.Date(this.columnLongValue);
				}
				else
				{
					try
					{
						SimpleDateFormat sdf = new SimpleDateFormat(this.colFormats[this.currentColIndex]);
						java.util.Date d = sdf.parse(value);
						this.currentRow[this.currentColIndex] = new java.sql.Date(d.getTime());
					}
					catch (Exception e)
					{
						LogMgr.logError("XmlDataFileParser.buildColumnData()", "Could not convert data value " + value + " using format " + this.colFormats[this.currentColIndex], e);
						this.currentRow[this.currentColIndex] = null;
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
		try
		{
			Class cls = Class.forName(aClass);
			Constructor con = cls.getConstructor(new Class[] { String.class });
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
	{
		this.receiver.setTargetTable(this.tableName, this.colNames);
	}
	
	private void sendRowData()
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
	}
	
}
