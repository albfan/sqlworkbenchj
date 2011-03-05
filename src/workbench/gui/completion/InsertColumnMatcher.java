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
		columns = new ArrayList<InsertColumnInfo>();
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
				return;
			}

			boolean valuesList = false;
			boolean afterValues = false;

			List<EntryInfo> columnEntries = null;
			List<EntryInfo> valueEntries = null;

			int bracketCount = 0;
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
				token = lexer.getNextToken(false, false);
			}

			for (int i=0; i < columnEntries.size(); i++)
			{
				InsertColumnInfo info = new InsertColumnInfo();
				info.columnStart = columnEntries.get(i).start;
				info.columnEnd = columnEntries.get(i).end;
				info.columnName = columnEntries.get(i).value;
				if (i < valueEntries.size())
				{
					info.valueStart = valueEntries.get(i).start;
					info.valueEnd = valueEntries.get(i).end;
					info.value = valueEntries.get(i).value;
				}
				columns.add(info);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("InsertColumnMatcher.analyzeStatemet()", "Could not analyze statement: " + sql, e);
		}
	}

	private List<EntryInfo> getListValues(SQLLexer lexer, int lastStart, String sql)
	{
		List<EntryInfo> result = new ArrayList<EntryInfo>();

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
					EntryInfo info = new EntryInfo();
					info.start = lastComma;
					info.end = token.getCharBegin();
					info.value = sql.substring(info.start, info.end).trim();
					result.add(info);
					break;
				}
			}
			else if (c.equals(",") && bracketCount == 1)
			{
				EntryInfo info = new EntryInfo();
				info.start = lastComma;
				info.end = token.getCharBegin();
				info.value = sql.substring(info.start, info.end).trim();
				result.add(info);
				lastComma = token.getCharEnd();
			}
			token = lexer.getNextToken(false, false);
		}
		return result;
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

class EntryInfo
{
	int start;
	int end;
	String value;
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