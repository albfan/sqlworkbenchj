/*
 * JoinCreator
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.fksupport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import workbench.db.WbConnection;
import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.StatementContext;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.syntax.SqlKeywordHelper;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 * A class to automatically create the needed joins between tables.
 * <br/>
 * Based on the current cursor position the passed SQL will be analyzed, the
 * join tables extraced and the correct join condition generated.
 * <br/>
 * Currently this is limited to two tables, multi-table JOIN ... ON clauses are not supported.
 * <br/>
 * In statements with mutiple JOIN conditions, always the last two tables are joined.
 * <br/>
 * Sub-Selects are not supported.
 *
 * @author Thomas Kellerer
 */
public class JoinCreator
{
	private String sql;
	private WbConnection connection;
	private int cursorPos;
	private Map<Integer, TableAlias> tablePositions;
	private Set<String> keywords;

	public JoinCreator(String statement, int positionInStatement, WbConnection dbConn)
	{
		this.connection = dbConn;
		SqlKeywordHelper helper = new SqlKeywordHelper(connection.getDbId());
		keywords = helper.getKeywords();
		int realPos = setSql(statement, positionInStatement);
		setCursorPosition(realPos);
	}

	public void setCursorPosition(int position)
	{
		cursorPos = position;
		retrieveTablePositions();
	}

	/**
	 * Parse the given SQL for potential sub-selects
	 * @param sqlToUse the sql from the editor
	 * @param position the position of the cursor in the passed statement
	 * @return the positioin of the cursor in the statement that is used for analysis
	 */
	private int setSql(String sqlToUse, int position)
	{
		StatementContext context = new StatementContext(connection, sqlToUse, position, false);
		BaseAnalyzer analyzer = context.getAnalyzer();
		if (analyzer == null)
		{
			// for some reason the StatementContext could not handle the statement
			// so try the basic statement anyway
			this.sql = sqlToUse;
			return position;
		}
		this.sql = analyzer.getAnalyzedSql();
		return analyzer.getCursorPosition();
	}

	public String getJoinCondition()
		throws SQLException
	{
		TableAlias joinTable = getJoinTable();
		TableAlias joinedTable = getJoinedTable();
		if (joinTable == null || joinedTable == null) return null;
		JoinColumnsDetector detector = new JoinColumnsDetector(connection, joinTable, joinedTable);
		String condition = detector.getJoinCondition();
		if (!condition.isEmpty())
		{
			String currentWord = StringUtil.findWordLeftOfCursor(sql, cursorPos);
			boolean whiteSpaceAtLeft = isWhitespaceAtCursor();

			if (currentWord == null || !currentWord.equalsIgnoreCase("on"))
			{
				condition = "ON " + condition;
			}
			if (!whiteSpaceAtLeft)
			{
				condition = " " + condition;
			}
		}
		return condition;
	}

	private boolean isWhitespaceAtCursor()
	{
		if (cursorPos > 0)
		{
			char c = sql.charAt(cursorPos - 1);
			return Character.isWhitespace(c);
		}
		return false;
	}

	public TableAlias getJoinTable()
	{
		Integer pos = getTableIndexBeforeCursor();
		List<Integer> tableIndex = new ArrayList<Integer>(tablePositions.keySet());
		//Collections.sort(tableIndex);
		int index = tableIndex.indexOf(pos);
		if (index > 0)
		{
			Integer mainPos = tableIndex.get(index - 1);
			return tablePositions.get(mainPos);
		}
		return null;
	}

	public TableAlias getJoinedTable()
	{
		Integer pos = getTableIndexBeforeCursor();
		return tablePositions.get(pos);
	}

	private Integer getTableIndexBeforeCursor()
	{
		List<Integer> tableIndex = new ArrayList<Integer>(tablePositions.keySet());
		//Collections.sort(tableIndex);
		for (int i=0; i < tableIndex.size(); i++)
		{
			if (tableIndex.get(i) > cursorPos && i > 0)
			{
				return tableIndex.get(i - 1);
			}
		}
		return tableIndex.get(tableIndex.size() - 1);
	}

	private void retrieveTablePositions()
	{
		tablePositions = new TreeMap<Integer, TableAlias>();
		SQLLexer lexer = new SQLLexer(sql);
		int bracketCount = 0;

		boolean nextIsTable = false;
		Set<String> joinKeywords = SqlUtil.getJoinKeyWords();

		SQLToken token = lexer.getNextToken(false, false);
		while (token != null)
		{
			String value = token.getContents();
			if ("(".equals(value))
			{
				bracketCount ++;
			}
			else if (")".equals(value))
			{
				bracketCount --;
			}
			else if (bracketCount == 0)
			{
				if (nextIsTable)
				{
					TableAlias tbl = null;
					SQLToken next = lexer.getNextToken(false, false);
					boolean useNext = false;
					if (next != null)
					{
						if (next.getText().equalsIgnoreCase("AS"))
						{
							next = lexer.getNextToken(false, false);
						}

						if (!keywords.contains(next.getText()))
						{
							tbl = new TableAlias(value + " " + next.getContents());
						}
					}

					if (tbl == null)
					{
						tbl = new TableAlias(value);
						useNext = true;
					}
					tablePositions.put(token.getCharBegin(), tbl);
					nextIsTable = false;
					if (useNext)
					{
						token = next;
						continue;
					}
				}
				else if(value.equals("FROM"))
				{
					nextIsTable = true;
				}
				else if (joinKeywords.contains(value))
				{
					nextIsTable = true;
				}
				else
				{
					nextIsTable = false;
				}
			}
			token = lexer.getNextToken(false, false);
		}
	}
}
