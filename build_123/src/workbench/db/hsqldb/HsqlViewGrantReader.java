/*
 * HsqlViewGrantReader.java
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

import workbench.db.JdbcUtils;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlViewGrantReader
  extends ViewGrantReader
{
  private String sql;

  public HsqlViewGrantReader(WbConnection con)
  {
    if (JdbcUtils.hasMinimumServerVersion(con, "2.0"))
    {
      sql = "select grantee, privilege_type, is_grantable  \n" +
            "from information_schema.TABLE_PRIVILEGES \n" +
            "where table_name = ? \n" +
            " and table_schema = ? ";
    }
    else
    {
      sql = "select grantee, privilege, is_grantable  \n" +
            "from information_schema.SYSTEM_TABLEPRIVILEGES \n" +
            "where table_name = ? \n" +
            "  and table_schem = ? ";
    }
  }

  @Override
  public String getViewGrantSql()
  {
    return sql;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }

  @Override
  public int getIndexForSchemaParameter()
  {
    return 2;
  }

}
