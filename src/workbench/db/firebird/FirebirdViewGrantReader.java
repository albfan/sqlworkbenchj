/*
 * FirebirdViewGrantReader.java
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
package workbench.db.firebird;

import workbench.db.ViewGrantReader;

/**
 * A class to read view grants for the Firebird database
 * @author Thomas Kellerer
 */
public class FirebirdViewGrantReader
  extends ViewGrantReader
{

  @Override
  public String getViewGrantSql()
  {
    String sql = "select trim(p.rdb$user) as grantee,  \n" +
             "case p.rdb$privilege \n" +
             "  when 'S' then 'SELECT'  \n" +
             "  when 'U' then 'UPDATE'  \n" +
             "  when 'D' then 'DELETE'  \n" +
             "  when 'I' then 'INSERT' \n" +
             "end as privilege,  \n" +
             "case p.rdb$grant_option  \n" +
             "  when 1 then 'YES' \n" +
             "  else 'NO' \n" +
             "end as is_grantable \n" +
             "from RDB$USER_PRIVILEGES p, rdb$relations r  \n" +
             "WHERE p.rdb$relation_name = r.rdb$relation_name \n" +
             "and p.rdb$user <> r.rdb$owner_name \n" +
             "AND  p.rdb$relation_name = ? ";

    return sql;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }

}
