/*
 * WbImport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.db.importer.TextFileParser;
import workbench.db.importer.XmlDataFileParser;
import workbench.exception.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  info@sql-workbench.net
 */
public class WbImport extends SqlCommand
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
	public static final String ARG_CONTAINSHEADER = "header";
	public static final String ARG_ENCODING = "encoding";
	public static final String ARG_FILECOLUMNS = "filecolumns";
	public static final String ARG_MODE = "mode";
	public static final String ARG_KEYCOLUMNS = "keycolumns";
	public static final String ARG_USEBATCH = "usebatch";
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

	private ArgumentParser cmdLine;

	public WbImport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_TYPE);
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
		cmdLine.addArgument(ARG_USEBATCH);
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

		this.isUpdatingCommand = true;
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		imp = new DataImporter();
		this.imp.setConnection(aConnection);

		StatementRunnerResult result = new StatementRunnerResult(aSql);
		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = aSql.indexOf(' ');
		if (pos > -1)
			aSql = aSql.substring(pos);
		else
			aSql = "";

		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrorUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		String type = null;
		String file = null;
		String cleancr = null;

		type = cmdLine.getValue(ARG_TYPE);
		file = cmdLine.getValue(ARG_FILE);


		if (type == null || file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportFileMissing"));
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);

		int commit = cmdLine.getIntValue(ARG_COMMIT,-1);
		imp.setCommitEvery(commit);

		imp.setContinueOnError(cmdLine.getBoolean(ARG_CONTINUE, true));

		String table = cmdLine.getValue(ARG_TARGETTABLE);

		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			if (table == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorTextImportRequiresTableName"));
				result.setFailure();
				return result;
			}

			TextFileParser textParser = new TextFileParser(file);
			textParser.setTableName(table);
			textParser.setConnection(aConnection);

			String delimiter = cmdLine.getValue(ARG_DELIM);
			if (delimiter != null) textParser.setDelimiter(delimiter);

			String quote = cmdLine.getValue(ARG_QUOTE);
			if (quote != null) textParser.setQuoteChar(quote);

			String format = cmdLine.getValue(ARG_DATE_FORMAT);
			if (format != null) textParser.setDateFormat(format);

			format = cmdLine.getValue(ARG_TIMESTAMP_FORMAT);
			if (format != null) textParser.setTimeStampFormat(format);

			format = cmdLine.getValue(ARG_DECODE);
			if (format != null) textParser.setDecimalChar(format);

			boolean header = cmdLine.getBoolean(ARG_CONTAINSHEADER);
			textParser.setContainsHeader(header);

			textParser.setDecodeUnicode(cmdLine.getBoolean(ARG_DECODE));

			String encoding = cmdLine.getValue(ARG_ENCODING);
			if (encoding != null) textParser.setEncoding(encoding);

			// filecolumns is the new parameter
			// -columns is deprecated
			String columns = cmdLine.getValue(ARG_FILECOLUMNS);
			if (columns == null) columns = cmdLine.getValue("columns");

			if (columns != null)
			{
				List cols = StringUtil.stringToList(columns, ",", true);
				try
				{
					textParser.setColumns(cols);
				}
				catch (Exception e)
				{
					result.addMessage(ResourceMgr.getString("ErrorWrongColumnList"));
					result.setFailure();
					return result;
				}
				textParser.setEmptyStringIsNull(cmdLine.getBoolean(ARG_EMPTY_STRING_IS_NULL, true));
			}

			if (!header && columns == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorHeaderOrColumnDefRequired"));
				result.setFailure();
				return result;
			}

			// the import columns have to set after setting
			// the file columns!
			columns = cmdLine.getValue(ARG_IMPORTCOLUMNS);
			if (columns != null)
			{
				List cols = StringUtil.stringToList(columns, ",", true);
				textParser.setImportColumns(cols);
			}

			// The column filter has to bee applied after the
			// columns are defined!
			String filter = cmdLine.getValue(ARG_COL_FILTER);
			if (filter != null)
			{
				addColumnFilter(filter, textParser);
			}

			filter = cmdLine.getValue(ARG_LINE_FILTER);
			if (filter != null)
			{
				textParser.setLineFilter(StringUtil.trimQuotes(filter));
			}
			imp.setProducer(textParser);
		}

		else if ("xml".equalsIgnoreCase(type))
		{
			XmlDataFileParser xmlParser = new XmlDataFileParser(file);
			if (table != null) xmlParser.setTableName(table);

			String encoding = cmdLine.getValue(ARG_ENCODING);
			if (encoding != null) xmlParser.setEncoding(encoding);

			boolean verbose = cmdLine.getBoolean(ARG_VERBOSEXML, true);
			xmlParser.setUseVerboseFormat(verbose);
			
			String cols = cmdLine.getValue(ARG_IMPORTCOLUMNS);
			if (cols != null)
			{
				xmlParser.setColumns(cols);
			}
			imp.setProducer(xmlParser);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		this.imp.setRowActionMonitor(this.rowMonitor);
		String value = cmdLine.getValue(ARG_PROGRESS);
		if (value == null)
		{
			int interval = DataImporter.estimateReportIntervalFromFileSize(file);
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
		else
		{
			int interval = StringUtil.getIntValue(value, 0);
			this.imp.setReportInterval(interval);
		}

		String mode = cmdLine.getValue(ARG_MODE);
		if (mode != null)
		{
			if (!imp.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrorInvalidModeIgnored").replaceAll("%mode%", mode));
			}
		}

		String keyColumns = cmdLine.getValue(ARG_KEYCOLUMNS);
		imp.setKeyColumns(keyColumns);

		String msg = ResourceMgr.getString("MsgImportingFile");
		msg += " " + file;
		if (table != null)
		{
			msg += " " + ResourceMgr.getString("MsgImportTable");
			msg += ": " + table.toUpperCase();
		}
		result.addMessage(msg);

		boolean delete = cmdLine.getBoolean(ARG_DELETE_TARGET);
		imp.setDeleteTarget(delete);

		boolean useBatch = cmdLine.getBoolean(ARG_USEBATCH);
		imp.setUseBatch(useBatch);
		if (useBatch && cmdLine.isArgPresent(ARG_BATCHSIZE))
		{
			int queueSize = cmdLine.getIntValue(ARG_BATCHSIZE,-1);
			if (queueSize > 0)
			{
				imp.setBatchSize(queueSize);
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrorImportInvalidBatchSize"));
			}
		}

		try
		{
			imp.startImport();
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.execute()", "Error when importing file (" + file +")", e);
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(e));
		}
		this.addWarnings(result);
		this.addErrors(result);
		if (result.isSuccess())
		{
			long rows = imp.getInsertedRows();
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted");
			result.addMessage(msg);
			rows = imp.getUpdatedRows();
			msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated");
			result.addMessage(msg);
		}
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

	private void addWarnings(StatementRunnerResult result)
	{
		String[] err = imp.getWarnings();
		for (int i=0; i < err.length; i++)
		{
			result.addMessage(err[i]);
		}
	}

	private int estimateReportInterval(String filename)
	{
		try
		{
			long records = FileUtil.estimateRecords(filename, 10);
			if (records < 100)
			{
				return 1;
			}
			else if (records < 1000)
			{
				return 10;
			}
			else if (records < 100000)
			{
				return 1000;
			}
			else
			{
				return 5000;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.estimateReportInterval()", "Error when checking input file", e);
			return 0;
		}
	}
	private void addErrors(StatementRunnerResult result)
	{
		String[] warn = imp.getErrors();
		for (int i=0; i < warn.length; i++)
		{
			result.addMessage(warn[i]);
		}
		if (warn.length > 0)
		{
			// force an empty line if we had warnings
			result.addMessage("");
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