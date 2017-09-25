/*
 * StatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import workbench.db.WbConnection;

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
	String preExec(StatementRunner runner, String sql);
	void postExec(StatementRunner runner, String sql, StatementRunnerResult result);

	/**
	 * If true, results should be displayed (and processed)
	 * @see #fetchResults()
	 */
	boolean displayResults();

	/**
	 * If true, results should be processed (but maybe not displayed)
	 * @see #displayResults()
	 */
	boolean fetchResults();

	/**
	 * This method is called to cleanup any resources kept open by the StatementHook for the given connection.
	 *
	 * @param conn the connection
	 */
	void close(WbConnection conn);


	/**
	 * Indicates that the hook as pending statements on the current connection.
	 *
	 * If this method returns true, no additional statements should be executed on the connection
	 * until the StatementRunner has finished running the current statement.
	 *
	 * Currently this is only used by the OracleStatementHook to signal that the sessions
	 * statistics are collected and no other statements should be executed.
	 *
	 * @return true if any pending actions are open on the StatementHook
	 */
	boolean isPending();
}
