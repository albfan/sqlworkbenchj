/*
 * FileImporter.java
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

import java.io.File;
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
 * @author  support@sql-workbench.net
 */
public class FileImporter
{
	private int importType = -1;
	private TextImportOptions textOptions;
	private XmlImportOptions xmlOptions;
	private ImportOptions generalOptions;
	private File inputFile;
	private TableIdentifier table;
	private RowDataProducer producer;
	private WbConnection connection;
	
	public FileImporter(File inputFile)
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
		if (type == ProducerFactory.IMPORT_TEXT)
			this.setImportTypeText();
		else if (type == ProducerFactory.IMPORT_XML)
			this.setImportTypeXml();
		else
			throw new IllegalArgumentException("Not a valid import type!");
	}

	public boolean isTextImport()
	{
		return this.importType == ProducerFactory.IMPORT_TEXT;
	}
	public boolean isXmlImport()
	{
		return this.importType == ProducerFactory.IMPORT_XML;
	}
	
	public void setImportTypeText()
	{
		this.importType = ProducerFactory.IMPORT_TEXT;
		this.producer = null;
	}
	
	public void setImportTypeXml()
	{
		this.importType = ProducerFactory.IMPORT_XML;
		this.producer = null;
	}
	
	private void setInputFile(File inputFilename)
	{
		this.inputFile = inputFilename;
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
			ProducerFactory factory = new ProducerFactory(inputFile);
			factory.setType(this.importType);
			factory.setXmlOptions(this.xmlOptions);
			factory.setGeneralOptions(this.generalOptions);
			factory.setTextOptions(this.textOptions);
			factory.setConnection(this.connection);
			factory.setImportTypeText();
			if (this.table != null)
			{
				factory.setTargetTable(this.table);
			}
			this.producer = factory.getProducer();
		}
		return this.producer;
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
	
	public String getSourceFilename()
	{
		if (this.inputFile == null) return null;
		return this.inputFile.getAbsolutePath();
	}
	
	public String getWbCommand()
	{
		StringBuffer result = new StringBuffer(150);
		StringBuffer indent = new StringBuffer();
		indent.append('\n');
		for (int i=0; i < WbImport.VERB.length(); i++) indent.append(' ');
		indent.append(' ');
		
		result.append(WbImport.VERB + " -" + WbImport.ARG_FILE + "=");
		String filename = inputFile.getAbsolutePath();
		if (filename.indexOf('-') > -1) result.append('"');
		result.append(StringUtil.replace(filename, "\\", "/"));
		if (filename.indexOf('-') > -1) result.append('"');
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
