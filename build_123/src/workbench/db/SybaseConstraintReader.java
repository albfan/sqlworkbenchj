/*
 * SybaseConstraintReader.java
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
package workbench.db;


/**
 * Constraint reader for Adaptive Server Anywhere
 * @author  Thomas Kellerer
 */
public class SybaseConstraintReader
  extends AbstractConstraintReader
{
  private final String TABLE_SQL =
      "select chk.check_defn \n" +
      "from syscheck chk, sysconstraint cons, systable tbl \n" +
      "where chk.check_id = cons.constraint_id \n" +
      "and   cons.constraint_type = 'T' \n" +
      "and   cons.table_id = tbl.table_id \n" +
      "and   tbl.table_name = ? \n";

  public SybaseConstraintReader(WbConnection conn)
  {
    super(conn.getDbId());
  }

  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

  @Override
  public String getTableConstraintSql()
  {
    return TABLE_SQL;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }
}
