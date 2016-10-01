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
package workbench.db.postgres;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import workbench.WbManager;
import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.compare.BatchedStatement;
import workbench.db.importer.ArrayValueHandler;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresArrayHandler
  implements ArrayValueHandler
{
  private Class pgObjectClass;
  private Method setValue;
  private Method setType;
  private boolean useDefaultClassloader;

  public PostgresArrayHandler(WbConnection connection)
  {
    useDefaultClassloader = WbManager.isTest();
    initialize(connection);
  }

  private void initialize(WbConnection connection)
  {
    try
    {
      String className = "org.postgresql.util.PGobject";

      if (useDefaultClassloader)
      {
        pgObjectClass = Class.forName(className);
      }
      else
      {
        pgObjectClass = ConnectionMgr.getInstance().loadClassFromDriverLib(connection.getProfile(), className);
      }

      setType = pgObjectClass.getMethod("setType", String.class);
      setValue = pgObjectClass.getMethod("setValue", String.class);
    }
    catch (Throwable t)
    {
      LogMgr.logError("PgCopyImporter.createCopyManager()", "Could not create CopyManager", t);
    }
  }

  @Override
  public void setValue(PreparedStatement stmt, int columnIndex, Object data, ColumnIdentifier colInfo)
    throws SQLException
  {
    Object pgo = createPgObject(data, colInfo.getDbmsType());
    stmt.setObject(columnIndex, pgo);
  }

  @Override
  public void setValue(BatchedStatement stmt, int columnIndex, Object data, ColumnIdentifier colInfo)
    throws SQLException
  {
    Object pgo = createPgObject(data, colInfo.getDbmsType());
    stmt.setObject(columnIndex, pgo);
  }

  private Object createPgObject(Object data, String userType)
  {
    if (setValue == null) return data;
    if (setType == null) return data;

    String internalType = PostgresDataTypeResolver.mapArrayDisplayToInternal(userType);

    if (internalType == null)
    {
      LogMgr.logDebug("PostgresArrayHandler.createPgObject()", "No mapping for type: " + userType);
      return data;
    }

    String value = adjustLiteral(data);

    try
    {
      Object pgo = pgObjectClass.newInstance();
      setType.invoke(pgo, internalType);
      setValue.invoke(pgo, value);
      return pgo;
    }
    catch (Throwable th)
    {
      LogMgr.logWarning("PostgresArrayHandler.createPgObject()", "Could not create instance of PGobject", th);
      return data;
    }
  }

  private String adjustLiteral(Object data)
  {
    if (data == null) return null;
    String value = StringUtil.trimToNull(data.toString());
    if (isArrayLiteral(value))
    {
      return value;
    }
    return "{" + value + "}";
  }

  private boolean isArrayLiteral(String literal)
  {
    if (literal == null) return true;
    if (literal.length() < 2) return false;
    return literal.charAt(0) == '{' && literal.charAt(literal.length() - 1) == '}';
  }
}
