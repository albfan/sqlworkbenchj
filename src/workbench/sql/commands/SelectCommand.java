/*
 * SelectCommand.java
 *
 * Created on 16. November 2002, 16:40
 */

package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SelectCommand extends SqlCommand
{

	public static final String VERB = "SELECT";
	private int maxRows = 0;

	public SelectCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		try
		{
			this.currentStatement = aConnection.createStatement();
			this.currentStatement.setMaxRows(this.maxRows);
			ResultSet rs = this.currentStatement.executeQuery(aSql);
			if (rs != null)
			{
				result.addResultSet(rs);
			}
			else
			{
				throw new WbException(ResourceMgr.getString("MsgReceivedNullResultSet"));
			}
			StringBuffer warnings = new StringBuffer();

			this.appendSuccessMessage(result);
			if (this.appendWarnings(aConnection, this.currentStatement, warnings))
			{
				result.addMessage(warnings.toString());
			}
			result.setSuccess();
		}
		catch (Throwable e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
		}
		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

	public boolean supportsMaxRows()
	{
		return true;
	}

	public void setMaxRows(int max)
	{
		if (max >= 0)
			this.maxRows = max;
		else
			this.maxRows = 0;
	}

}