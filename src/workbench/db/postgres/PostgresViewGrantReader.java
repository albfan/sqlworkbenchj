/*
 * PostgresViewGrantReader.java
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
package workbench.db.postgres;

import workbench.db.ViewGrantReader;

/**
 * A class to read view grants from the standard ANSI information_schema.
 * This will work for the following DBMS
 *
 * <ul>
 *	<li>PostgreSQL</li>
 *  <li>H2 Database</li>
 *  <li>MySQL</li>
 *  <li>Microsoft SQL Server 2000</li>
 * </ul>
 * @see workbench.db.ViewGrantReader#createViewGrantReader(workbench.db.WbConnection)
 *
 * @author Thomas Kellerer
 */
public class PostgresViewGrantReader
  extends ViewGrantReader
{

  @Override
  public String getViewGrantSql()
  {
    return 
      "select grantee, privilege_type, is_grantable  \n" +
      "from information_schema.table_privileges \n" +
      "where table_name = ? \n" +
      "  and table_schema = ? ";
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
