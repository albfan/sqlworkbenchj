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
		cmdLine.addArgument("mode");
		cmdLine.addArgument("keycolumns");
		cmdLine.addArgument("usebatch");
		cmdLine.addArgument("deletetarget");
		cmdLine.addArgument("emptystringnull");
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		imp = new DataImporter();

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

		String type = null;
		String file = null;
		String cleancr = null;

		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");

		if (type == null || file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
		imp.setCommitEvery(commit);

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

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) textParser.setEncoding(encoding);

			String columns = cmdLine.getValue("columns");
			if (columns != null)
			{
				List cols = StringUtil.stringToList(columns, ",");
				try
				{
					Iterator itr = cols.iterator();
					while (itr.hasNext())
					{
						String s = (String)itr.next();
						if (s == null || s.trim().length() == 0) itr.remove();
					}
					textParser.setColumns(cols);
				}
				catch (Exception e)
				{
					result.addMessage(ResourceMgr.getString("ErrorWrongColumnList"));
					result.setFailure();
					return result;
				}
				textParser.setEmptyStringIsNull(cmdLine.getBoolean("emptystringnull"));
			}
			if (header && columns == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorHeaderOrColumnDefRequired"));
				result.setFailure();
				return result;
			}
			imp.setProducer(textParser);
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			XmlDataFileParser xmlParser = new XmlDataFileParser(file);
			if (table != null) xmlParser.setTableName(table);

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) xmlParser.setEncoding(encoding);
			imp.setProducer(xmlParser);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);
		this.imp.setConnection(aConnection);
		this.imp.setRowActionMonitor(this.rowMonitor);
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

		boolean useBatch = cmdLine.getBoolean("usebatch");
		boolean delete = cmdLine.getBoolean("deletetarget");
		imp.setUseBatch(useBatch);
		imp.setDeleteTarget(delete);

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
		long rows = imp.getInsertedRows();
		msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted");
		result.addMessage(msg);
		rows = imp.getUpdatedRows();
		msg = rows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated");
		result.addMessage(msg);
		return result;
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
