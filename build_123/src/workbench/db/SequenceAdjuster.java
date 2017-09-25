/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
package workbench.db;

import java.sql.SQLException;

import workbench.db.firebird.FirebirdSequenceAdjuster;
import workbench.db.h2database.H2SequenceAdjuster;
import workbench.db.hsqldb.HsqlSequenceAdjuster;
import workbench.db.ibm.Db2SequenceAdjuster;
import workbench.db.postgres.PostgresSequenceAdjuster;

/**
 *
 * @author Thomas Kellerer
 */
public interface SequenceAdjuster
{
  int adjustTableSequences(WbConnection connection, TableIdentifier table, boolean includeCommit)
    throws SQLException;

  public static class Factory
  {
    public static SequenceAdjuster getSequenceAdjuster(WbConnection conn)
    {
      switch (DBID.fromConnection(conn))
      {
        case Postgres:
          return new PostgresSequenceAdjuster();

        case H2:
          return new H2SequenceAdjuster();

        case HSQLDB:
          if (JdbcUtils.hasMinimumServerVersion(conn, "2.0"))
          {
            return new HsqlSequenceAdjuster();
          }
          break;

        case DB2_LUW:
          return new Db2SequenceAdjuster();

        case Firebird:
          if (JdbcUtils.hasMinimumServerVersion(conn, "3.0"))
          {
            return new FirebirdSequenceAdjuster();
          }
          break;
      }
      return null;
    }
  }

}
