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
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve information about computed/generated columns in HSQLDB 2.0
 * 
 * @author Thomas Kellerer
 */
public class HsqlColumnEnhancer
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

		String sql = "select column_name, " +
								"        generation_expression, " +
								"        is_generated, " +
								"        is_identity, " +
								"        identity_generation, " +
								"        identity_start, " +
								"        identity_increment " +
								"from information_schema.columns \n" +
								"where table_name = ? \n" +
								"and table_schema = ? \n" +
								"and (is_generated = 'ALWAYS' OR (is_identity = 'YES' AND identity_generation IS NOT NULL))  \n";

		Map<String, String> expressions = new HashMap<String, String>();
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tablename);
			stmt.setString(2, schema);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String computedExpr = rs.getString(2);
				String isComputed = rs.getString(3);
				String isIdentity = rs.getString(4);
				String identityExpr = rs.getString(5);

				String columnExpression = null;
				if (StringUtil.equalString(isComputed, "ALWAYS"))
				{
					// HSQL apparently stores a fully qualified column name
					// Using that when re-creating the source looks a bit weird, so
					// we'll simply strip off the schema and table name
					int pos = computedExpr.lastIndexOf('.');
					if (pos > 1)
					{
						computedExpr = computedExpr.substring(pos + 1);
					}
					columnExpression = "GENERATED ALWAYS AS (" + computedExpr + ")";
				}
				else if (StringUtil.equalString(isIdentity, "YES"))
				{
					String start = rs.getString(6);
					String inc = rs.getString(7);
					columnExpression = "GENERATED " + identityExpr + " AS IDENTITY";
					if (!StringUtil.equalString("0", start) || !StringUtil.equalString("1", inc))
					{
						columnExpression += " (START WITH " + start + " INCREMENT BY " + inc + ")";
					}
				}
				if (StringUtil.isBlank(columnExpression)) continue;
				expressions.put(colname, columnExpression);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("H2ColumnEnhancer.updateComputedColumns()", "Error retrieving remarks", e);
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
				col.setDefaultValue(null);
				col.setComputedColumnExpression(expr);
			}
		}
	}

}
