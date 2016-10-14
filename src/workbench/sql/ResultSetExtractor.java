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

import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultSetExtractor
{

  public int extractEmbeddedResults(WbConnection conn, StatementRunnerResult result, DataStore data, boolean readData)
  {
    if (data.getRowCount() == 0) return 0;
    if (data.getColumnCount() != 1) return 0;

    int rowCount = data.getRowCount();
    int resultCount = 0;
    int totalRows = 0;

    for (int row=0; row < rowCount; row++)
    {
      Object value = data.getValue(row, 0);
      if (value instanceof ResultSet)
      {
        ResultSet rs = (ResultSet)value;
        if (readData)
        {
          try
          {
            DataStore ds = new DataStore(rs, true);
            result.addDataStore(ds);
            totalRows += ds.getRowCount();
            resultCount++;
          }
          catch (Exception ex)
          {
            LogMgr.logError("ResultSetExtractor.extractEmbeddedResults()", "Could not read result", ex);
          }
          finally
          {
            SqlUtil.closeResult(rs);
          }
        }
        else
        {
          result.addResultSet(rs);
        }
      }
    }

    if (totalRows > 0)
    {
      result.setRowsProcessed(totalRows);
    }

    return resultCount;
  }

}
