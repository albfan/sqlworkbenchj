/*
 * WbImport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
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

	private ArgumentParser cmdLine;

	public WbImport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("type");
		cmdLine.addArgument("file");
		cmdLine.addArgument("table");
		cmdLine.addArgument("delimiter");
		cmdLine.addArgument("quotechar");
		cmdLine.addArgument("dateformat");
		cmdLine.addArgument("timestampformat");
		cmdLine.addArgument("decimal");
		cmdLine.addArgument("commitevery");
		cmdLine.addArgument("header");
		cmdLine.addArgument("encoding");
		cmdLine.addArgument("columns");
		cmdLine.addArgument("filecolumns");
		cmdLine.addArgument("mode");
		cmdLine.addArgument("keycolumns");
		cmdLine.addArgument("usebatch");
		cmdLine.addArgument("batchsize");
		cmdLine.addArgument("deletetarget");
		cmdLine.addArgument("emptystringnull");
		cmdLine.addArgument("continueonerror");
		cmdLine.addArgument("decode");
		cmdLine.addArgument("verbosexml");
		cmdLine.addArgument("importcolumns");
		cmdLine.addArgument("columnfilter");
		cmdLine.addArgument("linefilter");
		cmdLine.addArgument("showprogress");
		
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

		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");

		
		if (type == null || file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportFileMissing"));
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		int commit = cmdLine.getIntValue("commitevery",-1);
		imp.setCommitEvery(commit);
		
		imp.setContinueOnError(cmdLine.getBoolean("continueonerror", true));
			
		String table = cmdLine.getValue("table");

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

			String delimiter = cmdLine.getValue("delimiter");
			if (delimiter != null) textParser.setDelimiter(delimiter);

			String quote = cmdLine.getValue("quotechar");
			if (quote != null) textParser.setQuoteChar(quote);

			String format = cmdLine.getValue("dateformat");
			if (format != null) textParser.setDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) textParser.setTimeStampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) textParser.setDecimalChar(format);

			boolean header = cmdLine.getBoolean("header");
			textParser.setContainsHeader(header);

			textParser.setDecodeUnicode(cmdLine.getBoolean("decode"));

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) textParser.setEncoding(encoding);

			String columns = cmdLine.getValue("filecolumns");
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
				textParser.setEmptyStringIsNull(cmdLine.getBoolean("emptystringnull", true));
			}

			if (!header && columns == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorHeaderOrColumnDefRequired"));
				result.setFailure();
				return result;
			}
			
			// the import columns have to set after setting 
			// the file columns!
			columns = cmdLine.getValue("importcolumns");
			if (columns != null)
			{
				List cols = StringUtil.stringToList(columns, ",", true);
				textParser.setImportColumns(cols);
			}
			
			// The column filter has to bee applied after the 
			// columns are defined!
			String filter = cmdLine.getValue("columnfilter");
			if (filter != null)
			{
				addColumnFilter(filter, textParser);
			}
			
			filter = cmdLine.getValue("linefilter");
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

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) xmlParser.setEncoding(encoding);
			
			boolean verbose = cmdLine.getBoolean("verbosexml", true);
			xmlParser.setUseVerboseFormat(verbose);
				
			imp.setProducer(xmlParser);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);
		
		this.imp.setRowActionMonitor(this.rowMonitor);
		this.imp.setReportProgress(cmdLine.getBoolean("showprogress",true));
		
		String mode = cmdLine.getValue("mode");
		if (mode != null)
		{

			if (!imp.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrorInvalidModeIgnored").replaceAll("%mode%", mode));
			}
		}

		String keyColumns = cmdLine.getValue("keycolumns");
		imp.setKeyColumns(keyColumns);

		String msg = ResourceMgr.getString("MsgImportingFile");
		msg += " " + file;
		if (table != null)
		{
			msg += " " + ResourceMgr.getString("MsgImportTable");
			msg += ": " + table.toUpperCase();
		}
		result.addMessage(msg);

		boolean delete = cmdLine.getBoolean("deletetarget");
		imp.setDeleteTarget(delete);
		
		boolean useBatch = cmdLine.getBoolean("usebatch");
		imp.setUseBatch(useBatch);
		if (useBatch && cmdLine.isArgPresent("batchsize"))
		{
			int queueSize = cmdLine.getIntValue("batchsize",-1);
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