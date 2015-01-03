/*
 * SelectAnalyzer.java
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.formatter.SqlFormatter;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.Alias;
import workbench.util.SqlParsingUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectAnalyzer
	extends BaseAnalyzer
{
	private final int NO_JOIN_ON = 0;
	private final int JOIN_ON_TABLE_LIST = 1;
	private final int JOIN_ON_COLUMN_LIST = 2;

	public SelectAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void checkContext()
	{
		this.context = NO_CONTEXT;

		String currentWord = getCurrentWord();

		setAppendDot(false);
		setColumnPrefix(null);

		SqlParsingUtil util = SqlParsingUtil.getInstance(dbConnection);

		int fromPos = util.getFromPosition(this.sql);
		int wherePos = -1;
		int joinPos = -1;
		if (fromPos > 0)
		{
			wherePos = util.getWherePosition(sql);
			joinPos = util.getJoinPosition(sql);
		}

		int groupPos = util.getKeywordPosition("GROUP BY", sql);
		int havingPos = util.getKeywordPosition("HAVING", sql);
		int orderPos = util.getKeywordPosition("ORDER BY", sql);

		// find the tables from the FROM clause
		List<Alias> tables = SqlUtil.getTables(sql, true, dbConnection);

		boolean afterWhere = (wherePos > 0 && cursorPos > wherePos);
		boolean afterGroup = (groupPos > 0 && cursorPos > groupPos);
		boolean afterOrder = (orderPos > 0 && cursorPos > orderPos);

		if (havingPos > -1 && afterGroup)
		{
			afterGroup = (cursorPos < havingPos);
		}

		if (orderPos > -1 && afterGroup)
		{
			afterGroup = (cursorPos < orderPos);
		}

		boolean afterHaving = (havingPos > 0 && cursorPos > havingPos);
		if (orderPos > -1 && afterHaving)
		{
			afterHaving = (cursorPos < orderPos);
		}

		boolean inTableList = between(cursorPos, fromPos, joinPos) || between(cursorPos, fromPos, wherePos);
		boolean inWhere =  between(cursorPos, wherePos, orderPos) || between(cursorPos, wherePos, groupPos) || between(cursorPos, wherePos, havingPos);

		if (inTableList)
		{
			if (inWhere || afterGroup || afterOrder) inTableList = false;
		}

		if (joinPos > 0 && inTableList)
		{
			int joinState = inJoinONPart();
			if (joinState == JOIN_ON_COLUMN_LIST)
			{
				inTableList = false;
			}
		}

		if (inTableList)
		{
			String q = getQualifierLeftOfCursor();
			if (q != null)
			{
				setOverwriteCurrentWord(true);//!this.dbConnection.getMetadata().isKeyword(q));
			}

			// If no FROM is present but there is a word with a dot
			// at the cursor position we will first try to use that
			// as a table name (because usually you type the table name
			// first in the SELECT list. If no columns for that
			// name are found, BaseAnalyzer will try to use that as a
			// schema name.
			if (fromPos < 0 && q != null)
			{
				context = CONTEXT_TABLE_OR_COLUMN_LIST;
				this.tableForColumnList = new TableIdentifier(q, dbConnection);
			}
			else
			{
				context = CONTEXT_TABLE_LIST;
			}

			this.schemaForTableList = getSchemaFromCurrentWord();
		}
		else
		{
			context = CONTEXT_COLUMN_LIST;
			// current cursor position is after the WHERE
			// statement or before the FROM statement, so
			// we'll try to find a proper column list

			int count = tables.size();
			this.tableForColumnList = null;

			if (afterGroup)
			{
				this.elements = getColumnsForGroupBy();
				this.addAllMarker = true;
				this.title = ResourceMgr.getString("TxtTitleColumns");
				return;
			}

			if (afterHaving)
			{
				this.elements = getColumnsForHaving();
				this.addAllMarker = false;
				this.title = ResourceMgr.getString("TxtTitleGroupFuncs");
				return;
			}

			this.addAllMarker = !afterWhere;
			char catalogSep = SqlUtil.getCatalogSeparator(dbConnection);
			char schemaSep = SqlUtil.getSchemaSeparator(dbConnection);

			// check if the current qualifier is either one of the
			// tables in the table list or one of the aliases used
			// in the table list.
			TableAlias currentAlias = null;
			String table = null;
			if (currentWord != null)
			{
				int pos = currentWord.indexOf(catalogSep);
				if (pos == -1)
				{
					pos = currentWord.indexOf(schemaSep);
				}

				if (pos > -1)
				{
					table = currentWord.substring(0, pos);
				}
			}

			if (table != null)
			{
				currentAlias = findAlias(table, tables, catalogSep, schemaSep);

				if (currentAlias != null)
				{
					tableForColumnList = currentAlias.getTable();
				}
				else if (this.parentAnalyzer != null)
				{
					// if we didn't find the alias in the current SELECT's
					// tables we check the "parent" statement as we might be inside
					// a sub-select
					List<TableAlias> outerTables = this.parentAnalyzer.getTables();
					if (outerTables != null)
					{
						for (TableAlias outer : outerTables)
						{
							if (outer.isTableOrAlias(table, catalogSep, schemaSep))
							{
								tableForColumnList = outer.getTable();
								currentAlias = outer;
							}
						}
					}
				}
			}
			else if (count == 1)
			{
				TableAlias tbl = new TableAlias(tables.get(0).getObjectName(), catalogSep, schemaSep);
				tableForColumnList = tbl.getTable();
			}

			if (tableForColumnList == null)
			{
				this.context = CONTEXT_FROM_LIST;
				this.addAllMarker = false;
				this.elements = new ArrayList();
				for (Alias entry : tables)
				{
					TableAlias tbl = new TableAlias(entry.getObjectName(), catalogSep, schemaSep);
					this.elements.add(tbl);
					setAppendDot(true);
				}
			}
			else if (currentAlias != null)
			{
				setColumnPrefix(currentAlias.getNameToUse());
			}
		}

		if (inWhere)
		{
			fkMarker = checkFkLookup();
			if (fkMarker != null && elements != null)
			{
				elements.add(fkMarker);
			}
		}
	}

	private TableAlias findAlias(String toSearch, List<Alias> possibleTables, char catalogSeparator, char schemaSeparator)
	{
		for (Alias element : possibleTables)
		{
			TableAlias tbl = new TableAlias(element.getObjectName(), catalogSeparator, schemaSeparator);
			tbl.setAlias(element.getAlias());

			if (tbl.isTableOrAlias(toSearch, catalogSeparator, schemaSeparator))
			{
				return tbl;
			}
		}
		return null;
	}

	private int inJoinONPart()
	{
		int result = NO_JOIN_ON;
		try
		{
			boolean afterFrom = false;

			SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, this.sql);
			SQLToken token = lexer.getNextToken(false, false);
			SQLToken lastToken = null;
			while (token != null)
			{
				String t = token.getContents();
				if (afterFrom)
				{
					if ("ON".equals(t) || "USING".equals(t))
					{
						if (cursorPos >= token.getCharEnd()) result = JOIN_ON_COLUMN_LIST;
					}
					else if (SqlUtil.getJoinKeyWords().contains(t))
					{
						if (lastToken != null && cursorPos > lastToken.getCharEnd() && cursorPos <= token.getCharBegin() &&
							SqlUtil.getJoinKeyWords().contains(lastToken.getContents()))
						{
							result = JOIN_ON_TABLE_LIST;
						}
						else if (cursorPos > token.getCharEnd())
						{
							result = JOIN_ON_TABLE_LIST;
						}
						else
						{
							result = NO_JOIN_ON;
						}
					}
					else if (SqlFormatter.FROM_TERMINAL.contains(t))
					{
						return result;
					}
				}
				else
				{
					if (SqlFormatter.FROM_TERMINAL.contains(t)) break;
					if (t.equals("FROM"))
					{
						if (cursorPos < token.getCharBegin()) return NO_JOIN_ON;
						afterFrom = true;
						result = JOIN_ON_TABLE_LIST;
					}
				}
				lastToken = token;
				token = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SelectAnalyzer.inJoinONPart()", "Error parsing SQL Statement!", e);
		}
		return result;
	}

	private List getColumnsForHaving()
	{
		List<String> cols = SqlUtil.getSelectColumns(this.sql, false, dbConnection);
		List<String> validCols = new LinkedList<>();
		for (String col : cols)
		{
			if (col.indexOf('(') > -1 && col.indexOf(')') > -1)
			{
				validCols.add(col);
			}
		}
		return validCols;
	}

	private List getColumnsForGroupBy()
	{
		List<String> cols = SqlUtil.getSelectColumns(this.sql, false, dbConnection);
		List<String> validCols = new LinkedList<>();
		String[] funcs = new String[]{"sum", "count", "avg", "min", "max" };
		StringBuilder regex = new StringBuilder(50);
		for (int i = 0; i < funcs.length; i++)
		{
			if (i > 0) regex.append('|');
			regex.append("\\s*");
			regex.append(funcs[i]);
			regex.append("\\s*\\(");
		}
		Pattern aggregate = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
		for (String col : cols)
		{
			if (StringUtil.findPattern(aggregate, col, 0) == -1)
			{
				validCols.add(col);
			}
		}
		return validCols;
	}

	/**
	 * This will only return any tables in the FROM clause to
	 * support correlated sub-queries
	 */
	@Override
	public List<TableAlias> getTables()
	{
		List<Alias> tables = SqlUtil.getTables(sql, true, dbConnection);
		List<TableAlias> result = new ArrayList<>(tables.size());
		for (Alias s : tables)
		{
			TableAlias tbl = new TableAlias(s.getObjectName());
			tbl.setAlias(s.getAlias());
			result.add(tbl);
		}
		return result;
	}
}
