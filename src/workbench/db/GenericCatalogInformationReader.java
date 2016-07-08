/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.Statement;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GenericCatalogInformationReader
  implements CatalogInformationReader, PropertyChangeListener
{
  private static final String NO_CATALOG = "$wb$-no-catalog";
  private final WbConnection dbConnection;
  private final DbSettings dbSettings;
  private boolean supportsGetCatalog = true;
  private boolean isCacheable = false;
  private String cachedCatalog = NO_CATALOG;

  public GenericCatalogInformationReader(WbConnection conn, DbSettings settings)
  {
    this.dbConnection = conn;
    this.dbSettings = settings;
    this.isCacheable = dbSettings.getBoolProperty("currentcatalog.cacheable", false);
    if (isCacheable)
    {
      this.dbConnection.addChangeListener(this);
    }
  }

  /**
   * Return the current catalog for this connection.
   * <p>
   * If no catalog is defined or the DBMS does not support catalogs, null is returned.
   * <p>
   * This method works around a bug in Microsoft's JDBC driver which does
   * not return the correct database (=catalog) after the database has
   * been changed with the USE <db> command from within the Workbench.
   * <p>
   * If no query has been configured for the current DBMS, Connection.getCatalog()
   * is used, otherwise the query that is configured with the property
   * workbench.db.[dbid].currentcatalog.query
   *
   * @see DbSettings#getQueryForCurrentCatalog()
   * @see DbSettings#supportsCatalogs()
   *
   * @return The name of the current catalog or null if there is no current catalog
   */
  @Override
  public String getCurrentCatalog()
  {
    if (!dbSettings.supportsCatalogs())
    {
      return null;
    }

    if (isCacheable && cachedCatalog != NO_CATALOG)
    {
      return cachedCatalog;
    }

    String catalog = null;

    String query = dbSettings.getQueryForCurrentCatalog();
    if (query != null)
    {
      // for some reason, getCatalog() does not return the correct
      // information when using Microsoft's JDBC driver.
      // If this is the case, a SQL query can be defined that is
      // used instead of the JDBC call, e.g. SELECT db_name()
      Statement stmt = null;
      ResultSet rs = null;

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("GenericCatalogInformationReader.getCurrentCatalog()", "Retrieving current catalog using: " + query);
      }

      try
      {
        stmt = this.dbConnection.createStatementForQuery();
        rs = stmt.executeQuery(query);
        if (rs.next())
        {
          catalog = rs.getString(1);
        }
      }
      catch (Exception e)
      {
        LogMgr.logWarning("GenericCatalogInformationReader.getCurrentCatalog()", "Error retrieving current catalog using query: " + query, e);
        catalog = null;
      }
      finally
      {
        SqlUtil.closeAll(rs, stmt);
      }
    }

    if (catalog == null && supportsGetCatalog)
    {
      try
      {
        catalog = this.dbConnection.getSqlConnection().getCatalog();
      }
      catch (Exception e)
      {
        LogMgr.logWarning("GenericCatalogInformationReader.getCurrentCatalog()", "Could not retrieve catalog using getCatalog()", e);
        catalog = null;
        supportsGetCatalog = false;
      }
    }

    if (isCacheable)
    {
      cachedCatalog = catalog;
    }
    return catalog;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (evt.getPropertyName().equals(WbConnection.PROP_CATALOG))
    {
      clearCache();
    }
  }

  @Override
  public void clearCache()
  {
    cachedCatalog = NO_CATALOG;
  }


}
