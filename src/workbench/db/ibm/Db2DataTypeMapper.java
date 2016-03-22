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
package workbench.db.ibm;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import workbench.db.DataTypeResolver;
import workbench.db.DefaultDataTypeResolver;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2DataTypeMapper
{
  private final Map<String, Integer> typeMap = new HashMap<>(19);
  private final DataTypeResolver resolver = new DefaultDataTypeResolver();

  public Db2DataTypeMapper()
  {
    typeMap.put("VARCHAR", Types.VARCHAR);
    typeMap.put("CHAR", Types.CHAR);
    typeMap.put("SMALLINT", Types.SMALLINT);
    typeMap.put("BIGINT", Types.BIGINT);
    typeMap.put("INTEGER", Types.INTEGER);
    typeMap.put("DECIMAL", Types.DECIMAL);
    typeMap.put("DOUBLE", Types.DOUBLE);
    typeMap.put("CHARACTER", Types.CHAR);
    typeMap.put("LONG VARCHAR", Types.LONGVARCHAR);
    typeMap.put("BLOB", Types.BLOB);
    typeMap.put("CLOB", Types.CLOB);
    typeMap.put("DATE", Types.DATE);
    typeMap.put("TIME", Types.TIME);
    typeMap.put("BOOLEAN", Types.BOOLEAN);
    typeMap.put("XML", Types.SQLXML);
    typeMap.put("VARBINARY", Types.VARBINARY);
    typeMap.put("REAL", Types.REAL);
    typeMap.put("ARRAY", Types.ARRAY);
    typeMap.put("BINARY", Types.BINARY);
  }

  public int getJDBCTypeName(String db2Name)
  {
    if (db2Name == null) return Types.OTHER;
    Integer type = typeMap.get(db2Name);
    if (type == null)
    {
      return Types.OTHER;
    }
    return type.intValue();
  }

  public String getDisplayType(String db2Type, int jdbcType, int length, int scale)
  {
    if (SqlUtil.isCharacterTypeWithLength(jdbcType))
    {
      return resolver.getSqlTypeDisplay(db2Type, jdbcType, length, 0);
    }
    return resolver.getSqlTypeDisplay(db2Type, jdbcType, length, scale);
  }
}
