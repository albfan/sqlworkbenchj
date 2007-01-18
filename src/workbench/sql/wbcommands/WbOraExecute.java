/*
 * WbOraExecute.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.StringTokenizer;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbOraExecute extends SqlCommand
{
	public static final WbOraExecute EXEC = new WbOraExecute("EXEC");
	public static final WbOraExecute EXECUTE = new WbOraExecute("EXECUTE");

	private String sqlcommand;

	private WbOraExecute(String aVerb)
	{
		this.sqlcommand = aVerb;
	}

	public String getVerb() { return this.sqlcommand; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		String sql = SqlUtil.makeCleanSql(aSql, false, false, '\'');
		StringTokenizer tok = new StringTokenizer(sql, " ");
		String verb = null;
		if (tok.hasMoreTokens()) verb = tok.nextToken();
		if (!this.sqlcommand.equalsIgnoreCase(verb)) throw new Exception("Wrong syntax. " + sqlcommand + " expected!");

		String upper = sql.toUpperCase();
		int startpos = upper.indexOf(this.sqlcommand.toUpperCase());
		String realSql = "";
		if (startpos > 0)
		{
			realSql = sql.substring(0, startpos - 1);
		}
		realSql = realSql + "{call " + sql.substring(startpos + this.sqlcommand.length() + 1) + "}";

		setConnection(aConnection);
		
		result.addMessage(ResourceMgr.getString("MsgProcCallConverted") + " " + realSql);

		try
		{
			this.currentStatement = aConnection.createStatement();
			boolean hasResult = this.currentStatement.execute(realSql);

			processResults(result, hasResult);
			result.setSuccess();
		}
		catch (Exception e)
		{
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
