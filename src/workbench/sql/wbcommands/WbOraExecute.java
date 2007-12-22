/*
 * WbOraExecute.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.log.LogMgr;

import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 * Orcle can only execute stored procedures if the SQL is converted to the 
 * JDBC syntax: {call procname() }
 * 
 * WbOraExecute will simply remove the EXEC or EXECUTE verb from the given 
 * SQL, and then add the necessary JDBC "syntax" to the resulting SQL statement,
 * passing the original SQL "unchanged" to the JDBC driver.
 * 
 * @author  support@sql-workbench.net
 */
public class WbOraExecute extends SqlCommand
{
//	public static final WbOraExecute EXEC = new WbOraExecute("EXEC");
//	public static final WbOraExecute EXECUTE = new WbOraExecute("EXECUTE");

	private String verb;

	private WbOraExecute(String command)
	{
		verb = command;
	}

	public String getVerb() { return this.verb; }

	/**
	 * Converts the passed sql to an Oracle compliant JDBC call and 
	 * runs the statement.
	 */
	public StatementRunnerResult execute(final String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		String realSql = "{call " + SqlUtil.stripVerb(sql) + "}";

		result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);

		try
		{
			this.currentStatement = currentConnection.createStatement();
			boolean hasResult = this.currentStatement.execute(realSql);
			result.setSuccess();
			processResults(result, hasResult);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbOraExcecute.execute()", "Error calling stored procedure", e);
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

}
