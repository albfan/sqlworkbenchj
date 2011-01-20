/*
 * JoinColumnsDetector.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.fksupport;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.db.FKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.storage.DataStore;
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

		StringBuilder result = new StringBuilder(20);
		boolean first = true;
		for (Map.Entry<String, String> entry : columns.entrySet())
		{
			if (!first)
			{
				result.append(" AND ");
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
		TableIdentifier realJoinTable = connection.getMetadata().findObject(joinTable.getTable());
		TableIdentifier realJoinedTable = connection.getMetadata().findObject(joinedTable.getTable());

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
		FKHandler fkHandler = new FKHandler(connection);
		DataStore ds = fkHandler.getImportedKeys(table2);
		int count = ds.getRowCount();
		for (int row = 0; row < count; row ++)
		{
			// see DatabaseMetaData.getExportedKeys() for column index description
			String pkTableCat = ds.getValueAsString(row, 0);
			String pkTableSchema = ds.getValueAsString(row, 1);
			String pkTableName = ds.getValueAsString(row, 2);
			TableIdentifier pkTable = new TableIdentifier(pkTableCat, pkTableSchema, pkTableName);

			if (pkTable.equals(table1))
			{
				String pkColName = ds.getValueAsString(row, 3);
				String pkColumnExpr = alias1.getNameToUse() + "." + getColumnName(pkColName);

				String fkColName = ds.getValueAsString(row, 7);
				String fkColExpr = alias2.getNameToUse() + "." + getColumnName(fkColName);
				columns.put(pkColumnExpr, fkColExpr);
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
