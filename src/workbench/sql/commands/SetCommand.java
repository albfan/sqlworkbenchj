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
public class SetCommand extends SqlCommand
{
	public static final String VERB = "SET";
	
	public SetCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			this.currentStatement = aConnection.createStatement();
			String[] words = aSql.split("\\s");
			boolean execSql = true;
			
			if (words.length > 2)
			{
				if (words[1].equalsIgnoreCase("serveroutput"))
				{
					if (words[2].equalsIgnoreCase("off"))
					{
						aConnection.getMetadata().disableOutput();
						result.addMessage(ResourceMgr.getString("MsgDbmsOutputDisabled"));
						execSql = false;
					}
					else if (words[2].equalsIgnoreCase("on"))
					{
						aConnection.getMetadata().enableOutput();
						result.addMessage(ResourceMgr.getString("MsgDbmsOutputEnabled"));
						execSql = false;
					}
				}
			}
				
			if (execSql) this.currentStatement.execute(aSql);

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
		return VERB;
	}

}