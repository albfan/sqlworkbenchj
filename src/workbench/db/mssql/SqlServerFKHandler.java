/*
 * SqlServerFKHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import workbench.db.DefaultFKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerFKHandler
	extends DefaultFKHandler
{
	private Map<TableIdentifier, Map<String, FkStatusInfo>> fkStatusInfo = new ConcurrentHashMap<>();

	public SqlServerFKHandler(WbConnection conn)
	{
		super(conn);
		containsStatusCol = false;
	}

	@Override
	public boolean supportsStatus()
	{
		return true;
	}

	@Override
	public FkStatusInfo getFkEnabledFlag(TableIdentifier table, String fkName)
	{
		if (CollectionUtil.isEmpty(fkStatusInfo)) return null;
		Map<String, FkStatusInfo> info = fkStatusInfo.get(table);
		if (CollectionUtil.isEmpty(info)) return null;

		return info.get(fkName);
	}

	@Override
	protected DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
		throws SQLException
	{
		readFkStatusForTable(tbl);
		return super.getRawKeyList(tbl, exported);
	}

	private void readFkStatusForTable(TableIdentifier table)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql =
			"select name, " +
			"       case when is_disabled = 1 then 'false' else 'true' end as enabled, \n" +
			"       case when is_not_trusted = 1 then 'false' else 'true' end as validated \n" +
			"from sys.foreign_keys with (nolock) \n" +
			"where parent_object_id = object_id(?)";

		Map<String, FkStatusInfo> info = new HashMap<>();

		try
		{
			stmt = getConnection().getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getFullyQualifiedName(getConnection()));
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String fkname = rs.getString(1);
				String enabledFlag = rs.getString(2);
				String validatedFlag = rs.getString(3);
				info.put(fkname, new FkStatusInfo("true".equals(enabledFlag), "true".equals(validatedFlag)));
			}
			fkStatusInfo.put(table, info);
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlServerFKHandler.readFkStatusForTable()", "Could not read FK status", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

}
