/*
 * HsqlTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlTableSourceBuilder
	extends TableSourceBuilder
{

	public HsqlTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public void readTableConfigOptions(TableIdentifier tbl)
	{
		if (!JdbcUtils.hasMinimumServerVersion(dbConnection, "2.2")) return;

		if (tbl.getTableTypeOption() != null) return;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select hsqldb_type \n" +
			"from information_schema.system_tables \n" +
			"where table_name = ? \n" +
			"and table_schem = ?";

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getTableName());
			pstmt.setString(2, tbl.getSchema());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("HsqlTableSourceBuilder.readTableConfigOptions()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String type = rs.getString(1);
				tbl.setTableTypeOption(type);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("PostgresTableSourceBuilder.readTableConfigOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

}
