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

import workbench.db.WbConnection;
import workbench.sql.StatementHook;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

/**
 * A statement hook that works around SQL Server's stupid implementation of SQL statements.
 *
 * For some idiotic reason the new MERGE statement requires to be sent with a semicolon at the end.
 * (all other statements can be sent without a semicolon, as in all other DBMS)
 *
 * This class works around this bug in Micrsoft's SQL parser by simply adding a semicolon
 * to all statements which apparently works fine for the Microsoft JDBC driver and the jTDS driver.
 *
 * Implementing this workaround by adding the semicolon is easier than rewriting the SQL parser
 * to optionally return the delimiter used.
 *
 * @author Thomas Kellerer
 */
public class SqlServerStatementHook
	implements StatementHook
{

	@Override
	public String preExec(StatementRunner runner, String sql)
	{
		return sql + ";";
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
