package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.db.DataSpooler;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbExport extends SqlCommand
{
	public static final String VERB = "EXPORT";
	public DataSpooler spooler;

	private ArgumentParser cmdLine;

	public WbExport()
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
		cmdLine.addArgument("cleancr");
		cmdLine.addArgument("charfunc");
		cmdLine.addArgument("concat");
		cmdLine.addArgument("commitevery");
		cmdLine.addArgument("header");
		cmdLine.addArgument("createtable");
		cmdLine.addArgument("nodata");
		cmdLine.addArgument("encoding");
		cmdLine.addArgument("showprogress");
		cmdLine.addArgument("sqlinsert");
		cmdLine.addArgument("sqlupdate");
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, WbException
	{
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
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
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
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		String type = null;
		String file = null;

		this.spooler = new DataSpooler();

		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");

		if (type == null || file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}

		String table = cmdLine.getValue("table");
		type = type.toLowerCase();

		String typeDisplay = null;

		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			// change the contents of type in order to display it properly
			typeDisplay = "Text";
			spooler.setOutputTypeText();
			String delimiter = cmdLine.getValue("delimiter");
			if (delimiter != null) spooler.setTextDelimiter(delimiter);

			String quote = cmdLine.getValue("quotechar");
			if (quote != null) spooler.setTextQuoteChar(quote);

			String format = cmdLine.getValue("dateformat");
			if (format != null) spooler.setTextDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) spooler.setTextTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) spooler.setDecimalSymbol(format);

			String header = cmdLine.getValue("header");
			spooler.setExportHeaders(StringUtil.stringToBool(header));
			spooler.setCleanCarriageReturns(cmdLine.getBoolean("cleancr"));
		}
		else if (type.startsWith("sql"))
		{
			if (type.equals("sql") || type.equals("sqlinsert"))
			{
				spooler.setOutputTypeSqlInsert();
				typeDisplay = "SQL INSERT";
			}
			else if (type.equals("sqlupdate"))
			{
				spooler.setOutputTypeSqlUpdate();
				typeDisplay = "SQL UPDATE";
			}
			String create = cmdLine.getValue("createtable");
			spooler.setIncludeCreateTable(StringUtil.stringToBool(create));
			spooler.setChrFunction(cmdLine.getValue("charfunc"));
			spooler.setConcatString(cmdLine.getValue("concat"));
			int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
			spooler.setCommitEvery(commit);
			if (table != null) spooler.setTableName(table);
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			// change the contents of type in order to display it properly
			typeDisplay = "XML";
			String format = cmdLine.getValue("dateformat");
			if (format != null) spooler.setTextDateFormat(format);

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) spooler.setXmlEncoding(encoding);

			format = cmdLine.getValue("timestampformat");
			if (format != null) spooler.setTextTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) spooler.setDecimalSymbol(format);

			spooler.setOutputTypeXml();
			if (table != null) spooler.setTableName(table);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);
		this.spooler.setOutputFilename(file);
		this.spooler.setConnection(aConnection);
		this.spooler.setRowMonitor(this.rowMonitor);

		String progress = cmdLine.getValue("showprogress");
		this.spooler.setShowProgress("true".equalsIgnoreCase(progress));

		String msg = ResourceMgr.getString("MsgSpoolInit");
		msg = StringUtil.replace(msg, "%type%", typeDisplay);
		msg = StringUtil.replace(msg, "%file%", file);
		//msg = msg + " quote=" + spooler.getTextQuoteChar();
		result.addMessage(msg);
		return result;
	}

	public boolean isResultSetConsumer()
	{
		return true;
	}

	public void consumeResult(StatementRunnerResult aResult)
	{
		try
		{
			if (aResult.hasResultSets())
			{
				ResultSet[] data = aResult.getResultSets();
				String sql = aResult.getSourceCommand();
				this.spooler.setSql(sql);
				long rowCount = this.spooler.startExport(data[0]);

				String msg = null;

				if (spooler.isSuccess())
				{
					msg = ResourceMgr.getString("MsgSpoolOk").replaceAll("%rows%", Long.toString(rowCount));
					aResult.addMessage(""); // force new line in output
					aResult.addMessage(msg);
				}
				String[] spoolMsg = this.spooler.getErrors();
				if (spoolMsg.length > 0)
				{
					for (int i=0; i < spoolMsg.length; i++)
					{
						aResult.addMessage(spoolMsg[i]);
					}
					aResult.addMessage("");
				}

				String warn = ResourceMgr.getString("TxtWarning");
				spoolMsg = this.spooler.getWarnings();
				if (spoolMsg.length > 0)
				{
					for (int i=0; i < spoolMsg.length; i++)
					{
						aResult.addMessage(warn + ": " + spoolMsg[i]);
					}
					aResult.addMessage("");
				}
				//msg = ResourceMgr.getString("MsgSpoolSource") + " " + aResult.getSourceCommand();
				//aResult.addMessage(msg);
				msg = ResourceMgr.getString("MsgSpoolTarget") + " " + this.spooler.getFullOutputFilename();
				aResult.addMessage(msg);
				aResult.clearResultSets();
				aResult.setSuccess();
			}
		}
		catch (Exception e)
		{
			aResult.setFailure();
			aResult.addMessage(ResourceMgr.getString("MsgSpoolError"));
			String msg = e.getMessage();
			if (msg == null)
			{
				msg = StringUtil.getStackTrace(e);
			}
			aResult.addMessage(msg);
			LogMgr.logError("WbSpoolCommand.consumeResult()", "Error spooling data", e);
		}
	}

	public void done()
	{
		super.done();
		this.spooler = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.spooler != null)
		{
			this.spooler.cancelExecution();
		}
	}
}