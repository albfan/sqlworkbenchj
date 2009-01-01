/*
 * JdbcUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import workbench.util.VersionNumber;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class JdbcUtils 
{

	public static boolean hasMinimumServerVersion(WbConnection con, String targetVersion)
	{
		return hasMinimumServerVersion(con.getSqlConnection(), targetVersion);
	}
	
	public static boolean hasMinimumServerVersion(Connection con, String targetVersion)
	{
		VersionNumber target = new VersionNumber(targetVersion);
		try
		{
			int serverMajor = con.getMetaData().getDatabaseMajorVersion();
			int serverMinor = con.getMetaData().getDatabaseMinorVersion();
			VersionNumber server = new VersionNumber(serverMajor, serverMinor);
			return server.isNewerOrEqual(target);
		}
		catch (Throwable th)
		{
			return false;
		}
	}
	
	public static boolean hasMiniumDriverVersion(Connection con, String targetVersion)
	{
		VersionNumber target = new VersionNumber(targetVersion);
		try
		{
			int driverMajor = con.getMetaData().getDriverMajorVersion();
			int driverMinor = con.getMetaData().getDriverMinorVersion();
			VersionNumber driver = new VersionNumber(driverMajor, driverMinor);
			return driver.isNewerOrEqual(target);
		}
		catch (Throwable th)
		{
			return false;
		}
	}
	
	public static int getColumnIndex(ResultSet rs, String colname)
	{
		try
		{
			if (rs == null) return -1;
			if (StringUtil.isEmptyString(colname)) return -1;
			ResultSetMetaData meta = rs.getMetaData();
			int colcount = meta.getColumnCount();
			for (int i=1; i <= colcount; i++)
			{
				String name = meta.getColumnName(i);
				if (name.equalsIgnoreCase(colname)) return i;
			}
		}
		catch (Exception e)
		{
			// ignore
		}
		return -1;
	}
	
}
