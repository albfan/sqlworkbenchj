/*
 * OracleExplainAnalyzer.java
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
package workbench.gui.completion;

import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;

/**
 * A statement analyzer for Oracle's EXPLAIN PLAN.
 *
 * It will not handle the statement that is explained. ExplainAnalyzerFactory will
 * take care of creating the correct analyzer depending on the cursor position
 * @author Thomas Kellerer
 */
public class OracleExplainAnalyzer
	extends ExplainAnalyzer
{

	public OracleExplainAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
	protected void checkContext()
	{
		SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, sql);
		SQLToken t = lexer.getNextToken(false, false);
		int setPosition = Integer.MAX_VALUE;
		int intoPosition = Integer.MAX_VALUE;
		int forPosition = Integer.MAX_VALUE;

		while (t != null)
		{
			if ("SET".equalsIgnoreCase(t.getContents()))
			{
				setPosition = t.getCharBegin();
			}
			if ("INTO".equalsIgnoreCase(t.getContents()))
			{
				intoPosition = t.getCharBegin();
			}
			if ("FOR".equalsIgnoreCase(t.getContents()))
			{
				forPosition = t.getCharBegin();
			}
			t = lexer.getNextToken(false, false);
		}
		if (setPosition == Integer.MAX_VALUE
			  && (intoPosition == Integer.MAX_VALUE || intoPosition > cursorPos)
			  && forPosition > cursorPos)
		{
			elements = CollectionUtil.arrayList("SET STATEMENT_ID=");
			if (intoPosition == Integer.MAX_VALUE)
			{
				elements.add("INTO");
			}
			context = CONTEXT_SYNTAX_COMPLETION;
		}
		else if (cursorPos > setPosition && intoPosition == Integer.MAX_VALUE && cursorPos < forPosition)
		{
			elements = CollectionUtil.arrayList("INTO");
			context = CONTEXT_SYNTAX_COMPLETION;
		}
		else if (cursorPos > intoPosition && cursorPos < forPosition)
		{
			schemaForTableList = getSchemaFromCurrentWord();
			elements = null;
			context = CONTEXT_TABLE_LIST;
		}
	}

	@Override
	protected int getStatementStart(String sql)
	{
		SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, sql);
		SQLToken t = lexer.getNextToken(false, false);
		boolean nextIsStatement = false;
		while (t != null)
		{
			if (nextIsStatement)
			{
				return t.getCharBegin();
			}
			if ("FOR".equalsIgnoreCase(t.getContents()))
			{
				nextIsStatement = true;
			}
			t = lexer.getNextToken(false, false);
		}
		return Integer.MAX_VALUE;
	}

}
