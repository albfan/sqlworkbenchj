package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.DataSpooler;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.sql.commands.SelectCommand;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbSpoolCommand
	extends SelectCommand
{
	public static final String VERB = "SPOOL";
	public DataSpooler spooler;
	private int instance;

	private ArgumentParser cmdLine;

	public WbSpoolCommand()
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

		String type = null;
		String file = null;
		
		String cleancr = null;
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
		
		if ("text".equalsIgnoreCase(type))
		{
			
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
		else if ("sql".equalsIgnoreCase(type))
		{
			spooler.setOutputTypeSqlInsert();
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
			String format = cmdLine.getValue("dateformat");
			if (format != null) spooler.setTextDateFormat(format);

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
		String msg = ResourceMgr.getString("MsgSpoolInit");
		msg = StringUtil.replace(msg, "%type%", type.toUpperCase());
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
				this.spooler.setSql(aResult.getSourceCommand());
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
				msg = ResourceMgr.getString("MsgSpoolSource") + " " + aResult.getSourceCommand();
				aResult.addMessage(msg);
				msg = ResourceMgr.getString("MsgSpoolTarget") + " " + this.spooler.getOutputFilename();
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
			this.spooler.stopExport();
		}
	}
}
