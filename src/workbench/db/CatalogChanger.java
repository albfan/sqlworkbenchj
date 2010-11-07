/*
 * CatalogChanger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to change the current catalog in the database.
 *
 * This class uses DatabaseMetaData.setCatalog() to change the current catalog (database).
 * This can be changed in <tt>workbench.settings</tt> to use the SQL command <tt>USE</tt>
 * instead.
 * 
 * @author Thomas Kellerer
 */
public class CatalogChanger
{
	/**
	 * Changes the current catalog using Connection.setCatalog()
	 * and notifies the connection object about the change.
	 *
	 * @param newCatalog the name of the new catalog/database that should be selected
	 * @see WbConnection#catalogChanged(String, String)
	 */
	public boolean setCurrentCatalog(WbConnection conn, String newCatalog)
		throws SQLException
	{
		if (conn == null) return false;
		if (StringUtil.isEmptyString(newCatalog)) return false;

		String old = conn.getMetadata().getCurrentCatalog();
		boolean useSetCatalog = conn.getDbSettings().useSetCatalog();
		boolean clearWarnings = Settings.getInstance().getBoolProperty("workbench.db." + conn.getMetadata().getDbId() + ".setcatalog.clearwarnings", true);

		DbMetadata meta = conn.getMetadata();

		// MySQL does not seem to like changing the current database by executing a USE command
		// through Statement.execute(), so we'll use setCatalog() instead
		// which seems to work with SQL Server as well.
		// If for some reason this does not work, it could be turned off
		if (useSetCatalog)
		{
			conn.getSqlConnection().setCatalog(conn.getMetadata().removeQuotes(newCatalog.trim()));
		}
		else
		{
			Statement stmt = null;
			try
			{
				stmt = conn.createStatement();
				String cat = meta.quoteObjectname(newCatalog.trim());
				stmt.execute("USE " + cat);
				if (clearWarnings)
				{
					stmt.clearWarnings();
				}
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}

		if (clearWarnings)
		{
			conn.clearWarnings();
		}

		String newCat = meta.getCurrentCatalog();
		if (!StringUtil.equalString(old, newCat))
		{
			conn.catalogChanged(old, newCatalog);
		}
		LogMgr.logDebug("DbMetadata.setCurrentCatalog()", "Catalog changed to " + newCat);

		return true;
	}
}
