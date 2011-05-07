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
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read additional column level information for a table.
 *
 * The following additional information is retrieved:
 * <ul>
 * <li>
 *     column remarks by using stored procedures for accessing "extended properties".
 *     This workaround is necessary because SQL Server does not support real (ANSI SQL) comments and the driver
 *     consequently does not return them either.
 * </li>
 * <li>Definition of computed (virtual) columns </li>
 * <li>Column level collation definition</li>
 * </ul>
 *
 * @author Thomas Kellerer
 */
public class SqlServerColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		if (JdbcUtils.hasMinimumServerVersion(conn, "9.0"))
		{
			updateComputedColumns(table, conn);
		}
		if (Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.remarks.column.retrieve", false))
		{
			updateColumnRemarks(table, conn);
		}
		readCollations(table, conn);
	}

	private void updateColumnRemarks(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = StringUtil.trimQuotes(table.getTable().getTableName());
		String schema = StringUtil.trimQuotes(table.getTable().getSchema());

		String propName = Settings.getInstance().getProperty("workbench.db.microsoft_sql_server.remarks.propertyname", "MS_DESCRIPTION");

		String sql = "SELECT objname, cast(value as varchar) as value \n FROM ";

		if (JdbcUtils.hasMinimumServerVersion(conn, "9.0"))
		{
			sql += "fn_listextendedproperty ('" + propName + "','schema', ?, 'table', ?, 'column', null)";
		}
		else
		{
			// SQL Server 2000 (and probably before) uses a different function name and parameters
			sql += "::fn_listextendedproperty ('" + propName + "','user', ?, 'table', ?, 'column', null)";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("SqlServerColumnEnhancer.updateColumnRemarks()", "Using query=\n" + sql);
		}

		Map<String, String> remarks = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, tablename);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String col = rs.getString(1);
				String remark = rs.getString(2);
				if (col != null && remark != null)
				{
					remarks.put(col.trim(), remark);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlServerColumnEnhancer.updateColumnRemarks()", "Error retrieving remarks", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		for (ColumnIdentifier col : table.getColumns())
		{
			String remark = remarks.get(col.getColumnName());
			col.setComment(remark);
		}
	}

	private void updateComputedColumns(TableDefinition table, WbConnection conn)
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
			LogMgr.logError("SqlServerColumnEnhancer.updateComputedColumns()", "Error retrieving computed columns", e);
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

	public void readCollations(TableDefinition table, WbConnection conn)
	{
		String defaultCollation = null;
		Statement info = null;
		ResultSet rs = null;
		try
		{
			String getDefaultSql = "select databasepropertyex('"+ table.getTable().getCatalog() + "', 'Collation')";
			info = conn.createStatement();
			rs = info.executeQuery(getDefaultSql);
			if (rs.next())
			{
				defaultCollation = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("MySQLColumnEnhancer.readCollations()", "Could not read default collation", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, info);
		}

		PreparedStatement stmt = null;

		HashMap<String, String> collations = new HashMap<String, String>(table.getColumnCount());
		String sql =
			"SELECT column_name, \n" +
			"       collation_name \n" +
			"FROM information_schema.columns \n" +
			"WHERE table_name = ? \n" +
			"  AND table_schema = ? \n " +
			"  AND table_catalog = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerColumnEnhancer.readCollations()", "Using collation SQL=\n" + sql);
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTable().getTableName());
			stmt.setString(2, table.getTable().getSchema());
			stmt.setString(3, table.getTable().getCatalog());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String collation = rs.getString(2);
				if (isNonDefault(collation, defaultCollation))
				{
					collations.put(colname, "COLLATE " + collation);
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("SqlServerColumnEnhancer.readCollations()", "Could not read column collations", ex);
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
				String dataType = col.getDbmsType() + " " + collation;
				col.setDbmsType(dataType);
			}
		}
	}

	private boolean isNonDefault(String value, String defaultValue)
	{
		if (StringUtil.isEmptyString(value)) return false;
		return !value.equals(defaultValue);
	}
}
