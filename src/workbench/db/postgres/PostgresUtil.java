/*
 * PostgresUtil
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.Connection;
import java.sql.Statement;
import workbench.db.JdbcUtils;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUtil
{
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
}
