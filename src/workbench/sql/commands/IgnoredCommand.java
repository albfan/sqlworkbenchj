package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * This class implements a wrapper for Oracle's SET command
 * Oracle's SET command is only valid from within SQL*Plus.
 * By supplying an implementation for the Workbench, we can ignore the errors
 * reported by the JDBC interface, so that SQL scripts intended for SQL*Plus
 * can also be run from within the workbench
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
		try
		{
			this.currentStatement = aConnection.createStatement();
			this.currentStatement.execute(aSql);

			StringBuffer warnings = new StringBuffer();
			if (this.appendWarnings(aConnection, this.currentStatement , warnings))
			{
				result.addMessage(warnings.toString());
			}
			this.currentStatement.close();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgSetErrorIgnored") + ": " + e.getMessage());
		}
		finally
		{
			result.setSuccess();
			this.done();
		}

		return result;
	}

	public String getVerb()
	{
		return verb;
	}
	public static SqlCommand[] getCommandsToIgnoreForOracle()
	{
		SqlCommand[] result = new SqlCommand[2];
		result[0] = new IgnoredCommand("PROMPT");
		result[1] = new IgnoredCommand("WHENEVER");
		return result;
	}
}