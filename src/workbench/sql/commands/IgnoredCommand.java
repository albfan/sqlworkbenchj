package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * This class simply ignores the command and does not send it to the DBMS
 * 
 * Thus scripts e.g. intended for SQL*Plus (containing WHENEVER or EXIT) 
 * can be executed from within the workbench.
 * The commands to be ignored can be configured in workbench.settings
 *
 * @author  workbench@kellerer.org
 */
public class IgnoredCommand extends SqlCommand
{
	private String verb;

	public IgnoredCommand(String aVerb)
	{
		this.verb = aVerb;
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		String msg = ResourceMgr.getString("MsgCommandIgnored").replaceAll("%verb%", this.verb);
		result.addMessage(msg);
		result.setSuccess();
		this.done();
		return result;
	}

	public String getVerb()
	{
		return verb;
	}
	
}