/*
 * InsertColumnMatcher
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
import java.util.Collections;
import java.util.List;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
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

	public InsertColumnMatcher(String sql)
	{
		analyzeStatement(sql);
	}

	public boolean isInsertStatement()
	{
		return isInsert;
	}

	private void analyzeStatement(String sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken token = lexer.getNextToken(false, false);
			if (!token.getContents().equals("INSERT"))
			{
				isInsert = false;
				columns = Collections.emptyList();
				return;
			}

			boolean afterValues = false;

			List<ElementInfo> columnEntries = null;
			List<ElementInfo> valueEntries = null;

			int bracketCount = 0;
			boolean isSubSelect = false;

			while (token != null)
			{
				if (token.getContents().equals(")"))
				{
					bracketCount --;
					if (bracketCount == 0 && afterValues)
					{
						break;
					}
				}
				else if (token.getContents().equals("(") && !afterValues)
				{
					bracketCount ++;
					if (bracketCount == 1)
					{
						columnEntries = getListValues(lexer, token.getCharEnd(), sql);
						// getListValues returned when hitting a ) so the bracket has been closed
						bracketCount --;
					}
				}
				else if (token.getContents().equals("(") && afterValues)
				{
					bracketCount ++;
					if (bracketCount == 1)
					{
						valueEntries = getListValues(lexer, token.getCharEnd(), sql);
						bracketCount --;
					}
				}
				else if (token.getContents().equals("VALUES"))
				{
					afterValues = true;
				}
				else if (token.getContents().equals("SELECT"))
				{
					String subSelect = sql.substring(token.getCharBegin());
					valueEntries = SqlUtil.getColumnEntries(subSelect, true);
					for (ElementInfo element : valueEntries)
					{
						element.setOffset(token.getCharBegin());
					}
					isSubSelect = true;
				}
				token = lexer.getNextToken(false, false);
			}

			int maxElements = columnEntries.size() > valueEntries.size() ? columnEntries.size() : valueEntries.size();
			columns = new ArrayList<InsertColumnInfo>(maxElements);
			for (int i=0; i < maxElements; i++)
			{
				InsertColumnInfo info = new InsertColumnInfo();
				if (i < columnEntries.size())
				{
					info.columnStart = columnEntries.get(i).getStartPosition();
					info.columnEnd = columnEntries.get(i).getEndPosition();
					info.columnName = columnEntries.get(i).getElementValue();
				}
				if (i < valueEntries.size())
				{
					info.valueStart = valueEntries.get(i).getStartPosition();
					info.valueEnd = valueEntries.get(i).getEndPosition();
					info.value = valueEntries.get(i).getElementValue();
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
			if (next.value != null && next.valueStart > 0 && next.valueEnd > 0)
			{
				next.valueStart = current.valueEnd + 1; // plus 1 because of the comma
			}
		}
	}

	private List<ElementInfo> getListValues(SQLLexer lexer, int lastStart, String sql)
	{
		List<ElementInfo> result = new ArrayList<ElementInfo>();

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
			if (info.valueStart <= position && info.valueEnd >= position)
			{
				return true;
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

		List<String> result = new ArrayList<String>(columns.size());
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
				return info.value;
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
				return info.value;
			}
			if (info.valueStart <= position && info.valueEnd >= position)
			{
				return info.columnName;
			}
		}
		return null;
	}
}

class InsertColumnInfo
{
	int columnStart;
	int columnEnd;
	int valueStart;
	int valueEnd;
	String columnName;
	String value;
	boolean valueIsColumn;
}