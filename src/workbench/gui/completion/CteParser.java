/*
 * CteParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SelectColumn;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CteParser
{

	private String baseSql;
	private int baseSqlStart;
	private List<CteDefinition> cteList = new ArrayList<CteDefinition>();

	public CteParser(String sql)
	{
		parseCte(sql);
	}

	public String getBaseSql()
	{
		return baseSql;
	}

	public int getBaseSqlStart()
	{
		return baseSqlStart;
	}

	/**
	 * Split the passed SQL into individual CTE definitions.
	 *
	 * @param sql  the sql to parse
	 * @return a list that contains one string for each CTE
	 */
	public List<CteDefinition> getCteDefinitions()
	{
		return cteList;
	}

	private void parseCte(String sql)
	{
		SQLLexer lexer = new SQLLexer(sql);

		int lastDefStart = 0;
		int lastStart = 0;
		int bracketCount = 0;
		String currentName = null;

		SQLToken token = lexer.getNextToken(false, false);
		boolean nextIsName = false;
		int colDefStart = -1;
		boolean afterAs = false;
		String colDefs = null;

		while (token != null)
		{
			String s = token.getText();
			if (nextIsName)
			{
				 if (!"recursive".equalsIgnoreCase(s))
				 {
					 currentName = s;
					 nextIsName = false;
				 }
			}
			if ("as".equalsIgnoreCase(s))
			{
				afterAs = true;
			}
			else if ("with".equalsIgnoreCase(s))
			{
				nextIsName = true;
			}
			else if (",".equals(s) && bracketCount == 0)
			{
				nextIsName = true;
				lastDefStart = token.getCharEnd();
			}
			else if ("(".equals(s))
			{
				if (bracketCount == 0 && afterAs)
				{
					lastStart = token.getCharEnd();
					colDefStart = -1;
				}
				else if (bracketCount == 0 && !afterAs)
				{
					colDefStart = token.getCharEnd();
				}
				bracketCount ++;
			}
			else if (")".equals(s))
			{
				bracketCount --;
				if (bracketCount == 0 && colDefStart > -1)
				{
					int end = token.getCharBegin();
					colDefs = sql.substring(colDefStart, end);
				}
				else if (bracketCount == 0 & colDefStart == -1)
				{
					int end = token.getCharBegin();
					String cte = sql.substring(lastStart, end);
					List<ColumnIdentifier> cols;
					if (colDefs != null)
					{
						cols = getColumnsFromDefinition(colDefs);
					}
					else
					{
						cols = getColumnsFromSelect(cte);
					}
					CteDefinition def = new CteDefinition(currentName, cols);
					def.setStartInStatement(lastStart);
					def.setEndInStatement(end);
					def.setInnerSql(cte);
					cteList.add(def);
					afterAs = false;
					colDefs = null;
				}
			}
			else if (bracketCount == 0 && "select".equalsIgnoreCase(s))
			{
				// this is the actual SELECT statement for the CTE
				// (I don't think anything else than a SELECT can "terminate" the list of CTE definitions)
				baseSql = sql.substring(token.getCharBegin());
				baseSqlStart = token.getCharBegin();
			}
			token = lexer.getNextToken(false, false);
		}
	}

	private List<ColumnIdentifier> getColumnsFromDefinition(String columnList)
	{
		List<String> cl = StringUtil.stringToList(columnList, ",", true, true, false, true);
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(cl.size());
		for (String col : cl)
		{
			SelectColumn sc = new SelectColumn(col);
			cols.add(new ColumnIdentifier(sc.getNameToUse()));
		}
		return cols;
	}

	private List<ColumnIdentifier> getColumnsFromSelect(String select)
	{
		List<String> cl = SqlUtil.getSelectColumns(select, true);
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(cl.size());

		for (String col : cl)
		{
			if (StringUtil.isNonBlank(col))
			{
				SelectColumn sc = new SelectColumn(col);
				cols.add(new ColumnIdentifier(sc.getNameToUse()));
			}
		}
		return cols;
	}
}
