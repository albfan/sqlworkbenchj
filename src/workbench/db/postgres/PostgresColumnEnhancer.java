/*
 * PostgresColumnEnhancer
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A column enhancer to read the column collation available since PostgreSQL 9.1
 *
 * @author Thomas Kellerer
 */
public class PostgresColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		if (JdbcUtils.hasMinimumServerVersion(conn, "9.1"))
		{
			readCollations(table, conn);
		}
	}

	private void readCollations(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		HashMap<String, String> collations = new HashMap<String, String>(table.getColumnCount());
		String sql =
			"select att.attname, col.collcollate \n" +
			"from pg_attribute att \n" +
			"  join pg_class tbl on tbl.oid = att.attrelid  \n" +
			"  join pg_namespace ns on tbl.relnamespace = ns.oid  \n" +
			"  join pg_collation col on att.attcollation = col.oid \n" +
			"where tbl.relname = ?   \n" +
			"  and ns.nspname = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresColumnEnhancer.readCollations()", "PostgresColumnEnhancer using SQL=\n" + sql);
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTable().getTableName());
			stmt.setString(2, table.getTable().getSchema());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String collation = rs.getString(2);
				if (StringUtil.isNonEmpty(collation))
				{
					collations.put(colname, collation);
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("PostgresColumnEnhancer.readCollations()", "Could not read column collations", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		for (ColumnIdentifier col : table.getColumns())
		{
			String collation = collations.get(col.getColumnName());
			if (StringUtil.isNonEmpty(collation))
			{
				col.setCollation(collation);
				col.setCollationExpression(" COLLATE \"" + collation + "\"");
			}
		}
	}
}
