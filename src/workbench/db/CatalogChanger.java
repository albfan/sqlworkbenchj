/*
 * CatalogChanger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to change the current catalog in the database.
 *
 * This class uses DatabaseMetaData.setCatalog() to change the current catalog (database) or runs the USE command
 * depending on the DB property <tt>usesetcatalog</tt>
 *
 * @author Thomas Kellerer
 * @see workbench.db.DbSettings#useSetCatalog()
 */
public class CatalogChanger
{
  private boolean fireEvents = true;

  public void setFireEvents(boolean flag)
  {
    this.fireEvents = flag;
  }

	/**
	 * Changes the current catalog using Connection.setCatalog()
	 * and notifies the connection object about the change.
	 *
   * Not all JDBC drivers support <tt>Connection.setCatalog()</tt> so this can be turned off
   * through {@link DbSettings#useSetCatalog()}. If that is disabled, a <tt>USE</tt> command
   * is sent to the database.
   *
	 * @param newCatalog the name of the new catalog/database that should be selected
	 * @see WbConnection#catalogChanged(String, String)
   *
   * @see DbSettings#useSetCatalog()
	 */
	public boolean setCurrentCatalog(WbConnection conn, String newCatalog)
		throws SQLException
	{
		if (conn == null) return false;
		if (StringUtil.isEmptyString(newCatalog)) return false;

		String old = conn.getMetadata().getCurrentCatalog();
		boolean useSetCatalog = conn.getDbSettings().useSetCatalog();
		boolean clearWarnings = conn.getDbSettings().getBoolProperty("setcatalog.clearwarnings", true);

		DbMetadata meta = conn.getMetadata();

    String sql = conn.getDbSettings().getSwitchCatalogStatement();

		// MySQL does not seem to like changing the current database by executing a USE command
		// so we'll use setCatalog() instead which seems to work with SQL Server as well.
		// If for some reason this does not work, it could be turned off
    if (useSetCatalog || StringUtil.isBlank(sql))
		{
			conn.getSqlConnection().setCatalog(conn.getMetadata().removeQuotes(newCatalog.trim()));
		}
		else
		{
			Statement stmt = null;
			try
			{
				stmt = conn.createStatement();
				sql = sql.replace(TableSourceBuilder.CATALOG_PLACEHOLDER, meta.quoteObjectname(newCatalog.trim()));
        LogMgr.logDebug("CatalogChanger.setCurrentCatalog()", "Changing catalog using: " + sql);
				stmt.execute(sql);
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
			// Some JDBC drivers report the success of changing the catalog through a warning
			// as we are displaying our own message anyway in the USE command, there is no need
			// to display the warning as well.
			conn.clearWarnings();
		}

		String newCat = meta.getCurrentCatalog();
		if (fireEvents && StringUtil.stringsAreNotEqual(old, newCat))
		{
			conn.catalogChanged(old, newCatalog);
		}
		LogMgr.logDebug("CatalogChanger.setCurrentCatalog()", "Catalog changed to " + newCat);

		return true;
	}
}
