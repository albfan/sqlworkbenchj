/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db.hsqldb;

import java.sql.Types;

import workbench.db.DefaultDataTypeResolver;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlDataTypeResolver
  extends DefaultDataTypeResolver
{

  /**
   * Adjusts the display for BIT columns.
   *
   * @see workbench.util.SqlUtil#getSqlTypeDisplay(java.lang.String, int, int, int)
   */
  @Override
  public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
  {
    if (sqlType == Types.BIT)
    {
      return dbmsName + "(" + size + ")";
    }
    return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
  }

}
