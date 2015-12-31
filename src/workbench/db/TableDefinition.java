/*
 * TableDefinition.java
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
