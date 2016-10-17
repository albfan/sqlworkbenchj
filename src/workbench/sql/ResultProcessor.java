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
package workbench.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A wrapper to process the results produced by a Statement.
 *
 * This properly deals with ResultSets that contain ResultSets.
 *
 * @author Thomas Kellerer
 */
public class ResultProcessor
{
  private boolean containsEmbeddedResults;
  private final Statement currentStatement;
  private ResultSet currentResult;
  private final WbConnection originalConnection;

  public ResultProcessor(Statement statement, ResultSet firstResult, WbConnection conn)
  {
    originalConnection = conn;
    currentStatement = statement;
    currentResult = firstResult;
    containsEmbeddedResults = checkForEmbeddedResults(conn, firstResult);
  }

  public ResultSet getResult()
  {
    if (containsEmbeddedResults && currentResult != null)
    {
      try
      {
        return (ResultSet)currentResult.getObject(1);
      }
      catch (Throwable th)
      {
        containsEmbeddedResults = false;
        LogMgr.logError("ResultProcessor.getResult()", "Could not retrieve embedded ResultSet", th);
      }
    }

    if (currentResult != null)
    {
      ResultSet rs = currentResult;
      currentResult = null;
      return rs;
    }

    try
    {
      ResultSet rs = currentStatement.getResultSet();
      containsEmbeddedResults = checkForEmbeddedResults(originalConnection, rs);
      if (containsEmbeddedResults)
      {
        currentResult = rs;
      }
      return rs;
    }
    catch (Exception ex)
    {
      return null;
    }
  }

  public boolean checkForMoreResults()
  {
    try
    {
      return hasMoreResults();
    }
    catch (Exception ex)
    {
      // Some drivers throw errors if no result is available. In this case
      // simply assume there are no more results.
      LogMgr.logError("SqlCommand.processResults()", "Error when calling getMoreResults()", ex);
      return false;
    }
  }

  public boolean hasMoreResults()
    throws SQLException
  {
    if (containsEmbeddedResults && currentResult != null)
    {
      if (currentResult.next())
      {
        return true;
      }
      // the currentResultSet is exhausted, we need to make sure it's closed
      SqlUtil.closeResult(currentResult);
      currentResult = null;
    }
    return currentStatement.getMoreResults();
  }

  private boolean checkForEmbeddedResults(WbConnection conn, ResultSet rs)
  {
    if (rs == null) return false;
    if (conn.getDbSettings().supportsEmbeddedResults() == false) return false;

    try
    {
      ResultSetMetaData meta = rs.getMetaData();
      if (meta == null) return false;

      String clzName = meta.getColumnClassName(1);
      Class clz = Class.forName(clzName);

      boolean isEmbeddedResult =  ResultSet.class.isAssignableFrom(clz);
      if (!isEmbeddedResult && conn.getDbSettings().refcursorIsEmbeddedResult())
      {
        String typename = meta.getColumnTypeName(1);
        isEmbeddedResult = conn.getDbSettings().getRefCursorTypeNames().contains(typename);
      }

      if (isEmbeddedResult)
      {
        rs.next(); // initialize the iterator
        return true;
      }

    }
    catch (Throwable th)
    {
      LogMgr.logWarning("ResultProcessor.checkForEmbeddedResults()", "Could not check for embedded resulst", th);
    }
    return false;
  }
}
