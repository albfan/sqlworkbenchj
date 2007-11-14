/*
 * DerbyShutdownHook.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.shutdown;

import java.sql.SQLException;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * A Shutdown hook for Apache Derby.
 * 
 * @author support@sql-workbench.net
 */
public class DerbyShutdownHook
	implements DbShutdownHook
{

	public void shutdown(WbConnection conn)
		throws SQLException
	{
		if (ConnectionMgr.getInstance().isActive(conn)) return;
		
		conn.close();
		
		if (!canShutdown(conn)) return;
		
		ConnectionProfile prof = conn.getProfile();
		
		String drvClass = prof.getDriverclass();
		String drvName = prof.getDriverName();

		String url = prof.getUrl();
		int pos = url.indexOf(";");
		if (pos < 0) pos = url.length();
		String command = url.substring(0, pos) + ";shutdown=true";
		
		try
		{
			DbDriver drv = ConnectionMgr.getInstance().findDriverByName(drvClass, drvName);
			LogMgr.logInfo("ConnectionMgr.shutdownDerby()", "Local Derby connection detected. Shutting down engine...");
			drv.commandConnect(command);
		}
		catch (SQLException e)
		{
			// This exception is expected!
			// Cloudscape/Derby reports the shutdown success through an exception
			LogMgr.logInfo("ConnectionMgr.shutdownDerby()", ExceptionUtil.getDisplay(e));
		}
		catch (Throwable th)
		{
			LogMgr.logError("ConnectionMgr.shutdownDerby()", "Error when shutting down Cloudscape/Derby", th);
		}
	}

	private boolean canShutdown(WbConnection conn)
	{
		String cls = conn.getProfile().getDriverclass();
		
		// Never send a shutdown to a Derby server connection
		if (!cls.equals("org.apache.derby.jdbc.EmbeddedDriver")) return false;
		
		String url = conn.getUrl();
		int pos = StringUtil.indexOf(url, ':', 2);
		if (pos < 0) return true;
		
		String prefix = url.substring(pos + 1);
		
		// Do not shutdown Cloudscape server connections!
		if (url.startsWith(prefix + "net:")) return false;
		
		// Derby network URL starts with a // 
		if (url.startsWith(prefix + "//")) return false;
		
		return true;
	}
}
