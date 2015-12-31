/*
 * ProducerFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.util.List;

import workbench.interfaces.ImportFileParser;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.wbcommands.CommonArgs;
import workbench.sql.wbcommands.WbImport;

import workbench.util.StringUtil;
import workbench.util.ValueConverter;

/**
 *	A factory for RowDataProducer to import text or XML files.
 *
 * @author  Thomas Kellerer
 */
public class ProducerFactory
{
	public enum ImportType {
		Text,
		XML,
		Spreadsheet;

		public static ImportType valueOf(int type)
		{
			if (type == 0) return Text;
			if (type == 1) return XML;
			if (type == 2) return Spreadsheet;
			return null;
		}

		public int toInteger()
		{
			if (this == Text) return 0;
			if (this == XML) return 1;
			if (this == Spreadsheet) return 2;
			return -1;
		}
	};

	private ImportType importType = null;
	private TextImportOptions textOptions;
	private ImportOptions generalOptions;
	private File inputFile;
	private List<ColumnIdentifier> inputColumns;
	private TableIdentifier table;
	private RowDataProducer producer;
	private ImportFileParser fileParser;
	private WbConnection connection;

	public ProducerFactory(File file)
	{
		this.setInputFile(file);
	}

	public void setConnection(WbConnection conn)
	{
		if (this.connection != conn)
		{
			this.producer = null;
		}
		this.connection = conn;
	}

