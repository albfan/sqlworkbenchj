/*
 * InsertColumnMatcher.java
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.ElementInfo;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class InsertColumnMatcher
{
	private List<InsertColumnInfo> columns;
	private boolean isInsert;
	private String noValue = "<" + ResourceMgr.getString("TxtNoValue") + ">";


	public InsertColumnMatcher(WbConnection conn, String sql)
	{
		analyzeStatement(conn, sql);
	}

	InsertColumnMatcher(String sql)
	{
		analyzeStatement(null, sql);
	}

	public boolean isInsertStatement()
	{
		return isInsert;
	}

	private void analyzeStatement(WbConnection conn, String sql)
	{
		try
		{
			Set<String> verbs = CollectionUtil.caseInsensitiveSet("INSERT", "MERGE");

			SQLLexer lexer = SQLLexerFactory.createLexer(conn, sql);
			SQLToken token = lexer.getNextToken(false, false);

			if (token == null || !verbs.contains(token.getContents()))
			{
				isInsert = false;
				columns = Collections.emptyList();
				return;
			}

			boolean afterValues = false;

			if (token.getContents().equals("MERGE"))
			{
				// "fast forward" to the actual INSERT part
				// so that the following code does not need to handle any "noise" before the actual insert
				while (token != null)
				{
					if (token.getContents().equals("INSERT")) break;
					token = lexer.getNextToken(false, false);
				}
			}

			List<ElementInfo> columnEntries = null;
			List<List<ElementInfo>> rowValues = new ArrayList<>(1);

			int bracketCount = 0;
			boolean isSubSelect = false;

			while (token != null)
			{
				String text = token.getContents();
				if (token.getContents().equals(")"))
				{
					bracketCount --;
				}
				else if (token.getContents().equals("(") && !afterValues)
				{
					bracketCount ++;
					if (bracketCount == 1)
					{
						columnEntries = getListValues(lexer, token.getCharEnd(), sql);
						// getListValues() terminated at the closing bracket
						// so the bracket count must be decreased here again.
						bracketCount --;
					}
				}
				else if (token.getContents().equals("(") && afterValues)
				{
					bracketCount ++;
					if (bracketCount == 1)
					{
						rowValues.add(getListValues(lexer, token.getCharEnd(), sql));
						bracketCount --;
					}
				}
				else if (token.getContents().equals("VALUES"))
				{
					afterValues = true;
				}
				else if (text.equals("SELECT") || text.equals("WITH"))
				{
					String subSelect = sql.substring(token.getCharBegin());
					List<ElementInfo> entries = SqlUtil.getColumnEntries(subSelect, true, conn);
					for (ElementInfo element : entries)
					{
						element.setOffset(token.getCharBegin());
					}
					rowValues.add(entries);
					isSubSelect = true;
					break; // no need to go any further, it's an INSERT ... SELECT statement
				}
				token = lexer.getNextToken(false, false);
			}

			int maxValues = -1;
			for (List<ElementInfo> values : rowValues)
			{
				if (values.size() > maxValues)
				{
					maxValues = values.size();
				}
			}

			int maxElements = columnEntries.size() > maxValues ? columnEntries.size() : maxValues;
			columns = new ArrayList<>(maxElements);
			for (int i=0; i < maxElements; i++)
			{
				InsertColumnInfo info = new InsertColumnInfo();
				if (i < columnEntries.size())
				{
					info.columnStart = columnEntries.get(i).getStartPosition();
					info.columnEnd = columnEntries.get(i).getEndPosition();
					info.columnName = columnEntries.get(i).getElementValue();
				}
				else
				{
					info.columnStart = -1;
					info.columnEnd = -1;
					info.columnName = null;
				}
				for (List<ElementInfo> entries : rowValues)
				{
					if (i < entries.size())
					{
						info.addValue(entries.get(i).getElementValue(), entries.get(i).getStartPosition(), entries.get(i).getEndPosition());
					}
					else
					{
						info.addValue(null, -1, -1);
					}
				}
				columns.add(info);
			}
			if (isSubSelect)
			{
				adjustValueStarts();
			}
		}
		catch (Exception e)
		{
			if (columns == null)
			{
				columns = Collections.emptyList();
			}
			LogMgr.logError("InsertColumnMatcher.analyzeStatemet()", "Could not analyze statement: " + sql, e);
		}
	}

	/**
	 * The start position of sub-select columns does not consider whitespace before the column name.
	 * It is always the start of the actual column name. This is not correct
	 * for tooltip hinting (as the "start of the column is right behind the previous comma),
	 * so we need to adjust those values.
	 */
	private void adjustValueStarts()
	{
		for (int i=0; i < columns.size() - 1; i++)
		{
			InsertColumnInfo current = columns.get(i);
			InsertColumnInfo next = columns.get(i + 1);
			for (int v=0; v < next.values.size(); v++)
			{
				if (next.values.get(v).valueStart > 0 && next.values.get(v).valueEnd > 0)
				{
					next.values.get(v).valueStart = current.values.get(v).valueEnd -1;
				}
			}
		}
	}

	private List<ElementInfo> getListValues(SQLLexer lexer, int lastStart, String sql)
	{
		List<ElementInfo> result = new ArrayList<>();

		int bracketCount = 1;
		int lastComma = lastStart;
		SQLToken token = lexer.getNextToken(false, false);
		while (token != null)
		{
			String c = token.getText();
			if (c.equals("("))
			{
				bracketCount ++;
			}
			else if (c.equals(")"))
			{
				bracketCount --;
				if (bracketCount == 0)
				{
					ElementInfo info = new ElementInfo(sql.substring(lastComma, token.getCharBegin()).trim(), lastComma, token.getCharBegin());
					result.add(info);
					break;
				}
			}
			else if (c.equals(",") && bracketCount == 1)
			{
				ElementInfo info = new ElementInfo(sql.substring(lastComma, token.getCharBegin()).trim(), lastComma, token.getCharBegin());
				result.add(info);
				lastComma = token.getCharEnd();
			}
			token = lexer.getNextToken(false, false);
		}
		return result;
	}

	public boolean inValueList(int position)
	{
		for (InsertColumnInfo info : columns)
		{
			for (ColumnValueInfo valInfo : info.values)
			{
				if (valInfo.valueStart <= position && valInfo.valueEnd >= position)
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean inColumnList(int position)
	{
		for (InsertColumnInfo info : columns)
		{
			if (info.columnStart <= position && info.columnEnd >= position)
			{
				return true;
			}
		}
		return false;
	}


	public List<String> getColumns()
	{
		if (columns == null) return Collections.emptyList();

		List<String> result = new ArrayList<>(columns.size());
		for (InsertColumnInfo column : columns)
		{
			result.add(column.columnName);
		}
		return result;
	}

	/**
	 * Return the value for the given column name.
	 * @param columnName the column to search for
	 * @return the value or null if the column was not found
	 */
	public String getValueForColumn(String columnName)
	{
		if (StringUtil.isBlank(columnName)) return null;

		for (InsertColumnInfo info : columns)
		{
			if (StringUtil.equalStringIgnoreCase(columnName, info.columnName))
			{
				return info.getValue();
			}
		}
		return null;
	}

	/**
	 * Return the name of the column at the given position.
	 *
	 * If the cursor is not in the VALUES list, null will be returned.
	 *
	 * @param position the cursor position.
	 * @return the column name or null
	 */
	public String getInsertColumnName(int position)
	{
		for (InsertColumnInfo info : columns)
		{
			for (ColumnValueInfo valInfo : info.values)
			{
				if (valInfo.valueStart <= position && valInfo.valueEnd >= position)
				{
					return info.columnName;
				}
			}
		}
		return null;
	}

	/**
	 * Return the tooltip text for the given cursor position.
	 * If the cursor is in the column list, the tooltip will
	 * contain the column's value. If the cursor is in the
	 * values list, it returns the column's name
	 *
	 * @param position
	 * @return the value or column name. null if position is invalid
	 */
	public String getTooltipForPosition(int position)
	{
		for (InsertColumnInfo info : columns)
		{
			if (info.columnStart <= position && info.columnEnd >= position)
			{
				return info.getValue();
			}
			for (ColumnValueInfo valInfo : info.values)
			{
				if (valInfo.valueStart <= position && valInfo.valueEnd >= position)
				{
					return info.columnName;
				}
			}
		}
		return null;
	}

	private class InsertColumnInfo
	{
		int columnStart;
		int columnEnd;
		String columnName;
		List<ColumnValueInfo> values = new ArrayList<>(1);

		void addValue(String value, int valueStart, int valueEnd)
		{
			values.add(new ColumnValueInfo(value, valueStart, valueEnd));
		}

		String getValue()
		{
			if (values.isEmpty())
			{
				return null;
			}
			if (values.size() == 1)
			{
				return values.get(0).value;
			}
			StringBuilder result = new StringBuilder(values.size() * 5);
			result.append('[');
			for (int i=0; i < values.size(); i++)
			{
				if (i > 0) result.append(", ");
				String value = values.get(i).value;
				if (StringUtil.isBlank(value))
				{
					result.append(noValue);
				}
				else
				{
					result.append(values.get(i).value);
				}
			}
			result.append(']');
			return result.toString();
		}
	}

	private class ColumnValueInfo
	{
		int valueStart;
		int valueEnd;
		String value;

		ColumnValueInfo(String value, int valueStart, int valueEnd)
		{
			this.valueStart = valueStart;
			this.valueEnd = valueEnd;
			this.value = value;
		}

		@Override
		public String toString()
		{
			return value;
		}
	}
}

