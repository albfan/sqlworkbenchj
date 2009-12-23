/*
 * TableDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

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
}
