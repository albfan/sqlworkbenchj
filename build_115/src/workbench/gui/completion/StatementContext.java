/*
 * StatementContext.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
import java.util.List;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.WbSelectBlob;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A factory to generate a BaseAnalyzer based on a given SQL statement.
 *
 * @author Thomas Kellerer
 */
public class StatementContext
{
	private BaseAnalyzer analyzer;

	public StatementContext(WbConnection conn, String sql, int pos)
	{
		this(conn, sql, pos, true);
	}

	public StatementContext(WbConnection conn, String sql, int pos, boolean retrieve)
	{
		BaseAnalyzer subSelectAnalyzer = checkCTE(conn, sql, pos);
		if (subSelectAnalyzer == null)
		{
			subSelectAnalyzer = checkSubselect(conn, sql, pos);
		}

		BaseAnalyzer verbAnalyzer = createAnalyzer(conn, sql, pos);

		if (subSelectAnalyzer != null)
		{
			this.analyzer = subSelectAnalyzer;
			this.analyzer.setParent(verbAnalyzer);
		}
		else if (verbAnalyzer != null)
		{
			this.analyzer = verbAnalyzer;
			this.analyzer.setParent(null);
		}

		if (analyzer != null && retrieve)
		{
			analyzer.retrieveObjects();
		}
	}

	private BaseAnalyzer createAnalyzer(WbConnection conn, String sql, int pos)
	{
		String verb = SqlUtil.getSqlVerb(sql);

		BaseAnalyzer verbAnalyzer = null;

		CommandTester wbTester = new CommandTester();

		if ("SELECT".equalsIgnoreCase(verb) || WbSelectBlob.VERB.equalsIgnoreCase(verb))
		{
			verbAnalyzer = new SelectAnalyzer(conn, sql, pos);
		}
		else if ("UPDATE".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new UpdateAnalyzer(conn, sql, pos);
		}
		else if ("DELETE".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new DeleteAnalyzer(conn, sql, pos);
		}
		else if ("DROP".equalsIgnoreCase(verb) || "TRUNCATE".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new DdlAnalyzer(conn, sql, pos);
		}
		else if ("ALTER".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new AlterTableAnalyzer(conn, sql, pos);
		}
		else if ("INSERT".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new InsertAnalyzer(conn, sql, pos);
		}
		else if ("CREATE".equalsIgnoreCase(verb) || "CREATE OR REPLACE".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new CreateAnalyzer(conn, sql, pos);
		}
		else if ("EXECUTE".equalsIgnoreCase(verb) || "EXEC".equalsIgnoreCase(verb) || "WBCALL".equalsIgnoreCase(verb) || "CALL".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new ExecAnalyzer(conn, sql, pos);
		}
		else if (wbTester.isWbCommand(verb) || verb.toLowerCase().startsWith("wb"))
		{
			verbAnalyzer = new WbCommandAnalyzer(conn, sql, pos);
		}
		else if ("EXPLAIN".equalsIgnoreCase(verb))
		{
			ExplainAnalyzerFactory factory = new ExplainAnalyzerFactory();
			verbAnalyzer = factory.getAnalyzer(conn, sql, pos);
		}
		else if ("WITH".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new CteAnalyzer(conn, sql, pos);
		}
		return verbAnalyzer;
	}

	public BaseAnalyzer getAnalyzer()
	{
		return this.analyzer;
	}

	/**
	 * Checks if the cursor is positioned inside a CTE definition.
	 *
	 * @param conn  the connection to use
	 * @param sql   the sql to check
	 * @param pos   the cursor position
	 * @return a BaseAnalyzer for the current inner SQL if any.
	 */
	private BaseAnalyzer checkCTE(WbConnection conn, String sql, int pos)
	{
		String verb = SqlUtil.getSqlVerb(sql);
		if (!"WITH".equalsIgnoreCase(verb)) return null;

		CteParser cteParser = new CteParser(sql);
		List<CteDefinition> definitions = cteParser.getCteDefinitions();
		if (definitions.isEmpty()) return null;
		for (CteDefinition cte : definitions)
		{
			if (pos >= cte.getStartInStatement() && pos <= cte.getEndInStatement())
			{
				int newPos = pos - cte.getStartInStatement();
				return createAnalyzer(conn, cte.getInnerSql(), newPos);
			}
		}
		return null;
	}

