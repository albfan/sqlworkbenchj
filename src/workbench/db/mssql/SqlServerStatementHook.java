/*
 * SqlServerStatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.util.List;
import java.util.Set;

import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.sql.StatementHook;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A statement hook that works around SQL Server's broken handling of semicolons together with the MERGE statement.
 * <br/><br/>
 * For some idiotic reason the new MERGE statement requires to be sent with a semicolon at the end.
 * (all other statements can be sent without a semicolon, as in all other DBMS)
 * <br/><br/>
 * This class works around this bug in Micrsoft's SQL parser by simply adding a semicolon
 * to all statements which apparently works fine for the Microsoft JDBC driver and the jTDS driver.
 * <br/><br/>
 * Optionally the SQL statements where this fix should be applied can be configured through the configuration
 * property;<br/>
 * <tt>workbench.db.microsoft_sql_server.semicolon.bug</tt>
 * <br/><br/>
 * If that contains a list of SQL commands, only those will be subject to this fix. <br/>
 * The following definition would limit this workaround to the MERGE statement:<br/>
 * <tt>workbench.db.microsoft_sql_server.semicolon.bug=merge</tt>
 *
 * @author Thomas Kellerer
 */
public class SqlServerStatementHook
	implements StatementHook
{

	private final Set<String> verbsWithSemicolon;

	public SqlServerStatementHook()
	{
		List<String> verbsToFix = Settings.getInstance().getListProperty("workbench.db.microsoft_sql_server.semicolon.bug", false);
		if (!verbsToFix.isEmpty())
		{
			verbsWithSemicolon = CollectionUtil.caseInsensitiveSet();
			verbsWithSemicolon.addAll(verbsToFix);
		}
		else
		{
			verbsWithSemicolon = null;
		}
	}


	@Override
	public String preExec(StatementRunner runner, String sql)
	{
		// if no verb was defined to apply the workaround for the broken semicolon handling
		// then add the semicolon to all statements.
		if (verbsWithSemicolon == null)
		{
			return sql + ";";
		}

		// If verbs were defined, only append the (unnecessary) semicolon to those
		String verb = SqlUtil.getSqlVerb(sql);
		if (verbsWithSemicolon.contains(verb))
		{
			return sql + ";";
		}
		return sql;
	}

	@Override
	public void postExec(StatementRunner runner, String sql, StatementRunnerResult result)
	{
	}

	@Override
	public boolean displayResults()
	{
		return true;
	}

	@Override
	public boolean fetchResults()
	{
		return true;
	}

	@Override
	public void close(WbConnection conn)
	{
		// nothing to do
	}

}
