/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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
import java.sql.Connection;
import java.sql.Types;

import workbench.log.LogMgr;

import workbench.db.WbConnection;

/**
 * A wrapper around Postgres' TypeInfo which doesn't require the Postgres JDBC
 * driver at compile time.
 *
 * @author Thomas Kellerer
 */
public class PgTypeInfo
{
  private Object typeInfo;
  private Method getSqlType;
  private Method getScale;
  private Method getPrecision;
  private Method getDisplaySize;

  public PgTypeInfo(WbConnection conn)
  {
    initMethods(conn.getSqlConnection());
  }

  public int getSQLType(int typeOid)
  {
    try
    {
      Object sqlTypeObj = getSqlType.invoke(typeInfo, typeOid);
      if (sqlTypeObj instanceof Number)
      {
        return ((Number)sqlTypeObj).intValue();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PgTypeInfo.getSqlType()", "Could not initialize method information", th);
    }
    return Types.OTHER;
  }

  public int getScale(int typeOid, int typeMod)
  {
    try
    {
      Object scaleObj = getScale.invoke(typeInfo, typeOid, typeMod);
      if (scaleObj instanceof Number)
      {
        return ((Number)scaleObj).intValue();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PgTypeInfo.getScale()", "Could not initialize method information", th);
    }
    return Types.OTHER;
  }

  public int getPrecision(int typeOid, int typeMod)
  {
    try
    {
      Object value = getPrecision.invoke(typeInfo, typeOid, typeMod);
      if (value instanceof Number)
      {
        return ((Number)value).intValue();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PgTypeInfo.getPrecision()", "Could not initialize method information", th);
    }
    return Types.OTHER;
  }

  public int getDisplaySize(int typeOid, int typeMod)
  {
    try
    {
      Object value = getDisplaySize.invoke(typeInfo, typeOid, typeMod);
      if (value instanceof Number)
      {
        return ((Number)value).intValue();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PgTypeInfo.getDisplaySize()", "Could not initialize method information", th);
    }
    return Types.OTHER;
  }

  private void initMethods(Connection con)
  {
    try
    {
      Method getTypeInfo = con.getClass().getMethod("getTypeInfo", (Class[])null);
      typeInfo = getTypeInfo.invoke(con, (Object[])null);

      getSqlType = typeInfo.getClass().getMethod("getSQLType", int.class);
      getScale = typeInfo.getClass().getMethod("getScale", int.class, int.class);
      getPrecision = typeInfo.getClass().getMethod("getPrecision", int.class, int.class);
      getDisplaySize = typeInfo.getClass().getMethod("getDisplaySize", int.class, int.class);
    }
    catch (Throwable th)
    {
      LogMgr.logError("PgTypeInfo.initMethods()", "Could not initialize method information", th);
    }
  }

}
