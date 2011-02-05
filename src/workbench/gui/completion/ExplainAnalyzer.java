/*
 * ExplainAnalyzer
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class ExplainAnalyzer
	extends BaseAnalyzer
{

	public ExplainAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	protected abstract int getStatementStart(String sql);

	protected String getExplainSql()
	{
		int statementStart = getStatementStart(sql);
		if (statementStart != Integer.MAX_VALUE)
		{
			return sql.substring(0, statementStart - 1);
		}
		return sql;
	}
	
	/**
	 * Return the statement to be explained, regardless of where the
	 * cursor position is.
	 */
	public String getExplainedStatement()
	{
		int statementStart = getStatementStart(this.sql);
		if (statementStart < sql.length())
		{
			return sql.substring(statementStart);
		}
		return null;
	}
}
