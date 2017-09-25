/*
 * DefaultStatementHook.java
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
	public boolean isPending()
	{
		return false;
	}

	@Override
	public boolean fetchResults()
	{
		return true;
	}

	@Override
	public void close(WbConnection conn)
	{
	}
}