	private BaseAnalyzer checkSubselect(WbConnection conn, String sql, int pos)
	{
		Set<String> unionKeywords = CollectionUtil.caseInsensitiveSet("UNION", "UNION ALL", "MINUS", "INTERSECT");

		try
		{
			SQLLexer lexer = new SQLLexer(sql);

			SQLToken t = lexer.getNextToken(false, false);
			if (t == null) return null;

			SQLToken lastToken = null;

			int lastStart = 0;
			int lastEnd = 0;
			String verb = t.getContents();

			// Will contain the position of each SELECT verb
			// if a UNION is encountered.
			List<Integer> unionStarts = new ArrayList<Integer>();
			int bracketCount = 0;
			boolean inSubselect = false;
			boolean checkForInsertSelect = verb.equals("INSERT") || verb.equals("CREATE") || verb.equals("CREATE OR REPLACE");

			while (t != null)
			{
				final String value = t.getContents();

				if ("(".equals(value))
				{
					bracketCount ++;
					if (bracketCount == 1) lastStart = t.getCharBegin();
				}
				else if (")".equals(value))
				{
					bracketCount --;
					if (inSubselect && bracketCount == 0)
					{
						lastEnd = t.getCharBegin();
						if (lastStart <= pos && pos <= lastEnd)
						{
							int newpos = pos - lastStart - 1;
							String sub = sql.substring(lastStart + 1, lastEnd);
							return new SelectAnalyzer(conn, sub, newpos);
						}
					}
					if (bracketCount == 0)
					{
						inSubselect = false;
						lastStart = 0;
						lastEnd = 0;
					}
				}
				else if (bracketCount == 0 && checkForInsertSelect && value.equals("SELECT"))
				{
					if (pos >= t.getCharBegin())
					{
						int newPos = pos - t.getCharBegin();
						return new SelectAnalyzer(conn, sql.substring(t.getCharBegin()), newPos);
					}
				}
				else if (bracketCount == 0 && unionKeywords.contains(value))
				{
					if (value.equals("UNION"))
					{
						SQLToken t2 = lexer.getNextToken(false, false);
						if (t2.getContents().equals("ALL"))
						{
							// swallow potential UNION ALL
							unionStarts.add(Integer.valueOf(t.getCharBegin()));
						}
						else
						{
							unionStarts.add(Integer.valueOf(t.getCharEnd()));

							// continue with the token just read
							lastToken = t;
							t = t2;
							continue;
						}
					}
					else
					{
						unionStarts.add(Integer.valueOf(t.getCharEnd()));
					}
				}

				if (bracketCount == 1 && lastToken.getContents().equals("(") && value.equals("SELECT"))
				{
					inSubselect = true;
				}

				lastToken = t;
				t = lexer.getNextToken(false, false);
			}

			if (unionStarts.size() > 0)
			{
				int index = 0;
				int lastPos = 0;
				while (index < unionStarts.size())
				{
					int startPos = unionStarts.get(index).intValue();
					if (lastPos <= pos && pos <= startPos)
					{
						int newPos = pos - lastPos;
						return new SelectAnalyzer(conn, sql.substring(lastPos, startPos), newPos);
					}
					lastPos = startPos;
					index ++;
				}
				// check last union
				int startPos = unionStarts.get(unionStarts.size() - 1).intValue();
				if (pos >= startPos)
				{
					int newPos = pos - startPos;
					return new SelectAnalyzer(conn, sql.substring(startPos), newPos);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("StatementContenxt.inSubSelect()", "Error when checking sub-select", e);
		}

		return null;
	}

	public boolean isStatementSupported()
	{
		return this.analyzer != null;
	}

	public List getData()
	{
		if (analyzer == null) return Collections.EMPTY_LIST;
		List result = analyzer.getData();
		if (result == null) return Collections.EMPTY_LIST;
		return result;
	}

	public String getTitle()
	{
		if (analyzer == null) return "";
		return analyzer.getTitle();
	}

}
