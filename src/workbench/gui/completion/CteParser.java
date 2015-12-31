/*
 * CteParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

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
	private List<CteDefinition> cteList = new ArrayList<>();

	CteParser(String sql)
	{
		parseCte(null, sql);
	}

	public CteParser(WbConnection con, String sql)
	{
		parseCte(con, sql);
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
	 * @return a list that contains one string for each CTE
	 */
	public List<CteDefinition> getCteDefinitions()
	{
		return cteList;
	}

	private void parseCte(WbConnection con, String sql)
	{
		SQLLexer lexer = SQLLexerFactory.createLexer(con, sql);

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
						cols = getColumnsFromSelect(cte, con);
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
		List<ColumnIdentifier> cols = new ArrayList<>(cl.size());
		int colIndex = 1;
		for (String col : cl)
		{
			SelectColumn sc = new SelectColumn(col);
			ColumnIdentifier ci = new ColumnIdentifier(sc.getNameToUse());
			ci.setPosition(colIndex);
			cols.add(ci);
			colIndex ++;
		}
		return cols;
	}

	private List<ColumnIdentifier> getColumnsFromSelect(String select, WbConnection conn)
	{
		List<String> cl = SqlUtil.getSelectColumns(select, true, conn);
		List<ColumnIdentifier> cols = new ArrayList<>(cl.size());

		int colIndex = 1;
		for (String col : cl)
		{
			if (StringUtil.isNonBlank(col))
			{
				SelectColumn sc = new SelectColumn(col);
				ColumnIdentifier ci = new ColumnIdentifier(sc.getNameToUse());
				ci.setPosition(colIndex);
				cols.add(ci);
				colIndex ++;
			}
		}
		return cols;
	}
}
