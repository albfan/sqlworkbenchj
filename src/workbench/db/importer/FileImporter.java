/*
 * ProducerFactory.java
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

import java.util.Collections;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.gui.dialogs.dataimport.XmlImportOptions;
import workbench.sql.wbcommands.WbImport;
import workbench.util.StringUtil;

/**
 *	A factory for RowDataProducer to import text or XML files. 
 * @author  info@sql-workbench.net
 */
public class FileImporter
{
	public static final int IMPORT_TEXT = 0;
	public static final int IMPORT_XML = 1;
	private int importType = -1;
	private TextImportOptions textOptions;
	private XmlImportOptions xmlOptions;
	private ImportOptions generalOptions;
	private String inputFile;
	private List inputColumns;
	private TableIdentifier table;
	private RowDataProducer producer;
	private WbConnection connection;
	
	public FileImporter(String inputFile)
	{
		this.setInputFile(inputFile);
	}
	
	public void setConnection(WbConnection conn)
	{
		this.connection = conn;
		this.producer = null;
	}
	
	public void setGeneralOptions(ImportOptions options)
	{
		this.generalOptions = options;
	}
	
	public void setTextOptions(TextImportOptions options)
	{
		this.textOptions = options;
	}

	public TextImportOptions getTextOptions()
	{
		return this.textOptions;
	}
	
	public XmlImportOptions getXmlOptions()
	{
		return this.xmlOptions;
	}
	
	public void setXmlOptions(XmlImportOptions options)
	{
		this.xmlOptions = options;
	}
	
	public void setImporterOptions(DataImporter importer)
	{
		importer.setMode(generalOptions.getMode());
	}
	
	public void setType(int type)
	{
		if (type == IMPORT_TEXT)
			this.setImportTypeText();
		else if (type == IMPORT_XML)
			this.setImportTypeXml();
		else
			throw new IllegalArgumentException("Not a valid import type!");
	}

	public boolean isTextImport()
	{
		return this.importType == IMPORT_TEXT;
	}
	public boolean isXmlImport()
	{
		return this.importType == IMPORT_XML;
	}
	
	public void setImportTypeText()
	{
		this.importType = IMPORT_TEXT;
		this.producer = null;
	}
	
	public void setImportTypeXml()
	{
		this.importType = IMPORT_XML;
		this.producer = null;
	}
	
	private void setInputFile(String inputFilename)
	{
		this.inputFile = inputFilename;
		this.inputColumns = null;
		this.producer = null;
	}
	
	public void setTargetTable(TableIdentifier tableId)
	{
		this.table = tableId;
		if (this.table == null) return;
		if (this.producer == null) getProducer();
		if (this.producer instanceof TextFileParser)
		{
			TextFileParser parser = (TextFileParser)this.producer;
			parser.setTableName(tableId.getTableExpression());
		}
	}
	
	public RowDataProducer getProducer()
	{
		if (this.producer == null)
		{
			if (this.importType == IMPORT_TEXT)
				this.producer = createTextFileParser();
			else if (this.importType == IMPORT_XML)
				this.producer = createXmlFileParser();
		}
		return this.producer;
	}

	/**
	 *	Return the list of columns defined in the file
	 */
	public List getFileColumns()
	{
		if (this.inputColumns == null)
		{
			getProducer();
		}
		return this.inputColumns;
	}

	public void setImportColumns(List cols)
		throws Exception
	{
		if (this.producer == null) getProducer();
		if (this.producer instanceof TextFileParser)
		{
			TextFileParser parser = (TextFileParser)this.producer;
			parser.setColumns(cols);
		}
	}
	
	private RowDataProducer createTextFileParser()
	{
		TextFileParser parser = new TextFileParser(inputFile);
		parser.setContainsHeader(this.textOptions.getContainsHeader());
		parser.setDateFormat(this.generalOptions.getDateFormat());
		parser.setTimeStampFormat(this.generalOptions.getTimestampFormat());
		parser.setQuoteChar(this.textOptions.getTextQuoteChar());
		parser.setDecimalChar(this.textOptions.getDecimalChar());
		parser.setDecodeUnicode(this.textOptions.getDecode());
		parser.setDelimiter(this.textOptions.getTextDelimiter());
		parser.setConnection(this.connection);
		if (this.table != null)
		{
			parser.setTableName(this.table.getTableExpression());
		}
		this.inputColumns = parser.getColumnsFromFile();
		return parser;
	}
	
	public String getSourceFilename()
	{
		return this.inputFile;
	}
	
	private RowDataProducer createXmlFileParser()
	{
		XmlDataFileParser parser = new XmlDataFileParser(inputFile);
		parser.setUseVerboseFormat(this.xmlOptions.getUseVerboseXml());
		return parser;
	}
	
	public String getWbCommand()
	{
		StringBuffer result = new StringBuffer(150);
		StringBuffer indent = new StringBuffer();
		indent.append('\n');
		for (int i=0; i < WbImport.VERB.length(); i++) indent.append(' ');
		indent.append(' ');
		
		result.append(WbImport.VERB + " -" + WbImport.ARG_FILE + "=");
		if (inputFile.indexOf('-') > -1) result.append('"');
		result.append(StringUtil.replace(inputFile, "\\", "/"));
		if (inputFile.indexOf('-') > -1) result.append('"');
		result.append(indent);
		result.append('-');
		result.append(WbImport.ARG_TYPE);
		result.append('=');
		if (this.isXmlImport())
		{
			result.append("xml");
		}
		else 
		{
			result.append("text");
		}
		
		return result.toString();
	}
	
}
