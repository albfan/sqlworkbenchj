/*
 * StatementContext.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
	private CommandTester wbTester = new CommandTester();
	private final Set<String> unionKeywords = CollectionUtil.hashSet("UNION", "UNION ALL", "MINUS", "INTERSECT");

	public StatementContext(WbConnection conn, String sql, int pos)
	{
		String verb = SqlUtil.getSqlVerb(sql);

		BaseAnalyzer subSelectAnalyzer = checkSubselect(conn, sql, pos);
		BaseAnalyzer verbAnalyzer = null;

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
		else if ("EXEC".equalsIgnoreCase(verb) || "WBCALL".equalsIgnoreCase(verb) || "CALL".equalsIgnoreCase(verb))
		{
			verbAnalyzer = new ExecAnalyzer(conn, sql, pos);
		}
		else if (wbTester.isWbCommand(verb) || verb.toLowerCase().startsWith("wb"))
		{
			verbAnalyzer = new WbCommandAnalyzer(conn, sql, pos);
		}

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

		if (analyzer != null)
		{
			analyzer.retrieveObjects();
		}
	}

	public BaseAnalyzer getAnalyzer() { return this.analyzer; }

	private BaseAnalyzer checkSubselect(WbConnection conn, String sql, int pos)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);

			SQLToken t = lexer.getNextToken(false, false);
			SQLToken lastToken = null;

			int lastStart = 0;
			int lastEnd = 0;
			String verb = t.getContents();

			// Will contain the position of each SELECT verb
			// if a UNION is encountered.
			List<Integer> unionStarts = new ArrayList<Integer>();
			int bracketCount = 0;
			boolean inSubselect = false;
			boolean checkForInsertSelect = verb.equals("INSERT")
				|| verb.equals("CREATE")
				|| verb.equals("CREATE OR REPLACE");

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
