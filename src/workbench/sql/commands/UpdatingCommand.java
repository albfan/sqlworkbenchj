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
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		try
		{
			this.currentStatement = aConnection.createStatement();
			int updateCount = this.currentStatement.executeUpdate(aSql);
			StringBuffer warnings = new StringBuffer();
			this.appendWarnings(aConnection, this.currentStatement, warnings);
			result.addMessage(ResourceMgr.getString("MsgStatementOK"));
			result.addMessage(updateCount + " " + ResourceMgr.getString("MsgRowsAffected"));
			if (warnings.toString().length() > 0) result.addMessage(warnings.toString());
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
