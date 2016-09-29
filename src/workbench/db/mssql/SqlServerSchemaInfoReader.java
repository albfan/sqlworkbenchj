/*
 * SqlServerSchemaInfoReader.java
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
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.db.JdbcUtils;
import workbench.db.SchemaInformationReader;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSchemaInfoReader
  implements SchemaInformationReader
{
  private final WbConnection dbConnection;
  private String defaultSchema;
  private boolean schemaRetrieved;

  public SqlServerSchemaInfoReader(WbConnection con)
  {
    dbConnection = con;
    retrieveCurrentSchema();
  }

  private void retrieveCurrentSchema()
  {
    // As the default schema is a property of the user definition and nothing that can be changed at runtime
    // I assume it's safe to cache the current schema.

    schemaRetrieved = false;
    if (JdbcUtils.hasMiniumDriverVersion(dbConnection, "4.2") && SqlServerUtil.isMicrosoftDriver(dbConnection))
    {
      try
      {
        // not all driver versions support this properly, so in case anything goes wrong
        // use the default SQL query to retrieve the user's default schema
        defaultSchema = dbConnection.getSqlConnection().getSchema();
        schemaRetrieved = true;
      }
      catch (Throwable th)
      {
        LogMgr.logWarning("SqlServerSchemaInfoReader.<init>", "Error retrieving current schema using getSchema(): " + th.getMessage());
      }
    }

    if (!schemaRetrieved)
    {
      defaultSchema = retrieveSchema(dbConnection);
      schemaRetrieved = true;
    }
    LogMgr.logDebug("SqlServerSchemaInfoReader.<init>", "Using current schema: " + defaultSchema);
  }

  private String retrieveSchema(WbConnection con)
  {
    String schema = null;
    // this is what the Microsoft 4.x driver is using.
    // The function is available starting with SQL Server 2005
    String sql = "SELECT schema_name()";

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.getSqlConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      rs = stmt.executeQuery(sql);
      if (rs.next())
      {
        schema = rs.getString(1);
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("SqlServerSchemaInfoReader", "Could not obtain default schema using: \n" + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return schema;
  }

  @Override
  public boolean isSupported()
  {
    return true;
  }

  @Override
  public void clearCache()
  {
    this.defaultSchema = null;
  }

  @Override
  public String getCachedSchema()
  {
    return defaultSchema;
  }

  @Override
  public String getCurrentSchema()
  {
    if (!schemaRetrieved)
    {
      synchronized (dbConnection)
      {
        retrieveCurrentSchema();
      }
    }
    return defaultSchema;
  }

  @Override
  public void dispose()
  {
    // nothing to do
  }

}
