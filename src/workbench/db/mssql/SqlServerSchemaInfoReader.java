/*
 * SqlServerSchemaInfoReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.db.JdbcUtils;
import workbench.log.LogMgr;

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
	private String defaultSchema;

	public SqlServerSchemaInfoReader(WbConnection con)
	{
		// As the default schema is a property of the user definition and nothing that can be changed at runtime
		// I assume it's safe to cache the current schema.
    if (JdbcUtils.hasMiniumDriverVersion(con, "4.0") && SqlServerUtil.isMicrosoftDriver(con))
    {
      defaultSchema = getSchema(con);
    }
    else
    {
      defaultSchema = retrieveSchema(con);
    }
    LogMgr.logDebug("SqlServerSchemaInfoReader.<init>", "Using current schema: " + defaultSchema);
	}

  private String getSchema(WbConnection con)
  {
    try
    {
      // not all driver versions support this properly
      return con.getSqlConnection().getSchema();
    }
    catch (Throwable th)
    {
      LogMgr.logError("SqlServerSchemaInfoReader.getSchema()", "Error retrieving current schema using getSchema()", th);
      return null;
    }
  }

  private String  retrieveSchema(WbConnection con)
  {
    String schema = null;
    String sql
      = "SELECT default_schema_name\n" +
      "FROM sys.database_principals with (nolock) \n" +
      "WHERE type = 'S' \n" +
      "AND name = current_user";

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
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
		return defaultSchema;
	}

	@Override
	public void dispose()
	{
		// nothing to do
	}


}
