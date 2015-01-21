/*
 * StatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

	void close(WbConnection conn);

	boolean isPending();
}
