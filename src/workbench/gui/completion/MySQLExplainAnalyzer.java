/*
 * MySQLExplainAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLExplainAnalyzer
	extends ExplainAnalyzer
{

	public MySQLExplainAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
	protected int getStatementStart(String sql)
	{
		try
		{
			SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, sql);
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null)
			{
				// Only SELECT statements are supported
				if (t.getText().equalsIgnoreCase("SELECT"))
				{
					return t.getCharBegin();
				}
				t = lexer.getNextToken(false, false);
			}
			return Integer.MAX_VALUE;
		}
		catch (Exception e)
		{
			return Integer.MAX_VALUE;
		}
	}

	@Override
	protected void checkContext()
	{
		Set<String> allOptions = CollectionUtil.caseInsensitiveSet("EXTENDED", "PARTITIONS");
		Set<String> usedOptions = CollectionUtil.caseInsensitiveSet();

		String sqlToParse = getExplainSql();
		try
		{
			SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, sqlToParse);
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null)
			{
				String v = t.getText();
				if (!v.equalsIgnoreCase("EXPLAIN"))
				{
					usedOptions.add(v);
				}
				t = lexer.getNextToken(false, false);
			}
			if (usedOptions.isEmpty())
			{
				this.elements = new ArrayList<>(allOptions);
			}
			else
			{
				// only one option allowed
				this.elements = Collections.emptyList();
			}
			this.context = CONTEXT_SYNTAX_COMPLETION;
		}
		catch (Exception e)
		{
			LogMgr.logError("MySQLExplainAnalyzer.checkContext()", "Error getting optiosn", e);
			this.elements = new ArrayList<>();
			this.context = NO_CONTEXT;
		}
	}

}
