/*
 * H2TableSourceBuilder.java
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
package workbench.db.h2database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DropType;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class H2TableSourceBuilder
	extends TableSourceBuilder
{
	public H2TableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
  public String getTableSource(TableIdentifier table, DropType drop, boolean includeFk, boolean includeGrants)
		throws SQLException
	{
		if ("TABLE LINK".equals(table.getType()))
		{
			String sql = getLinkedTableSource(table, drop != DropType.none);
			if (sql != null) return sql;
		}
		return super.getTableSource(table, drop, includeFk, includeGrants);
	}

	private String getLinkedTableSource(TableIdentifier table, boolean includeDrop)
		throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String sql =
			"SELECT sql FROM information_schema.tables " +
			" WHERE table_schema = ? " +
			"   AND table_name = ? " +
			"   AND table_type = 'TABLE LINK'";

		StringBuilder createSql = new StringBuilder(100);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("H2TableSourceBuilder.getLinkedTableSource()", "Using statement: " + sql);
		}

		if (includeDrop)
		{
			createSql.append("DROP TABLE ");
			createSql.append(table.getTableExpression(dbConnection));
			createSql.append(";\n\n");
		}
		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getSchema());
			stmt.setString(2, table.getTableName());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				String create = rs.getString(1);
				if (StringUtil.isNonEmpty(create))
				{
					create = create.replace("/*--hide--*/", "");
				}
				createSql.append(create.trim());
				createSql.append(";\n");
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("H2TableSourceBuilder.getLinkedTableSource()", "Error retrieving table source", ex);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return createSql.toString();
	}

	@Override
	public void readTableOptions(TableIdentifier tbl, List<ColumnIdentifier> columns)
	{
		if (tbl.getSourceOptions().isInitialized()) return;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select storage_type, \n" +
			"       (select value from information_schema.settings where name = 'DEFAULT_TABLE_TYPE') as default_type \n" +
			"from information_schema.tables \n" +
			"where table_name = ? \n" +
			"and table_schema = ?";

		boolean alwaysShowType = Settings.getInstance().getBoolProperty("workbench.db.h2.table_type.show_always", false);

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getTableName());
			pstmt.setString(2, tbl.getSchema());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("H2TableSourceBuilder.readTableConfigOptions()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String type = rs.getString(1);
				String defaultType = rs.getString(2);
				if ("0".equals(defaultType))
				{
					defaultType = "CACHED";
				}
				else
				{
					defaultType = "MEMORY";
				}

				if (alwaysShowType || !defaultType.equals(type))
				{
					tbl.getSourceOptions().setTypeModifier(type);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("H2TableSourceBuilder.readTableConfigOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		tbl.getSourceOptions().setInitialized();
	}

}
