/*
 * DefaultStatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

/**
 * Empty implementation of the StatementHook interface.
 *
 * @author Thomas Kellerer
 */
public class DefaultStatementHook
	implements StatementHook
{

	@Override
	public String preExec(StatementRunner runner, String sql)
	{
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
	public void close()
	{
	}
}
