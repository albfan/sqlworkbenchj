/*
 * WbImport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;

import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.db.importer.ParsingInterruptedException;
import workbench.db.importer.TextFileParser;
import workbench.db.importer.XmlDataFileParser;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbImport 
	extends SqlCommand
{
	public static final String VERB = "WBIMPORT";
	private DataImporter imp;
	public static final String ARG_TYPE = "type";
	public static final String ARG_FILE = "file";
	public static final String ARG_TARGETTABLE = "table";
	public static final String ARG_DELIM = "delimiter";
	public static final String ARG_QUOTE = "quotechar";
	public static final String ARG_DATE_FORMAT = "dateformat";
	public static final String ARG_TIMESTAMP_FORMAT = "timestampformat";
	public static final String ARG_DECCHAR = "decimal";
	public static final String ARG_COMMIT = "commitevery";
	public static final String ARG_COMMIT_BATCH = "commitbatch";
	public static final String ARG_CONTAINSHEADER = "header";
	public static final String ARG_ENCODING = "encoding";
	public static final String ARG_FILECOLUMNS = "filecolumns";
	public static final String ARG_MODE = "mode";
	public static final String ARG_KEYCOLUMNS = "keycolumns";
	public static final String ARG_BATCHSIZE = "batchsize";
	public static final String ARG_DELETE_TARGET = "deletetarget";
	public static final String ARG_EMPTY_STRING_IS_NULL = "emptystringnull";
	public static final String ARG_CONTINUE = "continueonerror";
	public static final String ARG_DECODE = "decode";
	public static final String ARG_VERBOSEXML = "verbosexml";
	public static final String ARG_IMPORTCOLUMNS = "importcolumns";
	public static final String ARG_COL_FILTER = "columnfilter";
	public static final String ARG_LINE_FILTER = "linefilter";
	public static final String ARG_PROGRESS = "showprogress";
	public static final String ARG_DIRECTORY = "sourcedir";
	public static final String ARG_TARGET_SCHEMA = "schema";
	public static final String ARG_USE_TRUNCATE = "usetruncate";
	public static final String ARG_TRIM_VALUES = "trimvalues";
	public static final String ARG_FILE_EXT = "extension";
	public static final String ARG_UPDATE_WHERE = "updatewhere";
	public static final String ARG_TRUNCATE_TABLE = "truncatetable";
	public static final String ARG_CREATE_TABLE = "createtarget";
	public static final String ARG_BLOB_ISFILENAME = "blobisfilename";
	public static final String ARG_CLOB_ISFILENAME = "clobisfilename";
	public static final String ARG_MULTI_LINE = "multiline";
	public static final String ARG_START_ROW = "startrow";
	public static final String ARG_END_ROW = "endrow";
	
	private ArgumentParser cmdLine;

	public WbImport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_TYPE);
		cmdLine.addArgument(ARG_UPDATE_WHERE);
		cmdLine.addArgument(ARG_FILE);
		cmdLine.addArgument(ARG_TARGETTABLE);
		cmdLine.addArgument(ARG_DELIM);
		cmdLine.addArgument(ARG_QUOTE);
		cmdLine.addArgument(ARG_DATE_FORMAT);
		cmdLine.addArgument(ARG_TIMESTAMP_FORMAT);
		cmdLine.addArgument(ARG_DECCHAR);
		cmdLine.addArgument(ARG_COMMIT);
		cmdLine.addArgument(ARG_CONTAINSHEADER);
		cmdLine.addArgument(ARG_ENCODING);
		cmdLine.addArgument("columns");
		cmdLine.addArgument(ARG_FILECOLUMNS);
		cmdLine.addArgument(ARG_MODE);
		cmdLine.addArgument(ARG_KEYCOLUMNS);
		cmdLine.addArgument(ARG_BATCHSIZE);
		cmdLine.addArgument(ARG_DELETE_TARGET);
		cmdLine.addArgument(ARG_EMPTY_STRING_IS_NULL);
		cmdLine.addArgument(ARG_CONTINUE);
		cmdLine.addArgument(ARG_DECODE);
		cmdLine.addArgument(ARG_VERBOSEXML);
		cmdLine.addArgument(ARG_IMPORTCOLUMNS);
		cmdLine.addArgument(ARG_COL_FILTER);
		cmdLine.addArgument(ARG_LINE_FILTER);
		cmdLine.addArgument(ARG_PROGRESS);
		cmdLine.addArgument(ARG_DIRECTORY);
		cmdLine.addArgument(ARG_TARGET_SCHEMA);
		cmdLine.addArgument(ARG_USE_TRUNCATE);
		cmdLine.addArgument(ARG_TRIM_VALUES);
		cmdLine.addArgument(ARG_FILE_EXT);
		cmdLine.addArgument(ARG_TRUNCATE_TABLE);
		cmdLine.addArgument(ARG_CREATE_TABLE);
		cmdLine.addArgument(ARG_BLOB_ISFILENAME);
		cmdLine.addArgument(ARG_CLOB_ISFILENAME);
		cmdLine.addArgument(ARG_MULTI_LINE);
		cmdLine.addArgument(ARG_START_ROW);
		cmdLine.addArgument(ARG_END_ROW);
		cmdLine.addArgument(ARG_COMMIT_BATCH);
		this.isUpdatingCommand = true;
	}

	public String getVerb() { return VERB; }

	private String getWrongParamsMessage()
	{
		String result = ResourceMgr.getString("ErrImportWrongParameters");
		boolean continueDefault = Settings.getInstance().getBoolProperty("workbench.import.default.continue", false);
		result = StringUtil.replace(result, "%continue_default%", Boolean.toString(continueDefault));
		
		boolean multiDefault = Settings.getInstance().getBoolProperty("workbench.import.default.multilinerecord", false);
		result = StringUtil.replace(result, "%multiline_default%", Boolean.toString(multiDefault));
		
		return result;
	}
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		imp = new DataImporter();
		this.imp.setConnection(aConnection);

		StatementRunnerResult result = new StatementRunnerResult(aSql);
		aSql = stripVerb(SqlUtil.makeCleanSql(aSql,false, false, '\''));
		
		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(getWrongParamsMessage());
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(getWrongParamsMessage());
			result.setFailure();
			return result;
		}
		if (!cmdLine.hasArguments())
		{
			result.addMessage(getWrongParamsMessage());
			result.setFailure();
			return result;
		}

		String type = cmdLine.getValue(ARG_TYPE);
		String filename = cmdLine.getValue(ARG_FILE);
		String dir = cmdLine.getValue(ARG_DIRECTORY);

		if (type == null)
		{
			result.addMessage(ResourceMgr.getString("ErrImportTypeMissing"));
			result.addMessage(getWrongParamsMessage());
			result.setFailure();
			return result;
		}

		if (filename == null && dir == null)
		{
			result.addMessage(ResourceMgr.getString("ErrImportFileMissing"));
			result.addMessage("");
			result.addMessage(getWrongParamsMessage());
			result.setFailure();
			return result;
		}

		filename = evaluateFileArgument(filename);
		File inputFile = (filename != null ? new File(filename) : null);
		
		int commit = cmdLine.getIntValue(ARG_COMMIT,-1);
		imp.setCommitEvery(commit);

		boolean continueDefault = Settings.getInstance().getBoolProperty("workbench.import.default.continue", false);
		imp.setContinueOnError(cmdLine.getBoolean(ARG_CONTINUE, continueDefault));

		String table = cmdLine.getValue(ARG_TARGETTABLE);
		String schema = cmdLine.getValue(ARG_TARGET_SCHEMA);

		if (filename != null)
		{
			File f = new File(filename);
			if (!f.exists())
			{
				String msg = ResourceMgr.getString("ErrImportFileNotFound");
				msg = StringUtil.replace(msg, "%filename%", filename);
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}
		else
		{
			File d = new File(dir);
			if (!d.exists())
			{
				String msg = ResourceMgr.getString("ErrImportSourceDirNotFound");
				msg = StringUtil.replace(msg, "%dir%", dir);
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			if (!d.isDirectory())
			{
				String msg = ResourceMgr.getString("ErrImportNoDir");
				msg = StringUtil.replace(msg, "%dir%", dir);
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
		}

		String encoding = cmdLine.getValue(ARG_ENCODING);
		
		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			if (table == null && dir == null)
			{
				String msg = ResourceMgr.getString("ErrTextImportRequiresTableName");
				LogMgr.logError("WbImport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
				return result;
			}

			TextFileParser textParser = new TextFileParser();
			textParser.setTableName(table);
			if (inputFile != null)
			{
				textParser.setInputFile(inputFile);
			}
			else
			{
				textParser.setSourceDirectory(dir);
				String ext = cmdLine.getValue(ARG_FILE_EXT);
				if (ext != null) textParser.setSourceExtension(ext);
			}
			boolean multiDefault = Settings.getInstance().getBoolProperty("workbench.import.default.multilinerecord", false);
			boolean multi = cmdLine.getBoolean(ARG_MULTI_LINE, multiDefault);
			textParser.setEnableMultilineRecords(multi);
			textParser.setTargetSchema(schema);
			textParser.setConnection(aConnection);
			textParser.setAbortOnError(!cmdLine.getBoolean(ARG_CONTINUE, true));
			textParser.setTreatClobAsFilenames(cmdLine.getBoolean(ARG_CLOB_ISFILENAME, false));
			
			String delimiter = cmdLine.getValue(ARG_DELIM);
			if (delimiter != null) textParser.setDelimiter(delimiter);

			String quote = cmdLine.getValue(ARG_QUOTE);
			if (quote != null) textParser.setQuoteChar(quote);

			String format = cmdLine.getValue(ARG_DATE_FORMAT);
			if (format != null) textParser.setDateFormat(format);

			format = cmdLine.getValue(ARG_TIMESTAMP_FORMAT);
			if (format != null) textParser.setTimeStampFormat(format);

			String decimal = cmdLine.getValue(ARG_DECCHAR);
			if (decimal != null) textParser.setDecimalChar(decimal);

			textParser.setTrimValues(cmdLine.getBoolean(ARG_TRIM_VALUES, false));
			textParser.setDecodeUnicode(cmdLine.getBoolean(ARG_DECODE, false));

			if (encoding != null) textParser.setEncoding(encoding);

			textParser.setEmptyStringIsNull(cmdLine.getBoolean(ARG_EMPTY_STRING_IS_NULL, true));

			if (dir == null)
			{
				boolean header = cmdLine.getBoolean(ARG_CONTAINSHEADER);
				textParser.setContainsHeader(header);

				String filecolumns = cmdLine.getValue(ARG_FILECOLUMNS);
				if (filecolumns == null) 
				{
					filecolumns = cmdLine.getValue("columns");
					if (filecolumns != null)
					{
						result.addMessage(ResourceMgr.getString("MsgImpColumnsDeprecated"));
						result.setWarning(true);
					}
				}

				String importcolumns = cmdLine.getValue(ARG_IMPORTCOLUMNS);
				if (importcolumns != null)
				{
					try
					{	
						List cols = StringUtil.stringToList(importcolumns, ",", true);
						textParser.setImportColumns(cols);
					}
					catch (IllegalArgumentException e)
					{
						result.addMessage(textParser.getMessages());
						result.setFailure();
						return result;
					}
					
				}

				if (filecolumns != null)
				{
					List cols = StringUtil.stringToList(filecolumns, ",", true);
					try
					{
						List colIds = new ArrayList(cols.size());
						for (int i=0; i < cols.size(); i++)
						{
							ColumnIdentifier col = new ColumnIdentifier((String)cols.get(i));
							colIds.add(col);
						}
						textParser.setColumns(colIds);
					}
					catch (Exception e)
					{
						result.addMessage(textParser.getMessages());
						//result.addMessage(ResourceMgr.getString("ErrWrongColumnList"));
						//result.addMessage(ExceptionUtil.getDisplay(e));
						result.setFailure();
						return result;
					}
				}

				if (filecolumns == null)
				{
					// read column definition from header line
					// if no header was specified, the text parser
					// will assume the columns in the text file 
					// map to the column in the target table
					try
					{
						textParser.setupFileColumns();
					}
					catch (Exception e)
					{
						result.setFailure();
						result.addMessage(textParser.getMessages());
						LogMgr.logError("WbImport.execute()", ExceptionUtil.getDisplay(e),null);
						return result;
					}
				}

				// The column filter has to bee applied after the
				// columns are defined!
				String filter = cmdLine.getValue(ARG_COL_FILTER);
				if (filter != null)
				{
					addColumnFilter(filter, textParser);
				}

			}
			else
			{
				// source directory specified --> Assume files contain headers
				textParser.setContainsHeader(cmdLine.getBoolean(ARG_CONTAINSHEADER, true));
			}

			textParser.setTreatBlobsAsFilenames(cmdLine.getBoolean(ARG_BLOB_ISFILENAME, true));
			
			String filter = cmdLine.getValue(ARG_LINE_FILTER);
			if (filter != null)
			{
				textParser.setLineFilter(StringUtil.trimQuotes(filter));
			}
			imp.setProducer(textParser);
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			XmlDataFileParser xmlParser = new XmlDataFileParser();
			
			// The encoding must be set as early as possible
			// as the XmlDataFileParser might need it to read
			// the table structure!
			if (encoding != null) xmlParser.setEncoding(encoding);

			boolean verbose = cmdLine.getBoolean(ARG_VERBOSEXML, true);
			xmlParser.setUseVerboseFormat(verbose);
			
			if (dir != null)
			{
				String ext = cmdLine.getValue(ARG_FILE_EXT);
				if (ext != null) xmlParser.setSourceExtension(ext);
				xmlParser.setSourceDirectory(dir);
			}
			else
			{
				xmlParser.setSourceFile(inputFile);
				if (table != null) xmlParser.setTableName(table);
				String cols = cmdLine.getValue(ARG_IMPORTCOLUMNS);
				if (cols != null)
				{
					try
					{
						xmlParser.setColumns(cols);
					}
					catch (Exception e)
					{
						result.setFailure();
						String col = xmlParser.getMissingColumn();
						String msg = ResourceMgr.getString("ErrImportColumnNotFound").replaceAll("%name%", col);
						result.addMessage(msg);
						LogMgr.logError("WbImport.execute()", msg, null);
						return result;
					}
				}
			}

			imp.setCreateTarget(cmdLine.getBoolean(ARG_CREATE_TABLE, false));
			imp.setProducer(xmlParser);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrImportWrongParameters"));
			result.setFailure();
			return result;
		}

		this.imp.setRowActionMonitor(this.rowMonitor);
		String value = cmdLine.getValue(ARG_PROGRESS);
		if (value == null && filename != null)
		{
			int interval = DataImporter.estimateReportIntervalFromFileSize(inputFile);
			imp.setReportInterval(interval);
		}
		else if ("true".equalsIgnoreCase(value))
		{
			this.imp.setReportInterval(1);
		}
		else if ("false".equalsIgnoreCase(value))
		{
			this.imp.setReportInterval(0);
		}
		else if (value != null)
		{
			int interval = StringUtil.getIntValue(value, 0);
			this.imp.setReportInterval(interval);
		}
		else
		{
			this.imp.setReportInterval(10);
		}

		String mode = cmdLine.getValue(ARG_MODE);
		if (mode != null)
		{
			if (!imp.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrInvalidModeIgnored").replaceAll("%mode%", mode));
			}
		}

		String where = cmdLine.getValue(ARG_UPDATE_WHERE);
		imp.setWhereClauseForUpdate(where);

		if (schema != null)
		{
			imp.setTargetSchema(schema);
		}

		String keyColumns = cmdLine.getValue(ARG_KEYCOLUMNS);
		imp.setKeyColumns(keyColumns);

		boolean delete = false;
		boolean useTruncate = false;

		if (cmdLine.isArgPresent(ARG_TRUNCATE_TABLE))
		{
			delete = cmdLine.getBoolean(ARG_TRUNCATE_TABLE);
			if (delete) useTruncate = true;
		}
		else
		{
			delete = cmdLine.getBoolean(ARG_DELETE_TARGET);
			useTruncate = cmdLine.getBoolean(ARG_USE_TRUNCATE, false);
		}

		imp.setDeleteTarget(delete);
		if (delete)
		{
			imp.setUseTruncate(useTruncate);
		}

		int queueSize = cmdLine.getIntValue(ARG_BATCHSIZE,-1);
		
		if (queueSize > 0)
		{
			imp.setUseBatch(true);
			imp.setBatchSize(queueSize);
			if (cmdLine.isArgPresent(ARG_COMMIT_BATCH))
			{
				// this will disable the commitEvery property
				imp.setCommitBatch(cmdLine.getBoolean(ARG_COMMIT_BATCH, false));
			}
		}
		
		int startRow = cmdLine.getIntValue(ARG_START_ROW, -1);
		if (startRow > 0) imp.setStartRow(startRow);
		
		int endRow = cmdLine.getIntValue(ARG_END_ROW, -1);
		if (endRow > 0) imp.setEndRow(endRow);
		
		try
		{
			imp.startImport();
			if (imp.isSuccess())
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbImport.execute()", "Error importing '" + filename, e);
			result.setFailure();
		}
		catch (ParsingInterruptedException e)
		{
			// Logging already done by DataImporter
			result.setFailure();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.execute()", "Error importing '" + filename +"': " + e.getMessage(), e);
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(e));
		}
		result.addMessage(imp.getMessages());

		return result;
	}

	private void addColumnFilter(String filters, TextFileParser textParser)
	{
		if (filters == null || filters.trim().length() == 0) return;
		List filterList = StringUtil.stringToList(filters, ",", false);
		if (filterList.size() == 0) return;
		for (int i=0; i < filterList.size(); i++)
		{
			String filterDef = (String)filterList.get(i);
			List l = StringUtil.stringToList(filterDef, "=", true);
			if (l.size() != 2) continue;
			String col = (String)l.get(0);
			String regex = (String)l.get(1);
			textParser.addColumnFilter(col, StringUtil.trimQuotes(regex));
		}
	}

	public void done()
	{
		super.done();
		this.imp = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.imp != null)
		{
			this.imp.cancelExecution();
		}
	}
}
