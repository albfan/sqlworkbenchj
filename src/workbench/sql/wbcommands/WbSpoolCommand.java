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
import workbench.util.CmdLineParser;
import workbench.util.LineTokenizer;
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
	
	private CmdLineParser cmdLine;
	private CmdLineParser.Option typeOption;
	private CmdLineParser.Option fileNameOption;
	private CmdLineParser.Option tableOption;
	
	public WbSpoolCommand()
	{
		cmdLine = new CmdLineParser();
		typeOption = cmdLine.addStringOption('t', "type");
		fileNameOption = cmdLine.addStringOption('f', "file");
		tableOption = cmdLine.addStringOption('b', "table");
	}

	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = aSql.indexOf(' ');
		aSql = aSql.substring(pos);
		String type = null;
		String file = null;
		String table = null;
		
		this.spooler = new DataSpooler();
		try
		{
			cmdLine.parse(new String[] { aSql });
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}
		
		type = (String)cmdLine.getOptionValue(typeOption);
		file = (String)cmdLine.getOptionValue(fileNameOption);
		table = (String)cmdLine.getOptionValue(tableOption);
		if (type == null || file == null) 
		{
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}
		
		if ("text".equalsIgnoreCase(type))
		{
			spooler.setOutputTypeText();
		}
		else if ("sql".equalsIgnoreCase(type))
		{
			spooler.setOutputTypeSqlInsert();
			if (table != null) spooler.setTableName(table);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorSpoolWrongParameters"));
			result.setFailure();
			return result;
		}
		
		this.spooler.setOutputFilename(file);
		this.spooler.setConnection(aConnection);
		String msg = ResourceMgr.getString("MsgSpoolInit");
		msg = StringUtil.replace(msg, "%type%", type.toUpperCase());
		msg = StringUtil.replace(msg, "%file%", file);
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
				this.spooler.startExport(data[0]);
				String msg = ResourceMgr.getString("MsgSpoolOk");
				aResult.addMessage(""); // force new line in output
				aResult.addMessage(msg);
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
	
	public static void main(String[] args)
	{
		String test = "spool -t type -f \"d:\\temp files\\test.txt\" -b my_table";
		String[] t = test.split(" ");
		for (int i=0; i<t.length; i++) System.out.println(t[i]);
	}
}
