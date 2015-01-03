/*
 * JoinCreator.java
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
package workbench.sql.fksupport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.StatementContext;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
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
	private boolean preferUsingOperator;
	private boolean alwaysUseParenthesis;
	private GeneratedIdentifierCase keywordCase;
	private GeneratedIdentifierCase identifierCase;

	public JoinCreator(String statement, int positionInStatement, WbConnection dbConn)
	{
		this.connection = dbConn;
		String dbid = connection != null ? connection.getDbId() : null;
		SqlKeywordHelper helper = new SqlKeywordHelper(dbid);
		keywords = helper.getKeywords();
		int realPos = setSql(statement, positionInStatement);
		setCursorPosition(realPos);
		preferUsingOperator = Settings.getInstance().getJoinCompletionPreferUSING();
		alwaysUseParenthesis = Settings.getInstance().getJoinCompletionUseParens();
		identifierCase = Settings.getInstance().getFormatterIdentifierCase();
		keywordCase =  Settings.getInstance().getFormatterKeywordsCase();
	}

	public void setKeywordCase(GeneratedIdentifierCase kwCase)
	{
		this.keywordCase = kwCase;
	}

	public void setIdentifierCase(GeneratedIdentifierCase idCase)
	{
		this.identifierCase = idCase;
	}

	public void setAlwaysUseParenthesis(boolean flag)
	{
		alwaysUseParenthesis = flag;
	}

	public void setPreferUsingOperator(boolean flag)
	{
		this.preferUsingOperator = flag;
	}

	public final void setCursorPosition(int position)
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
		return getJoinCondition(joinTable);
	}

	public String getJoinCondition(TableAlias joinTable)
		throws SQLException
	{
		TableAlias joinedTable = getJoinedTable();
		if (joinTable == null || joinedTable == null) return null;

		JoinColumnsDetector detector = new JoinColumnsDetector(connection, joinTable, joinedTable);
		detector.setPreferUsingOperator(preferUsingOperator);
		detector.setKeywordCase(keywordCase);
		detector.setIdentifierCase(identifierCase);

		String condition = detector.getJoinCondition();

		if (condition.isEmpty()) return condition;

		String currentWord = StringUtil.findWordLeftOfCursor(sql, cursorPos);
		boolean whiteSpaceAtLeft = isWhitespaceAtCursor();

		String operator = null;
		boolean useUpperCase = keywordCase == GeneratedIdentifierCase.upper;
		if (condition.indexOf('=') == -1 && preferUsingOperator)
		{
			operator = useUpperCase ? "USING " : "using ";
		}
		else
		{
			operator = useUpperCase ? "ON " : "on ";
		}

		if (alwaysUseParenthesis && !condition.startsWith("("))
		{
			condition = "(" + condition + ")";
		}

		if (currentWord == null || !currentWord.equalsIgnoreCase(operator.trim()))
		{
			condition = operator + condition;
		}

		if (!whiteSpaceAtLeft)
		{
			condition = " " + condition;
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

	public List<TableAlias> getPossibleJoinTables()
	{
		Integer index = getTableIndexBeforeCursor();
		if (index == null) return Collections.emptyList();

		int pos = index.intValue();

		TableAlias firstTable = getJoinTable();

		List<TableAlias> tables = new ArrayList<>(tablePositions.size());

		for (Map.Entry<Integer, TableAlias> entry : tablePositions.entrySet())
		{
			int tableIndex = entry.getKey().intValue();
			if (tableIndex < pos && !entry.getValue().equals(firstTable))
			{
				tables.add(entry.getValue());
			}
		}
		return tables;
	}

	public TableAlias getJoinTable()
	{
		Integer pos = getTableIndexBeforeCursor();
		List<Integer> tableIndex = new ArrayList<>(tablePositions.keySet());
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
		List<Integer> tableIndex = new ArrayList<>(tablePositions.keySet());
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
		tablePositions = new TreeMap<>();
		SQLLexer lexer = SQLLexerFactory.createLexer(connection, sql);
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
