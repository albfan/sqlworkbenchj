package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SingleVerbCommand extends SqlCommand
{
	public static final SqlCommand COMMIT = new SingleVerbCommand("COMMIT");
	public static final SqlCommand ROLLBACK = new SingleVerbCommand("ROLLBACK");

	private String verb;

	public SingleVerbCommand(String aVerb)
	{
		this.verb = aVerb;
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			if (aConnection.useJdbcCommit())
			{
				if ("COMMIT".equals(this.verb))
				{
					aConnection.getSqlConnection().commit();
				}
				else if ("ROLLBACK".equals(this.verb))
				{
					aConnection.getSqlConnection().rollback();
				}
			}
			else
			{
				this.currentStatement = aConnection.createStatement();
				this.currentStatement.execute(aSql);
			}

			result.addMessage(this.verb + " " + ResourceMgr.getString("MsgKnownStatementOK"));
			StringBuffer warnings = new StringBuffer();
			if (this.appendWarnings(aConnection, this.currentStatement , warnings))
			{
				result.addMessage(warnings.toString());
			}
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		finally
		{
			this.done();
		}

		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}