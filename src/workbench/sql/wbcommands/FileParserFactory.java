/*
 * FileParserFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.BlobMode;
import workbench.db.importer.DataImporter;
import workbench.db.importer.TextFileParser;
import workbench.db.postgres.PgCopyImporter;

import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FileParserFactory
{
	private ArgumentParser cmdLine;
	private File inputFile;
	private String table;
	private WbConnection currentConnection;
	private String schema;
	private DataImporter imp;

	public FileParserFactory(ArgumentParser parameters, File sourcefile, WbConnection conn, DataImporter importer)
	{
		this.cmdLine = parameters;
		this.inputFile = sourcefile;
		table = cmdLine.getValue(WbImport.ARG_TARGETTABLE);
		schema = cmdLine.getValue(CommonArgs.ARG_SCHEMA);
		currentConnection = conn;
	}

	public ImportFileParser createTextFileParser(StatementRunnerResult result)
	{
		TextFileParser textParser = new TextFileParser();
		setupCommonAttributes(textParser);

		boolean multi = cmdLine.getBoolean(WbImport.ARG_MULTI_LINE, WbImport.getMultiDefault());
		textParser.setEnableMultilineRecords(multi);
		textParser.setTreatClobAsFilenames(cmdLine.getBoolean(WbImport.ARG_CLOB_ISFILENAME, false));
		textParser.setNullString(cmdLine.getValue(WbExport.ARG_NULL_STRING, null));
		textParser.setAlwaysQuoted(cmdLine.getBoolean(WbExport.ARG_QUOTE_ALWAYS, false));
		textParser.setIllegalDateIsNull(cmdLine.getBoolean(WbImport.ARG_ILLEGAL_DATE_NULL, false));
		String delimiter = StringUtil.trimQuotes(cmdLine.getValue(CommonArgs.ARG_DELIM));
		if (cmdLine.isArgPresent(CommonArgs.ARG_DELIM) && StringUtil.isBlank(delimiter))
		{
			result.addMessageByKey("ErrImpDelimEmpty");
			result.setFailure();
			return null;
		}
		if (delimiter != null)
		{
			textParser.setTextDelimiter(delimiter);
		}

		String quote = cmdLine.getValue(WbImport.ARG_QUOTE);
		if (quote != null)
		{
			textParser.setTextQuoteChar(quote);
		}

		textParser.setDecode(cmdLine.getBoolean(WbImport.ARG_DECODE, false));

		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING);

		if (encoding != null)
		{
			textParser.setEncoding(encoding);
		}

		textParser.setEmptyStringIsNull(cmdLine.getBoolean(WbImport.ARG_EMPTY_STRING_IS_NULL, true));

		boolean headerDefault = Settings.getInstance().getBoolProperty("workbench.import.default.header", true);
		boolean header = cmdLine.getBoolean(WbImport.ARG_CONTAINSHEADER, headerDefault);

		String filecolumns = cmdLine.getValue(WbImport.ARG_FILECOLUMNS);

		// The flag for a header lines must be specified before setting the columns
		textParser.setContainsHeader(header);

		String importcolumns = cmdLine.getValue(WbImport.ARG_IMPORTCOLUMNS);
		List<ColumnIdentifier> toImport = null;
		if (StringUtil.isNonBlank(importcolumns))
		{
			toImport = stringToCols(importcolumns);
		}

		if (StringUtil.isNonBlank(table))
		{
			try
			{
				textParser.checkTargetTable();
			}
			catch (Exception e)
			{
				result.addMessage(textParser.getMessages());
				result.setFailure();
				return null;
			}

			// read column definition from header line
			// if no header was specified, the text parser
			// will assume the columns in the text file
			// map to the column in the target table
			try
			{
				if (StringUtil.isBlank(filecolumns))
				{
					textParser.setupFileColumns(toImport);
				}
				else
				{
					List<ColumnIdentifier> fileCols = stringToCols(filecolumns);
					textParser.setColumns(fileCols, toImport);
				}
			}
			catch (Exception e)
			{
				result.setFailure();
				result.addMessage(textParser.getMessages());
				LogMgr.logError("FileParserFactory.createTextFileParser()", ExceptionUtil.getDisplay(e), null);
				return null;
			}

		}

		// The column filter has to bee applied after the
		// columns are defined!
		String colFilter = cmdLine.getValue(WbImport.ARG_COL_FILTER);
		if (colFilter != null)
		{
			addColumnFilter(colFilter, textParser);
		}

		String btype = cmdLine.getValue(WbExport.ARG_BLOB_TYPE);
		BlobMode mode = BlobMode.getMode(btype);
		if (btype != null && mode != null)
		{
			textParser.setBlobMode(mode);
		}
		else if (cmdLine.isArgPresent(WbImport.ARG_BLOB_ISFILENAME))
		{
			boolean flag = cmdLine.getBoolean(WbImport.ARG_BLOB_ISFILENAME, true);
			if (flag)
			{
				textParser.setBlobMode(BlobMode.SaveToFile);
			}
			else
			{
				textParser.setBlobMode(BlobMode.None);
			}
		}

		String filter = cmdLine.getValue(WbImport.ARG_LINE_FILTER);
		if (filter != null)
		{
			textParser.setLineFilter(StringUtil.trimQuotes(filter));
		}
		textParser.setQuoteEscaping(CommonArgs.getQuoteEscaping(cmdLine));

		// when all columns are defined we can check for a fixed-width import
		String width = cmdLine.getValue(WbImport.ARG_COL_WIDTHS);
		if (!StringUtil.isEmptyString(width))
		{
			try
			{
				ColumnWidthDefinition def = new ColumnWidthDefinition(width);
				textParser.setColumnWidths(def.getColumnWidths());
			}
			catch (MissingWidthDefinition e)
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrImpWrongWidth", e.getColumnName()));
				result.setFailure();
				return null;
			}
		}

		if (cmdLine.isArgPresent(WbImport.ARG_PG_COPY) && currentConnection.getMetadata().isPostgres())
		{
			if (!imp.isModeInsert())
			{
				result.addMessage("COPY only possible with -mode=insert");
				result.setFailure();
				return null;
			}
			PgCopyImporter pg = new PgCopyImporter(currentConnection);
			if (pg.isSupported())
			{
				textParser.setStreamImporter(pg);
			}
			else
			{
				result.addMessage("PostgreSQL copy API not supported!");
				result.setWarning(true);
			}
		}
		return textParser;
	}

	private List<ColumnIdentifier> stringToCols(String columns)
	{
		List<String> names = StringUtil.stringToList(columns, ",", true, true);
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(names.size());
		for (String name : names)
		{
			cols.add(new ColumnIdentifier(name));
		}
		return cols;
	}

	private void addColumnFilter(String filters, ImportFileParser textParser)
	{
		List<String> filterList = StringUtil.stringToList(filters, ",", false);

		if (filterList.size() < 1) return;

		for (String filterDef : filterList)
		{
			List<String> l = StringUtil.stringToList(filterDef, "=", true);
			if (l.size() != 2) continue;

			String col = l.get(0);
			String regex = l.get(1);
			textParser.addColumnFilter(col, StringUtil.trimQuotes(regex));
		}
	}

	private void setupCommonAttributes(ImportFileParser parser)
	{
		parser.setConnection(currentConnection);
		parser.setTableName(table);
		parser.setTargetSchema(schema);
		if (inputFile != null)
		{
			parser.setInputFile(inputFile);
		}
	}

}
