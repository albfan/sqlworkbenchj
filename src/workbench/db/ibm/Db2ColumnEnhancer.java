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
package workbench.db.ibm;

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
 *
 * @author Thomas Kellerer
 */
public class Db2ColumnEnhancer
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

		String sql = "select colname, text  \n" +
             "from syscat.columns \n" +
             "where tabname = ?  \n" +
             "and tabschema = ? \n" +
             "and generated = 'A'";

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
				String def = rs.getString(2);
				if (def == null) continue;

				def = def.trim();
				if (def.toLowerCase().startsWith("as"))
				{
					def = "GENERATED ALWAYS " + def;
				}
				else
				{
					def = "GENERATED ALWAYS AS " + def;
				}
				expressions.put(colname, def);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2ColumnEnhancer.updateComputedColumns()", "Error retrieving remarks", e);
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