	public ImportOptions getGeneralOptions()
	{
		return generalOptions;
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

	public void setImporterOptions(DataImporter importer)
	{
		importer.setMode(generalOptions.getMode());
	}

	public void setType(ImportType type)
	{
		if (type == ImportType.Text)
		{
			this.setImportTypeText();
		}
		else if (type == ImportType.XML)
		{
			this.setImportTypeXml();
		}
	}

	public boolean isTextImport()
	{
		return this.importType == ImportType.Text;
	}

	public boolean isXmlImport()
	{
		return this.importType == ImportType.XML;
	}

	public boolean isSpreadsheetImport()
	{
		return this.importType == ImportType.Spreadsheet;
	}

	public void setImportTypeSpreadsheet()
	{
		this.importType = ImportType.Spreadsheet;
		this.producer = null;
	}

	public void setImportTypeText()
	{
		this.importType = ImportType.Text;
		this.producer = null;
	}

	public void setImportTypeXml()
	{
		this.importType = ImportType.XML;
		this.producer = null;
	}

	private void setInputFile(File inputFilename)
	{
		this.inputFile = inputFilename;
		this.inputColumns = null;
		this.producer = null;
		this.fileParser = null;
	}

	public void setTargetTable(TableIdentifier tableId)
	{
		this.table = tableId;
		if (this.table == null) return;
		if (this.producer == null) getProducer();
		fileParser.setTableName(tableId.getTableExpression());
	}

	public RowDataProducer getProducer()
	{
		if (this.producer == null)
		{
			switch (this.importType)
			{
				case Text:
					createTextFileParser();
					break;
				case XML:
					createXmlFileParser();
					break;
				case Spreadsheet:
					createSpreadsheetParser();
			}
		}
		return this.producer;
	}

	/**
	 *	Return the list of columns defined in the file
	 */
	public List<ColumnIdentifier> getFileColumns()
	{
		if (this.inputColumns == null)
		{
			getProducer();
			this.inputColumns = fileParser.getColumnsFromFile();
		}
		return this.inputColumns;
	}

	public void setColumnMap(List<ColumnIdentifier> sourceColumns, List<ColumnIdentifier> targetColumns)
		throws Exception
	{
		// A column mapping is only possible for text imports where the source contains a header row
		if (this.importType == ImportType.Text && this.textOptions.getContainsHeader())
		{
			((TextFileParser)producer).setColumnMap(sourceColumns, targetColumns);
		}
		else
		{
			setImportColumns(targetColumns);
		}
	}

	public void setImportColumns(List<ColumnIdentifier> cols)
		throws Exception
	{
		if (this.producer == null) getProducer();
		this.fileParser.setColumns(cols);
	}

	private void createTextFileParser()
	{
		TextFileParser parser = new TextFileParser(inputFile);
		parser.setEncoding(this.generalOptions.getEncoding());
		parser.setContainsHeader(this.textOptions.getContainsHeader());
		parser.setTextQuoteChar(this.textOptions.getTextQuoteChar());
		parser.setDecode(this.textOptions.getDecode());
		parser.setTextDelimiter(this.textOptions.getTextDelimiter());
		parser.setConnection(this.connection);
		parser.setQuoteEscaping(textOptions.getQuoteEscaping());
		parser.setAlwaysQuoted(textOptions.getQuoteAlways());
		ValueConverter converter = new ValueConverter();
		converter.setDefaultDateFormat(this.generalOptions.getDateFormat());
		converter.setDefaultTimestampFormat(this.generalOptions.getTimestampFormat());
		String dec = this.textOptions.getDecimalChar();
		if (dec != null) converter.setDecimalCharacter(dec.charAt(0));
		parser.setValueConverter(converter);

		if (this.table != null)
		{
			parser.setTableName(this.table.getTableExpression());
		}
		this.inputColumns = null;
		this.producer = parser;
		this.fileParser = parser;
	}

	public File getSourceFile()
	{
		return this.inputFile;
	}

	private void createSpreadsheetParser()
	{
		this.inputColumns = null;
		this.producer = null;
		this.fileParser = null;
	}

	private void createXmlFileParser()
	{
		XmlDataFileParser parser = new XmlDataFileParser(inputFile);
		parser.setEncoding(this.generalOptions.getEncoding());
		if (this.table != null)
		{
			parser.setTableName(this.table.getTableName());
		}
		this.inputColumns = null;
		this.producer = parser;
		this.fileParser = parser;
	}

	/**
	 * Appends text import options to the passed sql command
	 */
	private void appendTextOptions(StringBuilder command, StringBuilder indent)
	{
		if (this.textOptions == null) return;
		appendArgument(command, WbImport.ARG_CONTAINSHEADER, textOptions.getContainsHeader(), indent);
		appendArgument(command, WbImport.ARG_DECODE, textOptions.getDecode(), indent);
		String delim = textOptions.getTextDelimiter();
		if ("\t".equals(delim)) delim = "\\t";

		CommonArgs.appendArgument(command, CommonArgs.ARG_DATE_FORMAT, generalOptions.getDateFormat(), indent);
		CommonArgs.appendArgument(command, CommonArgs.ARG_TIMESTAMP_FORMAT, generalOptions.getTimestampFormat(), indent);
		CommonArgs.appendArgument(command, CommonArgs.ARG_DELIM, "'" + delim + "'", indent);
		CommonArgs.appendArgument(command, WbImport.ARG_QUOTE, textOptions.getTextQuoteChar(), indent);
		CommonArgs.appendArgument(command, CommonArgs.ARG_DECIMAL_CHAR, textOptions.getDecimalChar(), indent);
		CommonArgs.appendArgument(command, WbImport.ARG_FILECOLUMNS, this.fileParser.getColumns(), indent);
		CommonArgs.appendArgument(command, CommonArgs.ARG_QUOTE_ESCAPE, textOptions.getQuoteEscaping().toString(), indent);
	}

	private void appendArgument(StringBuilder result, String arg, boolean value, StringBuilder indent)
	{
		CommonArgs.appendArgument(result, arg, Boolean.toString(value), indent);
	}

	/**
	 *	Generates a WB SQL command from the current import
	 *  settings
	 */
	public StringBuilder getWbCommand()
	{
		StringBuilder result = new StringBuilder(150);
		StringBuilder indent = new StringBuilder();
		indent.append('\n');
		for (int i = 0; i < WbImport.VERB.length(); i++)
		{
			indent.append(' ');
		}
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

		CommonArgs.appendArgument(result, WbImport.ARG_TARGETTABLE, this.table.getTableName(), indent);
		CommonArgs.appendArgument(result, CommonArgs.ARG_ENCODING, this.generalOptions.getEncoding(), indent);
		appendTextOptions(result, indent);

		return result;
	}

}
