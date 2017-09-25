/*
 * JoinColumnsDetector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.Settings;

import workbench.db.DependencyNode;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.TableAlias;

/**
 *
 * @author Thomas Kellerer
 */
public class JoinColumnsDetector
{
	private final TableAlias joinTable;
	private final TableAlias joinedTable;
	private final WbConnection connection;
	private boolean preferUsingOperator;
	private boolean matchingColumnsAvailable;
	private GeneratedIdentifierCase keywordCase;
	private GeneratedIdentifierCase identifierCase;

	public JoinColumnsDetector(WbConnection dbConnection, TableAlias mainTable, TableAlias childTable)
	{
		this.joinTable = mainTable;
		this.joinedTable = childTable;
		this.connection = dbConnection;
		this.preferUsingOperator = Settings.getInstance().getJoinCompletionPreferUSING();
		this.identifierCase = Settings.getInstance().getFormatterIdentifierCase();
		this.keywordCase =  Settings.getInstance().getFormatterKeywordsCase();
	}

	public void setPreferUsingOperator(boolean flag)
	{
		this.preferUsingOperator = flag;
	}

	public void setKeywordCase(GeneratedIdentifierCase kwCase)
	{
		this.keywordCase = kwCase;
	}

	public void setIdentifierCase(GeneratedIdentifierCase idCase)
	{
		this.identifierCase = idCase;
	}

	/**
	 * Return the condition to be used in a JOIN or WHERE clause to join the two tables.
	 * <br/>
 *
	 * @return the join condition to be used (null if any table was not found)
	 * @throws SQLException
	 */
	public String getJoinCondition()
		throws SQLException
	{
		matchingColumnsAvailable = false;
		Map<String, String> columns = getJoinColumns();
		if (columns.isEmpty()) return "";

		String delim = null;
		boolean useUsingOperator = preferUsingOperator && matchingColumnsAvailable;
		if (useUsingOperator)
		{
			delim = ",";
		}
		else
		{
			delim = keywordCase == GeneratedIdentifierCase.upper ? " AND " : " and ";
		}
		StringBuilder result = new StringBuilder(20);
		boolean first = true;
		if (useUsingOperator)
		{
			result.append('(');
		}
		for (Map.Entry<String, String> entry : columns.entrySet())
		{
			if (!first)
			{
				result.append(delim);
			}
			result.append(entry.getKey());
			if (!useUsingOperator)
			{
				result.append(" = ");
				result.append(entry.getValue());
			}
			first = false;
		}
		if (useUsingOperator)
		{
			result.append(')');
		}
		return result.toString();
	}

	/**
	 * Return a map for columns to be joined.
	 * <br/>
	 * The key will be the PK column, the value the FK column. If either the joinTable or the joined table
	 * (see constructor) is not found, an empty map is returned;
	 *
	 * @return the mapping for the PK/FK columns
	 * @throws SQLException
	 */
	private Map<String, String> getJoinColumns()
		throws SQLException
	{
		TableIdentifier realJoinTable = connection.getObjectCache().getOrRetrieveTable(joinTable.getTable());
		TableIdentifier realJoinedTable = connection.getObjectCache().getOrRetrieveTable(joinedTable.getTable());

		if (realJoinTable == null || realJoinedTable == null)
		{
			return Collections.emptyMap();
		}

		Map<String, String> columns = getJoinColumns(realJoinTable, joinTable, realJoinedTable, joinedTable);
		if (columns.isEmpty())
		{
			columns = getJoinColumns(realJoinedTable, joinedTable, realJoinTable, joinTable);
		}
		return columns;
	}

	private Map<String, String> getJoinColumns(TableIdentifier table1, TableAlias alias1, TableIdentifier table2, TableAlias alias2)
		throws SQLException
	{
		Map<String, String> columns = new HashMap<>(2);
		List<DependencyNode> refTables = connection.getObjectCache().getReferencedTables(table2);

		for (DependencyNode node : refTables)
		{
			if (node.getTable().equals(table1))
			{
				Map<String, String> colMap = node.getColumns();
				int matchingCols = 0;
				for (Map.Entry<String, String> entry : colMap.entrySet())
				{
					if (entry.getKey().equalsIgnoreCase(entry.getValue()))
					{
						matchingCols ++;
					}
				}
				this.matchingColumnsAvailable = matchingCols == colMap.size();
				for (Map.Entry<String, String> entry : colMap.entrySet())
				{
					if (matchingColumnsAvailable && preferUsingOperator)
					{
						String col = getColumnName(entry.getKey());
						columns.put(col, col);
					}
					else
					{
						String pkColumnExpr = alias1.getNameToUse() + "." + getColumnName(entry.getKey());
						String fkColExpr = alias2.getNameToUse() + "." + getColumnName(entry.getValue());
						columns.put(pkColumnExpr, fkColExpr);
					}
				}
			}
		}
		return columns;
	}

	private String getColumnName(String colname)
	{
		if (colname == null) return colname;

		String result= connection.getMetadata().quoteObjectname(colname);

		if (connection.getMetadata().isQuoted(result)) return result;

		switch (identifierCase)
		{
			case lower:
				result = colname.toLowerCase();
				break;
			case upper:
				result = colname.toUpperCase();
				break;
			default:
				result = colname;
		}
		return result;
	}
}
