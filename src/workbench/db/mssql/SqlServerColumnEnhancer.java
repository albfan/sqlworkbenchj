/*
 * SqlServerColumnEnhancer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

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
		if (SqlServerUtil.isSqlServer2005(conn))
		{
			updateComputedColumns(table, conn);
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.remarks.column.retrieve", true))
		{
			updateColumnRemarks(table, conn);
		}

		if (SqlServerUtil.isSqlServer2005(conn))
		{
			readCollations(table, conn);
		}
	}

	private void updateColumnRemarks(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = SqlUtil.removeObjectQuotes(table.getTable().getTableName());
		String schema = SqlUtil.removeObjectQuotes(table.getTable().getSchema());

		String propName = Settings.getInstance().getSqlServerRemarksProperty();

		// I have to cast to a length specified varchar value otherwise
		// the remarks will be truncated at 31 characters for some strange reason
		// varchar(8000) character should work on any sensible SQL Server version  (varchar(max) doesn't work on SQL Server 2000)
		String sql = "SELECT objname, cast(value as varchar(8000)) as value \nFROM ";

		if (SqlServerUtil.isSqlServer2005(conn))
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
			LogMgr.logInfo("SqlServerColumnEnhancer.updateColumnRemarks()",
				"Retrieving column remarks using query:\n" + SqlUtil.replaceParameters(sql, schema, tablename));
		}

		Map<String, String> remarks = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
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
			LogMgr.logError("SqlServerColumnEnhancer.updateColumnRemarks()", "Error retrieving remarks using:\n" + SqlUtil.replaceParameters(sql, schema, tablename), e);
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
		String tablename;
		String sql;

		if (SqlServerUtil.isSqlServer2005(conn))
		{
			sql =
				"select name, definition, is_persisted \n" +
				"from sys.computed_columns with (nolock) \n" +
				"where object_id = object_id(?)";
			tablename = table.getTable().getTableExpression(conn);
		}
		else
		{
			// this method is only called when the server version is 2000 or later
			// so this else part means the server is a SQL Server 2000
			sql =
				"select c.name, t.[text], 0 as is_persisted \n" +
				"from sys.sysobjects o with (nolock) \n" +
				"  join sys.syscolumns c with (nolock) on o.id = c.id \n" +
				"  join sys.syscomments t with (nolock) on  t.number = c.colid and t.id = c.id \n" +
				"where o.xtype = 'U' \n" +
				" and c.iscomputed = 1 \n"+
				" and o.name = ?";
			tablename = table.getTable().getRawTableName();
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("SqlServerColumnEnhancer.updateComputedColumns()",
				"Retrieving computed columns using query:\n" + SqlUtil.replaceParameters(sql, tablename));
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		Map<String, String> expressions = new HashMap<>();
		Map<String, Boolean> persisted = new HashMap<>();

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tablename);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String def = rs.getString(2);
				if (StringUtil.isEmptyString(def)) continue;

				def = def.trim();
				boolean isPersisted = rs.getBoolean(3);
				persisted.put(colname, Boolean.valueOf(isPersisted));
				String exp1 = expressions.get(colname);
				if (exp1 != null)
				{
					def = exp1 + def;
				}
				expressions.put(colname, def);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlServerColumnEnhancer.updateComputedColumns()", "Error retrieving computed columns using:\n" + SqlUtil.replaceParameters(sql, tablename), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		for (ColumnIdentifier col : table.getColumns())
		{
			String expr = expressions.get(col.getColumnName());
			if (StringUtil.isBlank(expr)) continue;

			if (!expr.startsWith("("))
			{
				expr += "(" + expr + ")";
			}
			expr = "AS " + expr;
			Boolean isPersisted = persisted.get(col.getColumnName());
			if (Boolean.TRUE.equals(isPersisted))
			{
				expr = expr + " PERSISTED";
			}
			col.setComputedColumnExpression(expr);
		}
	}

	public void readCollations(TableDefinition table, WbConnection conn)
	{
		String defaultCollation = null;
		Statement info = null;
		ResultSet rs = null;

		String defSql = "select cast(databasepropertyex('" + table.getTable().getRawCatalog() + "', 'Collation') as varchar(128))";
		try
		{

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("SqlServerColumnEnhancer.readCollations()", "Retrieving default collation using: " + defSql);
			}

			info = conn.createStatement();
			rs = info.executeQuery(defSql);
			if (rs.next())
			{
				defaultCollation = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("SqlServerColumnEnhancer.readCollations()", "Could not read default collation using: " + defSql, e);
		}
		finally
		{
			SqlUtil.closeAll(rs, info);
		}

		PreparedStatement stmt = null;

		HashMap<String, String> expressions = new HashMap<>(table.getColumnCount());
		HashMap<String, String> collations = new HashMap<>(table.getColumnCount());

		String sql =
			"SELECT COLUMN_NAME, \n" +
			"       COLLATION_NAME \n" +
			"FROM INFORMATION_SCHEMA.COLUMNS \n" +
			"WHERE TABLE_NAME = ? \n" +
			"  AND TABLE_SCHEMA = ? \n " +
			"  AND TABLE_CATALOG = ?";

		String tableName = table.getTable().getRawTableName();
		String schema = table.getTable().getRawSchema();
		String catalog = table.getTable().getRawCatalog();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerColumnEnhancer.readCollations()",
				"Retrieving column collations using query:\n" + SqlUtil.replaceParameters(sql, tableName, schema, catalog));
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tableName);
			stmt.setString(2, schema);
			stmt.setString(3, catalog);

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String collation = rs.getString(2);
				if (isNonDefault(collation, defaultCollation))
				{
					expressions.put(colname, "COLLATE " + collation);
					collations.put(colname, collation);
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("SqlServerColumnEnhancer.readCollations()", "Could not read column collations using:\n" + SqlUtil.replaceParameters(sql, tableName, schema, catalog), ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		for (ColumnIdentifier col : table.getColumns())
		{
			String expression = expressions.get(col.getColumnName());
			if (StringUtil.isNonEmpty(expression))
			{
				String dataType = col.getDbmsType() + " " + expression;
				col.setDbmsType(dataType);

				String collation = collations.get(col.getColumnName());
				col.setCollation(collation);
			}
		}
	}

	private boolean isNonDefault(String value, String defaultValue)
	{
		if (StringUtil.isEmptyString(value)) return false;
		return !value.equals(defaultValue);
	}
}
