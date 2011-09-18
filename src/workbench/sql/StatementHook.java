/*
 * StatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

/**
 *
 * @author Thomas Kellerer
 */
public interface StatementHook
{
	/**
	 * Setup the environment for the statement hook before executing the statement.
	 *
	 * @param runner   the statementRunner running the statement
	 * @param sql      the statement to be executed (after replacing possible macros)
	 */
	void preExec(StatementRunner runner, String sql);
	void postExec(StatementRunner runner, String sql, StatementRunnerResult result);
	boolean processResults();
}
