/*
 * MySQLExplainAnalyzer
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
import java.util.Set;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
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
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null)
			{
				// Only SELECT statements are supported
				if (t.getContents().equalsIgnoreCase("SELECT"))
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
			SQLLexer lexer = new SQLLexer(sqlToParse);
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null)
			{
				if (!t.getContents().equalsIgnoreCase("EXPLAIN"))
				{
					usedOptions.add(t.getContents());
				}
				t = lexer.getNextToken(false, false);
			}
			allOptions.removeAll(usedOptions);
			this.elements = new ArrayList<String>(allOptions);
			this.context = CONTEXT_SYNTAX_COMPLETION;
		}
		catch (Exception e)
		{
			LogMgr.logError("MySQLExplainAnalyzer.checkContext()", "Error getting optiosn", e);
			this.elements = new ArrayList<String>();
			this.context = NO_CONTEXT;
		}
	}

}
