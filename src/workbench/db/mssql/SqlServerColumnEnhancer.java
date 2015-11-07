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
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
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
  private Map<String, String> defaultCollations = new HashMap<>();

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		if (SqlServerUtil.isSqlServer2000(conn))
		{
			updateComputedColumns(table, conn);
		}

		if (conn.getDbSettings().getBoolProperty("remarks.column.retrieve", true))
		{
			updateColumnRemarks(table, conn);
		}

		if (SqlServerUtil.isSqlServer2005(conn))
		{
			updateColumnInformation(table, conn);
		}
	}

	private void updateColumnRemarks(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = SqlUtil.removeObjectQuotes(table.getTable().getTableName());
		String schema = SqlUtil.removeObjectQuotes(table.getTable().getSchema());

		String propName = conn.getDbSettings().getProperty(SqlServerObjectListEnhancer.REMARKS_PROP_NAME, SqlServerObjectListEnhancer.REMARKS_PROP_DEFAULT);

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
				"from sysobjects o with (nolock) \n" +
				"  join syscolumns c with (nolock) on o.id = c.id \n" +
				"  join syscomments t with (nolock) on t.number = c.colid and t.id = c.id \n" +
				"where o.xtype = 'U' \n" +
				"  and c.iscomputed = 1 \n"+
				"  and o.name = ?";
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

  private String getDatabaseCollation(String database, WbConnection conn)
  {
    String collation = defaultCollations.get(database);
    if (collation != null)
    {
      return collation;
    }

    Statement info = null;
    ResultSet rs = null;

    String sql = "select cast(databasepropertyex('" + database + "', 'Collation') as varchar(128))";
    try
    {

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("SqlServerColumnEnhancer.getDatabaseCollation()", "Retrieving database collation using: " + sql);
      }

      info = conn.createStatement();
      rs = info.executeQuery(sql);
      if (rs.next())
      {
        collation = rs.getString(1);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("SqlServerColumnEnhancer.getDatabaseCollation()", "Could not read database collation using: " + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, info);
    }
    defaultCollations.put(database, collation);
    return collation;
  }

	public void updateColumnInformation(TableDefinition table, WbConnection conn)
	{
    String defaultCollation = getDatabaseCollation(table.getTable().getRawCatalog(), conn);

    String types = conn.getDbSettings().getProperty("adjust.datatypes", "geometry,geography");

    Set<String> nonJdbcTypes = CollectionUtil.caseInsensitiveSet();
    nonJdbcTypes.addAll(StringUtil.stringToList(types, ",", true, true));

		String sql =
			"select column_name, \n" +
			"       collation_name, \n" +
			"       data_type \n" +
			"from information_schema.columns \n" +
			"where table_name = ? \n" +
			"  and table_schema = ? \n " +
			"  and table_catalog = ?";

		String tableName = table.getTable().getRawTableName();
		String schema = table.getTable().getRawSchema();
		String catalog = table.getTable().getRawCatalog();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerColumnEnhancer.()",
				"Retrieving additional column information using:\n" + SqlUtil.replaceParameters(sql, tableName, schema, catalog));
		}

    PreparedStatement stmt = null;
    ResultSet rs = null;
    List<ColumnIdentifier> columns = table.getColumns();

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
          ColumnIdentifier col = ColumnIdentifier.findColumnInList(columns, colname);
  				String dataType = col.getDbmsType() + " COLLATE " + collation;
          col.setDbmsType(dataType);
          col.setCollation(collation);
				}
        String type = rs.getString(3);
        if (nonJdbcTypes.contains(type))
        {
          ColumnIdentifier col = ColumnIdentifier.findColumnInList(columns, colname);
          col.setDbmsType(type);
          col.setDataType(Types.OTHER);
        }
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("SqlServerColumnEnhancer.updateColumnInformation()",
        "Could not read column details using:\n" + SqlUtil.replaceParameters(sql, tableName, schema, catalog), ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private boolean isNonDefault(String value, String defaultValue)
	{
		if (StringUtil.isEmptyString(value)) return false;
		return !value.equals(defaultValue);
	}
}
