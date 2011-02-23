/*
 * ResultColumnMetaData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;
import workbench.util.SelectColumn;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 * A class to retrieve additional column meta data for result (query)
 * columns from a datastore.
 *
 * Currently this only retrieves the remarks for queries based on a single
 * table select statement
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaData
{

	private List<String> tables;
	private List<String> columns;
	private WbConnection connection;

	public ResultColumnMetaData(DataStore ds)
	{
		this(ds.getGeneratingSql(), ds.getOriginalConnection());
	}

	public ResultColumnMetaData(String sql, WbConnection conn)
	{
		connection = conn;
		if (StringUtil.isBlank(sql)) return;

		tables = SqlUtil.getTables(sql, true);
		if (CollectionUtil.isEmpty(tables)) return;

		columns = SqlUtil.getSelectColumns(sql, true);
	}

	public void retrieveColumnRemarks(ResultInfo info)
		throws SQLException
	{
		if (CollectionUtil.isEmpty(columns)) return;

		DbMetadata meta = connection.getMetadata();

		Map<String, TableDefinition> tableDefs = new HashMap<String, TableDefinition>(tables.size());
		for (String table : tables)
		{
			TableAlias alias = new TableAlias(table);
			TableIdentifier tbl = alias.getTable();
			tbl.adjustCase(connection);
			TableDefinition def = meta.getTableDefinition(tbl);
			tableDefs.put(alias.getNameToUse().toLowerCase(), def);
		}

		for (String col : columns)
		{
			SelectColumn c = new SelectColumn(col);
			String table = c.getColumnTable();
			if (table == null)
			{
				TableAlias alias = new TableAlias(tables.get(0));
				table = alias.getNameToUse();
			}
			if (table == null) continue;

			TableDefinition def = tableDefs.get(table.toLowerCase());
			if (c.getObjectName().equals("*"))
			{
				processTableColumns(def, info);
			}
			else if (def != null)
			{
				ColumnIdentifier id = def.findColumn(c.getObjectName());
				setColumnComment(def, id, info);
			}
		}
	}

	private void processTableColumns(TableDefinition def, ResultInfo info)
	{
		for (ColumnIdentifier col : def.getColumns())
		{
			setColumnComment(def, col, info);
		}
	}

	private void setColumnComment(TableDefinition def, ColumnIdentifier col, ResultInfo info)
	{
		if (def == null) return;
		if (col == null) return;

		int index = info.findColumn(col.getColumnName());
		if (index > -1)
		{
			info.getColumn(index).setComment(col.getComment());
			info.getColumn(index).setSourceTableName(def.getTable().getTableName());
		}
	}
}
