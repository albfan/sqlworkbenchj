/*
 * PostgresDataTypeResolver.java
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
package workbench.db.postgres;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import workbench.db.DataTypeResolver;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class PostgresDataTypeResolver
  implements DataTypeResolver
{

  private static final Map<String, String> arrayTypesToDisplay = new HashMap<>();
  private static final Map<String, String> displayToArrayType = new HashMap<>();

  static
  {
    arrayTypesToDisplay.put("_int2","smallint[]");
    arrayTypesToDisplay.put("_int4","integer[]");
    arrayTypesToDisplay.put("_int8","bigint[]");
    arrayTypesToDisplay.put("_varchar","varchar[]");
    arrayTypesToDisplay.put("_float4","real[]");
    arrayTypesToDisplay.put("_float8","double precision[]");
    arrayTypesToDisplay.put("_bpchar","char[]");
    arrayTypesToDisplay.put("_text","text[]");
    arrayTypesToDisplay.put("_bool","boolean[]");
    arrayTypesToDisplay.put("_numeric","numeric[]");
    arrayTypesToDisplay.put("_date","date[]");
    arrayTypesToDisplay.put("_time","time[]");
    arrayTypesToDisplay.put("_timestamp","timestamp[]");
    arrayTypesToDisplay.put("_timestamptz","timestamptz[]");
    arrayTypesToDisplay.put("_timetz","timetz[]");

    for (Map.Entry<String, String> entry : arrayTypesToDisplay.entrySet())
    {
      displayToArrayType.put(entry.getValue(), entry.getKey());
    }
    displayToArrayType.put("timestamp without time zone[]", "_timestamp");
    displayToArrayType.put("timestamp with time zone[]", "_timestamptz");
    displayToArrayType.put("time without time zone[]", "_time");
    displayToArrayType.put("time with time zone[]", "_timetz");
  }

  @Override
  public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
  {
    if (sqlType == Types.VARCHAR && "text".equals(dbmsName)) return "text";
    if (sqlType == Types.VARCHAR && "character varying".equals(dbmsName))
    {
      dbmsName = "varchar";
    }
    if (sqlType == Types.SMALLINT && "int2".equals(dbmsName)) return "smallint";
    if (sqlType == Types.INTEGER && "int4".equals(dbmsName)) return "integer";
    if (sqlType == Types.BIGINT && "int8".equals(dbmsName)) return "bigint";
    if ((sqlType == Types.BIT || sqlType == Types.BOOLEAN) && "bool".equals(dbmsName)) return "boolean";

    if (sqlType == Types.CHAR && "bpchar".equals(dbmsName))
    {
      return "char(" + size + ")";
    }

    if (sqlType == Types.VARCHAR && size == Integer.MAX_VALUE)
    {
      // enums are returned as Types.VARCHAR and size == Integer.MAX_VALUE
      // in order to not change the underlying data type, we just use
      // the type name that the driver returned
      return dbmsName;
    }

    if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL)
    {
      if (size == 65535 || size == 131089) size = 0;
      if (digits == 65531) digits = 0;
    }

    if (sqlType == Types.OTHER && "varbit".equals(dbmsName))
    {
      return "bit varying(" + size + ")";
    }

    if (sqlType == Types.BIT && "bit".equals(dbmsName))
    {
      return "bit(" + size + ")";
    }

    if (sqlType == Types.ARRAY && dbmsName.charAt(0) == '_')
    {
      return mapInternaArrayToDisplay(dbmsName);
    }

    if ("varchar".equals(dbmsName) && size < 0) return "varchar";

    return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
  }

  @Override
  public String getColumnClassName(int type, String dbmsType)
  {
    return null;
  }

  @Override
  public int fixColumnType(int type, String dbmsType)
  {
    if (type == Types.BIT && "bool".equals(dbmsType)) return Types.BOOLEAN;
    return type;
  }

  public static String mapArrayDisplayToInternal(String dbmsType)
  {
    String internal = displayToArrayType.get(dbmsType);
    if (internal != null)
    {
      return internal;
    }
    return "_" + StringUtil.replace(dbmsType, "[]", "");
  }

  public static String mapInternaArrayToDisplay(String internal)
  {
    if (StringUtil.isEmptyString(internal)) return null;

    String display = arrayTypesToDisplay.get(internal);
    if (display != null) return display;
    if (internal.charAt(0) == '_')
    {
      return internal.substring(1) + "[]";
    }
    return internal;
  }
}
