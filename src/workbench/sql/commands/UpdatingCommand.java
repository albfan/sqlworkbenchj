/*
 * SelectCommand.java
 *
 * Created on 16. November 2002, 16:40
 */

package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.log.LogMgr;

/**
 * @author  workbench@kellerer.org
 */
public class UpdatingCommand extends SqlCommand
{
	public static final SqlCommand UPDATE = new UpdatingCommand("UPDATE");
	public static final SqlCommand DELETE = new UpdatingCommand("DELETE");
	public static final SqlCommand INSERT = new UpdatingCommand("INSERT");

	private String verb;

	public UpdatingCommand(String aVerb)
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
			int updateCount = this.currentStatement.executeUpdate(aSql);
			result.addUpdateCount(updateCount);
			StringBuffer warnings = new StringBuffer();
			boolean hasWarnings = this.appendWarnings(aConnection, this.currentStatement, warnings);
			this.appendSuccessMessage(result);
			result.addMessage(updateCount + " " + ResourceMgr.getString("MsgRowsAffected"));
			if (hasWarnings) result.addMessage(warnings.toString());

			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			LogMgr.logDebug("UpdatingCommnad.execute()", "Error executing statement", e);
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