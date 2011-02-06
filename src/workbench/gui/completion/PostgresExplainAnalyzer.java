/*
 * PostgresExplainAnalyzer
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;

/**
 * A statement analyzer for Postgres' EXPLAIN statement.
 *
 * It will not handle the statement that is explained. ExplainAnalyzerFactory will
 * take care of creating the correct analyzer depending on the cursor position.
 *
 * @author Thomas Kellerer
 */
public class PostgresExplainAnalyzer
	extends ExplainAnalyzer
{
	public PostgresExplainAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@Override
	protected void checkContext()
	{
		String explain = getExplainSql();
		Set<String> usedOptions = CollectionUtil.caseInsensitiveSet();
		Map<String, List<String>> options90 = get90Options();

		int analyzePosition = -1;
		int verbosePosition = -1;

		int bracketOpen = explain.indexOf('(');
		boolean use90Options = bracketOpen > -1;
		SQLLexer lexer = new SQLLexer(explain);
		SQLToken t = lexer.getNextToken(false, false);
		SQLToken last = null;
		String currentWord = null;

		while (t != null)
		{
			if (use90Options)
			{
				if (options90.containsKey(t.getContents()))
				{
					usedOptions.add(t.getContents());
				}
			}
			else
			{
				if ("ANALYZE".equalsIgnoreCase(t.getContents()))
				{
					analyzePosition = t.getCharBegin();
				}
				if ("VERBOSE".equalsIgnoreCase(t.getContents()))
				{
					verbosePosition = t.getCharBegin();
				}
			}
			last = t;
			t = lexer.getNextToken(false, false);
			if (last != null && t != null)
			{
				if (cursorPos >= last.getCharEnd() && cursorPos <= t.getCharBegin())
				{
					currentWord = last.getContents();
				}
			}
		}

		if (use90Options)
		{
			if (usedOptions.isEmpty())
			{
				elements = new ArrayList<String>(options90.keySet());
				context = CONTEXT_SYNTAX_COMPLETION;
			}
			else
			{
				String word = currentWord;
				if (options90.containsKey(word))
				{
					elements = options90.get(word);
					context = CONTEXT_STATEMENT_PARAMETER;
				}
				else
				{
					elements = CollectionUtil.arrayList();
					for (String option : options90.keySet())
					{
						if (!usedOptions.contains(option))
						{
							elements.add(option);
							context = CONTEXT_SYNTAX_COMPLETION;
						}
					}
				}
			}
		}
		else
		{
			if ( (analyzePosition == -1 && verbosePosition == -1)
				  || (verbosePosition > -1 && cursorPos <= verbosePosition))
			{
				// no option given yet, the first one must be analyze
				this.elements = CollectionUtil.arrayList("analyze");
				context = CONTEXT_SYNTAX_COMPLETION;
			}
			else if (analyzePosition > -1 && cursorPos >= analyzePosition)
			{
				// ANALYZE is already specified, only option left is verbose
				this.elements = CollectionUtil.arrayList("verbose");
				context = CONTEXT_SYNTAX_COMPLETION;
			}
			else
			{
				context = NO_CONTEXT;
			}
		}
	}

	private Map<String, List<String>> get90Options()
	{
		Map<String, List<String>> options	= new TreeMap<String, List<String>>(CaseInsensitiveComparator.INSTANCE);
		List<String> booleanValues = CollectionUtil.arrayList("true", "false");
		options.put("analyze", booleanValues);
		options.put("verbose", booleanValues);
		options.put("costs", booleanValues);
		options.put("buffers", booleanValues);
		options.put("format", CollectionUtil.arrayList("text", "xml", "json", "yaml"));
		return options;
	}

	@Override
	protected int getStatementStart(String sql)
	{
		Set<String> explainable = CollectionUtil.caseInsensitiveSet(
			"SELECT", "UPDATE", "INSERT", "DELETE", "VALUES", "EXECUTE", "DECLARE", "CREATE TABLE");

		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null)
			{
				if (explainable.contains(t.getContents()))
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
}
