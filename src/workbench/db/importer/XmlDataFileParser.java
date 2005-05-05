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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
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
import workbench.exception.ExceptionUtil;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class XmlDataFileParser
	extends DefaultHandler
	implements RowDataProducer, ImportFileParser
{
	private String sourceDirectory;
	private String inputFile;
	private String tableName;
	private String tableNameFromFile;
	
	private int currentRowNumber = 1;
	private int colCount;
	private int realColCount;

	private List columnsToImport;
	private ColumnIdentifier[] columns;
	private String encoding = "UTF-8";

	private Object[] currentRow;
	private RowDataReceiver receiver;
	private boolean ignoreCurrentRow = false;
	private boolean abortOnError = false;
	private boolean verboseFormat = false;
	private String missingColumn;
	
	/** Define the tags for which the characters surrounded by the
	 *  tag should be collected
	 */
	private static final HashSet DATA_TAGS = new HashSet();
	static
	{
		DATA_TAGS.add(XmlRowDataConverter.LONG_COLUMN_TAG);
		DATA_TAGS.add(XmlRowDataConverter.SHORT_COLUMN_TAG);
	}

	private int currentColIndex = 0;
	private int realColIndex = 0;
	private boolean ignoreCurrentColumn = false;
	private long columnLongValue = 0;
	private boolean hasLongValue = false;
	private boolean isNull = false;
	private StrBuffer chars;
	private boolean keepRunning;
	private String rowTag = XmlRowDataConverter.LONG_ROW_TAG;
	private String columnTag = XmlRowDataConverter.LONG_COLUMN_TAG;
	
	private HashMap constructors = new HashMap();
	private SAXParser saxParser;
	
	public XmlDataFileParser()
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		try
		{
			saxParser = factory.newSAXParser();
		}
		catch (Exception e)
		{
			// should not happen!
			LogMgr.logError("XmlDataFileParser.<init>", "Error creating XML parser", e);
		}
	}
	public XmlDataFileParser(String inputFile)
	{
		this();
		this.inputFile = inputFile;
	}

	public String getColumns()
	{
		return StringUtil.listToString(this.columnsToImport, ',');
	}
	
	public void setColumns(String columnList)
		throws SQLException
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
		checkImportColumns();
	}
	
	/**	 Define the columns to be imported
	 */
	public void setColumns(List cols)
		throws SQLException
	{
		if (cols != null && cols.size() > 0)
		{
			this.columnsToImport = new ArrayList();
			Iterator itr = cols.iterator();
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
		checkImportColumns();
	}

	private void checkImportColumns()
		throws SQLException
	{
		if (this.columnsToImport == null) 
		{
			this.realColCount = this.colCount;
			return;
		}
		
		this.missingColumn = null;
		try
		{
			if (this.columns == null) this.readTableDefinition();
		}
		catch (Throwable e)
		{
		}
		for (int i=0; i < this.columnsToImport.size(); i++)
		{
			ColumnIdentifier c = (ColumnIdentifier)this.columnsToImport.get(i);
			if (!this.containsColumn(c)) 
			{
				this.missingColumn = c.getColumnName();
				throw new SQLException("Import column " + c.getColumnName() + " not present in input file!");
			}
		}
		this.realColCount = this.columnsToImport.size();
	}
	
	/**
	 *	Returns the first column from the import columns
	 *  that is not found in the import file
	 *	@see #setColumns(String)
	 *  @see #setColumns(List)
	 */
	public String getMissingColumn() { return this.missingColumn; }
	
	private boolean containsColumn(ColumnIdentifier col)
	{
		if (this.columns == null) return false;
		for (int i=0; i<this.columns.length; i++)
		{
			if (this.columns[i].equals(col)) return true;
		}
		return false;
	}
	
	public void setTableName(String aName)
	{
		this.tableName = aName;
	}

	public List getColumnsFromFile()
	{
		try
		{
			if (this.columns == null) this.readTableDefinition();
		}
		catch (FileNotFoundException e)
		{
			return Collections.EMPTY_LIST;
		}
		ArrayList result = new ArrayList(this.columns.length);
		for (int i=0; i < this.columns.length; i++)
		{
			result.add(this.columns[i]);
		}
		return result;
	}

	private void readTableDefinition()
		throws FileNotFoundException
	{
		XmlTableDefinitionParser tableDef = new XmlTableDefinitionParser(inputFile, this.encoding);
		this.columns = tableDef.getColumns();
		this.colCount = this.columns.length;
		this.tableNameFromFile = tableDef.getTableName();
		String format = tableDef.getTagFormat();
		if (format != null)
		{
			if (XmlRowDataConverter.KEY_FORMAT_LONG.equals(format))
			{
				this.setUseVerboseFormat(true);
			}
			else if (XmlRowDataConverter.KEY_FORMAT_SHORT.equals(format))
			{
				this.setUseVerboseFormat(false);
			}
		}
	}

	public String getSourceFilename()
	{
		return this.inputFile;
	}
	
	public void setSourceFile(String file)
	{
		this.sourceDirectory = null;
		this.inputFile = file;
	}
	
	public void setSourceDirectory(String dir)
	{
		File f = new File(dir);
		if (!f.isDirectory()) throw new IllegalArgumentException(dir + " is not a directory");
		this.sourceDirectory = dir;
		this.inputFile = null;
	}
	
	public String getSourceDirectory()
	{
		return this.sourceDirectory;
	}
	
	public void setAbortOnError(boolean flag)
	{
		this.abortOnError = flag;
	}
	
	private void processOneFile()
		throws Exception
	{
		if (this.columns == null) this.readTableDefinition();
		if (this.columnsToImport == null)
		{
			this.realColCount = this.colCount;
		}
		else
		{
			this.realColCount = this.columnsToImport.size();
		}
		this.sendTableDefinition();
		InputStream in = null;
		try
		{
			in = new FileInputStream(this.inputFile);
			InputSource source = new InputSource(new BufferedReader(new InputStreamReader(in, this.encoding), 512*1024));
			this.keepRunning = true;
			saxParser.parse(source, this);
		}
		catch (ParsingInterruptedException e)
		{

			this.receiver.importCancelled();
			throw e;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}

	private void reset()
	{
		tableName = null;
		tableNameFromFile = null;
		ignoreCurrentRow = false;
		currentColIndex = 0;
		realColIndex = 0;
		ignoreCurrentColumn = false;
		columnLongValue = 0;
		hasLongValue = false;
		isNull = false;
		chars = null;
		columns = null;
		columnsToImport = null;
	}
	
	private void processDirectory()
		throws Exception
	{
		File dir = new File(this.sourceDirectory);
		File[] files = dir.listFiles();
		int count = files.length;
		boolean verbose = this.verboseFormat;
		for (int i=0; i < count; i++)
		{
			if (files[i].getName().endsWith(".xml"))
			{
				try
				{
					this.inputFile = files[i].getAbsolutePath();
					this.reset();

					// readTableDefinition() might reset the verbose 
					// flag if a new XML structure is used
					// this ensures, that the flag specified by the 
					// user will be used for files that do not have the 
					// flag in the meta-data tag
					this.verboseFormat = verbose;
					this.processOneFile();
				}
				catch (SQLException e)
				{
					LogMgr.logWarning("XmlDataFileParser.processDirectory()", "Error importing " + this.inputFile + ": " + ExceptionUtil.getDisplay(e), null);
					if (this.abortOnError) return;
				}
			}
		}
	}
	
	public void start()
		throws Exception
	{
		try
		{
			if (this.sourceDirectory == null)
			{
				processOneFile();
			}
			else 
			{
				processDirectory();
			}
			this.receiver.importFinished();
		}
		catch (Exception e)
		{
			//LogMgr.logError("XmlDataFileParser.start()", "Error during parsing", e);
			this.receiver.importCancelled();
			throw e;
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
		
		if (qName.equals(this.rowTag))
		{
			// row definition ended, start a new row
			this.clearRowData();
			this.chars = null;
		}
		else if (qName.equals(this.columnTag))
		{
			this.chars = new StrBuffer();
			hasLongValue = false;
			String attrValue = attrs.getValue(XmlRowDataConverter.ATTR_LONGVALUE);
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
			attrValue = attrs.getValue(XmlRowDataConverter.ATTR_NULL);
			this.isNull = "true".equals(attrValue);
		}
		else
		{
			this.chars = null;
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
			this.currentRowNumber ++;
		}
		else if (qName.equals(this.columnTag))
		{
			this.buildColumnData();
			this.currentColIndex ++;
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
			this.realColIndex ++;
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
				java.sql.Date d = new java.sql.Date(this.columnLongValue);
				if (type == Types.TIMESTAMP)
				{
					this.currentRow[this.realColIndex] = new java.sql.Timestamp(d.getTime());
				}
				else
				{
					this.currentRow[this.realColIndex] = d;
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
		try
		{
			if (this.columnsToImport == null)
			{
				this.receiver.setTargetTable(this.tableName == null ? this.tableNameFromFile : this.tableName, this.columns);
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
				this.receiver.setTargetTable(this.tableName == null ? this.tableNameFromFile : this.tableName, cols);
			}
			this.currentRow = new Object[this.realColCount];
		}
		catch (SQLException e)
		{
			this.currentRow = null;
			throw e;
		}
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
