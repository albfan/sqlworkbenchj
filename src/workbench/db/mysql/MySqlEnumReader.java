/*
 * MySqlEnumReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mysql;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to retrieve enum definitions from a MySQL database.
 * <br/>
 * The method updateEnumDefinition() can be used to post-process a TableDefinition
 *
 * @author  Thomas Kellerer
 * @see workbench.db.DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
 */
public class MySqlEnumReader
	implements ColumnDefinitionEnhancer
{

	public void updateColumnDefinition(TableDefinition tbl, WbConnection connection)
	{
		if (!hasEnums(tbl)) return;
		
		Statement stmt = null;
		ResultSet rs = null;
		HashMap<String, String> defs = new HashMap<String, String>(17);

		try
		{
			stmt = connection.createStatement();
			rs = stmt.executeQuery("SHOW COLUMNS FROM " + tbl.getTable().getTableExpression(connection));
			while (rs.next())
			{
				String column = rs.getString(1);
				if (column == null)	continue;

				String type = rs.getString(2);
				if (type == null)	continue;

				String ltype = type.toLowerCase();
				if (ltype.startsWith("enum") || ltype.startsWith("set"))
				{
					defs.put(column, type);
				}
			}

			List<ColumnIdentifier> columns = tbl.getColumns();
			for (ColumnIdentifier col : columns)
			{
				String name = col.getColumnName();
				String type = defs.get(name);
				if (type != null)
				{
					col.setDbmsType(type);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("EnumReader.updateEnumDefinition()", "Could not read enum definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private boolean hasEnums(TableDefinition tbl)
	{
		for (ColumnIdentifier col : tbl.getColumns())
		{
			String typeName = col.getDbmsType();
			if (typeName.toLowerCase().startsWith("enum") || typeName.toLowerCase().startsWith("set"))
			{
				return true;
			}
		}
		return false;
	}
}
