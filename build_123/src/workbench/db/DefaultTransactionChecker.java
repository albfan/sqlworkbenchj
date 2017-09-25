/*
 * DefaultTransactionChecker.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultTransactionChecker
  implements TransactionChecker
{

  private String query;
  public DefaultTransactionChecker(String sql)
  {
    query = sql;
  }

  @Override
  public boolean hasUncommittedChanges(WbConnection con)
  {
    Savepoint sp = null;
    ResultSet rs = null;
    Statement stmt = null;
    int count = 0;
    try
    {
      if (con.getDbSettings().useSavePointForDML())
      {
        sp = con.setSavepoint();
      }
      stmt = con.createStatementForQuery();
      rs = stmt.executeQuery(query);
      if (rs.next())
      {
        count = rs.getInt(1);
      }
      con.releaseSavepoint(sp);
    }
    catch (SQLException sql)
    {
      LogMgr.logDebug(getClass().getSimpleName() + ".hasUncommittedChanges()", "Could not retrieve transaction state", sql);
      con.rollback(sp);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return count > 0;
  }

}
