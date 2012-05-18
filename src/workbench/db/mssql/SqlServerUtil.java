/*
 * SqlServerUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mssql;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerUtil
{
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

}
