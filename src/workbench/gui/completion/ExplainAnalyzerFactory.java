/*
 * ExplainAnalyzerFactory.java
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
package workbench.gui.completion;

import workbench.db.WbConnection;

/**
 * A class to create instances of ExplainAnalyzer for different databases.
 *
 * It also handles the case where the cursor is positioned in the actual
 * statement to be explained. In that case a "regular" analyzer for that
 * statement is returned.
 *
 * @author Thomas Kellerer
 */
public class ExplainAnalyzerFactory
{

	public ExplainAnalyzerFactory()
	{
	}

	public BaseAnalyzer getAnalyzer(WbConnection con, String sql, int cursorPos)
	{
		ExplainAnalyzer explain = null;

		if (con.getMetadata().isOracle())
		{
			explain = new OracleExplainAnalyzer(con, sql, cursorPos);
		}
		else if (con.getMetadata().isPostgres())
		{
			explain = new PostgresExplainAnalyzer(con, sql, cursorPos);
		}
		else if (con.getMetadata().isMySql())
		{
			explain = new MySQLExplainAnalyzer(con, sql, cursorPos);
		}
		else
		{
			return null;
		}

		int start = explain.getStatementStart(sql);

		// completion is for the actual explained statement, not for the EXPLAIN
		if (cursorPos >= start)
		{
			StatementContext context = new StatementContext(con, explain.getExplainedStatement(), cursorPos - start, false);
			return context.getAnalyzer();
		}

		return explain;
	}
}
