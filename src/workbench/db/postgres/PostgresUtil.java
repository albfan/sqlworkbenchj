/*
 * PostgresUtil
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.VersionNumber;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUtil
{

	/**
	 * The property that can be passed during connecting to identify the application.
	 *
	 * @see #supportsAppInfoProperty(java.lang.Class)
	 */
	public static final String APP_NAME_PROPERTY = "ApplicationName";

	/**
	 * Sets the application name for pg_stat_activity.
	 * To set the name, the autocommit will be turned off, and the transaction will be committed afterwards.
	 * The name will only be set if the PostgreSQL version is >= 9.0
	 *
	 * @param con the connection
	 * @param appName the name to set
	 */
	public static void setApplicationName(Connection con, String appName)
	{
		if (JdbcUtils.hasMinimumServerVersion(con, "9.0") && Settings.getInstance().getBoolProperty("workbench.db.postgresql.set.appname", true))
		{
				Statement stmt = null;
				try
				{
					// SET application_name seems to require autocommit to be turned off
					// as the autocommit setting that the user specified in the connection profile
					// will be set after this call, setting it to false should not do any harm
					con.setAutoCommit(false);
					stmt = con.createStatement();
					stmt.execute("SET application_name = '" + appName + "'");
					// make sure the transaction is ended
					// as this is absolutely the first thing we did, commit() should be safe
					con.commit();
				}
				catch (Exception e)
				{
					// Make sure the transaction is ended properly
					try { con.rollback(); } catch (Exception ex) {}
					LogMgr.logWarning("DbDriver.setApplicationName()", "Could not set client info", e);
				}
				finally
				{
					SqlUtil.closeStatement(stmt);
				}
		}
	}

	/**
	 * Checks if the passed driver supports the ApplicationName property.
	 *
	 * Setting the application name for pg_stat_activity is only supported by drivers >= 9.1
	 *
	 * @param pgDriver
	 * @return true if the driver supports the ApplicationName property
	 * @see #APP_INFO_PROPERTY
	 */
	public static boolean supportsAppInfoProperty(Class pgDriver)
	{
		try
		{
			Field major = pgDriver.getDeclaredField("MAJORVERSION");
			Field minor = pgDriver.getDeclaredField("MINORVERSION");
			int majorVersion = major.getInt(null);
			int minorVersion = minor.getInt(null);

			VersionNumber version = new VersionNumber(majorVersion, minorVersion);
			VersionNumber min = new VersionNumber(9,1);
			return version.isNewerOrEqual(min);
		}
		catch (Throwable th)
		{
			return false;
		}
	}

	/**
	 * Returns the current search path defined in the session (or the user).
	 * <br/>
	 * This uses the Postgres function <tt>current_schemas()</tt>
	 * <br/>
	 * @param con the connection for which the search path should be retrieved
	 * @return the list of schemas in the search path.
	 */
	public static List<String> getSearchPath(WbConnection con)
	{
		if (con == null) return Collections.emptyList();
		List<String> result = new ArrayList<String>();

		ResultSet rs = null;
		Statement stmt = null;
		Savepoint sp = null;
		try
		{
			sp = con.setSavepoint();
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery("select unnest(current_schemas(false))");
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (SQLException sql)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresUtil.getSearchPath()", "Could not read search path", sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

}
