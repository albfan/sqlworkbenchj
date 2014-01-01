/*
 * JoinColumnsDetector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private TableAlias joinTable;
	private TableAlias joinedTable;
	private WbConnection connection;

	public JoinColumnsDetector(WbConnection dbConnection, TableAlias mainTable, TableAlias childTable)
	{
		this.joinTable = mainTable;
		this.joinedTable = childTable;
		this.connection = dbConnection;
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
		Map<String, String> columns = getJoinColumns();
		if (columns.isEmpty()) return "";

		String and = Settings.getInstance().getFormatterUpperCaseKeywords() ? " AND " : " and ";
		StringBuilder result = new StringBuilder(20);
		boolean first = true;
		for (Map.Entry<String, String> entry : columns.entrySet())
		{
			if (!first)
			{
				result.append(and);
			}
			result.append(entry.getKey());
			result.append(" = ");
			result.append(entry.getValue());
			first = false;
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
		Map<String, String> columns = new HashMap<String, String>(2);
		List<DependencyNode> refTables = connection.getObjectCache().getReferencedTables(table2);

		for (DependencyNode node : refTables)
		{
			if (node.getTable().equals(table1))
			{
				Map<String, String> colMap = node.getColumns();
				for (Map.Entry<String, String> entry : colMap.entrySet())
				{
					String pkColumnExpr = alias1.getNameToUse() + "." +  getColumnName(entry.getKey());
					String fkColExpr = alias2.getNameToUse() + "." + getColumnName(entry.getValue());
					columns.put(pkColumnExpr, fkColExpr);
				}
			}
		}
		return columns;
	}

	private String getColumnName(String colname)
	{
		if (colname == null) return colname;
		String result;

		result = connection.getMetadata().quoteObjectname(colname);

		if (connection.getMetadata().isQuoted(result)) return result;

		String pasteCase = Settings.getInstance().getAutoCompletionPasteCase();
		if ("lower".equalsIgnoreCase(pasteCase))
		{
			result = colname.toLowerCase();
		}
		else if ("upper".equalsIgnoreCase(pasteCase))
		{
			result = colname.toUpperCase();
		}
		else
		{
			result = colname;
		}
		return result;
	}
}
