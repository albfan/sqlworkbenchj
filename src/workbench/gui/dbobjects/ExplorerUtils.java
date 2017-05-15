/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.dbobjects;

import workbench.resource.DbExplorerSettings;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ExplorerUtils
{
  public static void endTransaction(WbConnection dbConnection)
  {
    if (dbConnection == null) return;
    if (dbConnection.getAutoCommit()) return;

    if (isOwnTransaction(dbConnection) && dbConnection.selectStartsTransaction())
    {
      dbConnection.rollbackSilently();
    }
  }

  public static boolean isOwnTransaction(WbConnection dbConnection)
  {
    if (dbConnection == null) return false;
    if (dbConnection.getAutoCommit()) return false;
    return (dbConnection.getProfile().getUseSeparateConnectionPerTab() || DbExplorerSettings.getAlwaysUseSeparateConnForDbExpWindow());
  }

    /**
   * Initialize a connection to be used by the DbExplorer and DbTree.
   * <p>
   * <br>
   * This should only be used for Profiles where a different connection is used for the DbExplorer
   * and the regular SQL panels.
   * <br><br>
   * Currently it will do the following:
   * <p>
   * <ul>
   * <li>Disable DBMS_OUTPUT</li>
   * <li>Set a LOCK_TIMEOUT for SQL Server to prevent waiting indefinitely for the retrieval if some DDL statement wasn't comitted</li>
   * </ul>
   *
   * @param connection
   *
   * @see DbMetadata#disableOutput()
   * @see SqlServerUtil#setLockTimeout(workbench.db.WbConnection, int)
   * @see DbSettings#getLockTimoutForSqlServer()
   */
  public static void initDbExplorerConnection(WbConnection connection)
  {
    // when dealing with tables that have LONG or LONG RAW columns
    // and DBMS_OUTPUT was enabled, then retrieval of those columns
    // does not work. If we have separate connections for each tab
    // we can safely disable the DBMS_OUTPUT on this connection
    // as there won't be a way to view the output anyway
    connection.getMetadata().disableOutput();

    if (connection.getMetadata().isSqlServer())
    {
      // we rather want an error message than the DbExplorer or DbTree waiting indefinitely
      int timeout = connection.getDbSettings().getLockTimoutForSqlServer();
      if (timeout > 0)
      {
        SqlServerUtil.setLockTimeout(connection, timeout);
      }
    }
  }

}
