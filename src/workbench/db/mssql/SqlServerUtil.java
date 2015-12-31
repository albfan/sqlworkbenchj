/*
 * SqlServerUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.sql.Statement;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerUtil
{
  public static boolean isMicrosoftDriver(WbConnection conn)
  {
    if (conn == null) return false;
    String url = conn.getUrl();
    if (url == null) return false;
    return url.startsWith("jdbc:sqlserver:");
  }

	/**
	 * Returns true if the connection is to a SQL Server 2014 or later.
	 */
	public static boolean isSqlServer2014(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "12.0");
	}

	/**
	 * Returns true if the connection is to a SQL Server 2012 or later.
	 */
	public static boolean isSqlServer2012(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "11.0");
	}

	/**
	 * Returns true if the connection is to a SQL Server 2008R2 or later.
	 */
	public static boolean isSqlServer2008R2(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "10.5");
	}

	/**
	 * Returns true if the connection is to a SQL Server 2008 or later.
	 */
	public static boolean isSqlServer2008(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "10.0");
	}

	/**
	 * Returns true if the connection is to a SQL Server 2005 or later.
	 */
	public static boolean isSqlServer2005(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "9.0");
	}

	/**
	 * Returns true if the connection is to a SQL Server 2000 or later.
	 */
	public static boolean isSqlServer2000(WbConnection conn)
	{
		return JdbcUtils.hasMinimumServerVersion(conn, "8.0");
	}

	public static void setLockTimeout(WbConnection conn, int millis)
	{
		Statement stmt = null;
		String sql = "SET LOCK_TIMEOUT " + Integer.toString(millis <= 0 ? -1 : millis );
		try
		{
			stmt = conn.createStatement();
			LogMgr.logInfo("SqlServerUtil.setLockTimeout()", "Setting lock timeout: " + millis + "ms");
			stmt.execute(sql);
		}
		catch (Throwable ex)
		{
			LogMgr.logError("SqlServerUtil.setLockTimeout()", "Could not set lock timeout using: " + sql, ex);
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

}
