/*
 * TableDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDefinition
{
	private TableIdentifier table;
	private List<ColumnIdentifier> columns;

	public TableDefinition(TableIdentifier id)
	{
		this(id, null);
	}

	public TableDefinition(TableIdentifier id, List<ColumnIdentifier> cols)
	{
		table = id;
		columns = cols;
	}

	public TableIdentifier getTable()
	{
		return table;
	}

	public List<ColumnIdentifier> getColumns()
	{
		if (columns == null) return Collections.emptyList();
		return columns;
	}

	public int getColumnCount()
	{
		if (columns == null) return 0;
		return columns.size();
	}

	public ColumnIdentifier findColumn(String colName)
	{
		if (getColumnCount() == 0) return null;
		for (ColumnIdentifier col : columns)
		{
			if (col.getColumnName().equalsIgnoreCase(colName)) return col;
		}
		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(getColumnCount() * 10 + 25);
		result.append(table.toString());
		result.append(" (");
		List<ColumnIdentifier> cols = getColumns();
		for (int i=0; i < cols.size(); i++)
		{
			if (i > 0)
			{
				result.append(", ");
			}
			result.append(cols.get(i).getColumnName());
		}
		result.append(')');
		return result.toString();
	}
}
