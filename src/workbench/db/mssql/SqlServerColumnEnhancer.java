/*
 * FirebirdColumnEnhancer
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;


		String tablename = table.getTable().getTableExpression(conn);

		String sql = "select name, definition, is_persisted \n" +
             "from sys.computed_columns where object_id = object_id('";
		sql += tablename;
		sql += "')";

		Map<String, String> expressions = new HashMap<String, String>();
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String def = rs.getString(2);
				if (def == null) continue;

				def = def.trim();
				boolean isPersisted = rs.getBoolean(3);
				String expr = "AS ";
				if (def.startsWith("("))
				{
					expr += def;
				}
				else
				{
					expr += "(" + def + ")";
				}
				if (isPersisted)
				{
					expr += " PERSISTED";
				}
				expressions.put(colname, expr);
			}
		}
		catch (Exception e)
		{

		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		for (ColumnIdentifier col : table.getColumns())
		{
			String expr = expressions.get(col.getColumnName());
			if (StringUtil.isNonBlank(expr))
			{
				col.setComputedColumnExpression(expr);
			}
		}
	}
	
}
