/*
 * SelectCommand.java
 *
 * Created on 16. November 2002, 16:40
 */

package workbench.sql.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
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

	public SelectCommand()
	{
	}
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		try
		{
			this.currentStatement = aConnection.createStatement();
			ResultSet rs = this.currentStatement.executeQuery(aSql);
			result.addResultSet(rs);
			StringBuffer warnings = new StringBuffer();
			this.appendWarnings(aConnection, this.currentStatement, warnings);
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
		return VERB;
	}
	
}
