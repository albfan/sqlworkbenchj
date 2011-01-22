/*
 * FirebirdColumnEnhancer
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class H2ColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		updateComputedColumns(table, conn);
	}

	private void updateComputedColumns(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = table.getTable().getTableName();
		String schema = table.getTable().getSchema();

		String sql = "select column_name \n" +
             "from information_schema.columns \n" +
             "where table_name = ? \n" +
             "and table_schema = ? \n" +
             "and is_computed = true \n";

		Set<String> computedCols = CollectionUtil.caseInsensitiveSet();
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tablename);
			stmt.setString(2, schema);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				computedCols.add(colname);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("H2ColumnEnhancer.updateComputedColumns()", "Error retrieving column info", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		for (ColumnIdentifier col : table.getColumns())
		{
			if (computedCols.contains(col.getColumnName()))
			{
				String expr = col.getDefaultValue();
				if (StringUtil.isNonBlank(expr))
				{
					if (!expr.startsWith("AS"))
					{
						expr = "AS " + expr;
					}
					col.setDefaultValue(null);
					col.setComputedColumnExpression(expr);
				}
			}
		}
	}

}
