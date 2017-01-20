/*
 * SqlServerDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.hsqldb;


import java.sql.Types;

import workbench.db.DBID;
import workbench.resource.Settings;
import workbench.storage.DataConverter;

/**
 * A class to treat HSQLDB's BIT as a boolean
 *
 * @author Thomas Kellerer
 */
public class HsqlDataConverter
  implements DataConverter
{
  private static class LazyInstanceHolder
  {
    protected static final HsqlDataConverter instance = new HsqlDataConverter();
  }

  public static HsqlDataConverter getInstance()
  {
    return LazyInstanceHolder.instance;
  }

  private HsqlDataConverter()
  {
  }

  @Override
  public Class getConvertedClass(int jdbcType, String dbmsType)
  {
    if (convertsType(jdbcType, dbmsType)) return Boolean.class;
    return null;
  }

  /**
   * Checks if jdbcType == Types.BINARY and if dbmsType == "timestamp"
   *
   * @param jdbcType the jdbcType as returned by the driver
   * @param dbmsType the name of the datatype for this value
   *
   * @return true if Microsoft's "timestamp" type
   */
  @Override
  public boolean convertsType(int jdbcType, String dbmsType)
  {
    if (jdbcType == Types.BIT)
    {
      return dbmsType.equals("BIT");
    }
    return false;
  }

  @Override
  public Object convertValue(int jdbcType, String dbmsType, Object originalValue)
  {
    if (originalValue == null) return null;

    if (!convertsType(jdbcType, dbmsType)) return originalValue;

    try
    {
      return (Boolean)originalValue;
    }
    catch (Throwable th)
    {
      return originalValue;
    }
  }

  public static boolean convertBit()
  {
    return Settings.getInstance().getBoolProperty("workbench.db." + DBID.HSQLDB.getId() + ".converter.bit", false);
  }
}
