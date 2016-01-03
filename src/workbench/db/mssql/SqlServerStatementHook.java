/*
 * SqlServerStatementHook.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.mssql;

import java.util.List;
import java.util.Set;

import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.sql.StatementHook;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.CollectionUtil;

/**
 * A statement hook that works around SQL Server's broken handling of semicolons together with the MERGE statement.
 * <br/><br/>
 * For some idiotic reason the new MERGE statement requires to be sent <b>with</b> a semicolon at the end of the SQL string.
 * (all other statements can be sent without a semicolon, as with all other JDBC drivers).
 * <br/><br/>
 * This class works around this bug in Micrsoft's SQL parser by simply adding a semicolon
 * to all statements which apparently works fine for the Microsoft JDBC driver and the jTDS driver.
 * <br/><br/>
 * Optionally the SQL statements where this fix should be applied can be configured through the configuration
 * property;<br/>
 * <tt>workbench.db.microsoft_sql_server.semicolon.bug</tt>
 * <br/><br/>
 * If that contains a list of SQL commands, only those will be subject to this fix.<br/>
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
		if (verbsToFix.isEmpty())
		{
			verbsWithSemicolon = null;
		}
		else
		{
			verbsWithSemicolon = CollectionUtil.caseInsensitiveSet();
			verbsWithSemicolon.addAll(verbsToFix);
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
		String verb = runner.getConnection().getParsingUtil().getSqlVerb(sql);
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
	public boolean isPending()
	{
		return false;
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
	}

}
