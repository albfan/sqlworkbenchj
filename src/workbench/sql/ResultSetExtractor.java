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
import java.util.ArrayList;
import java.util.List;

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
    if (data == null) return 0;
    if (data.getRowCount() == 0) return 0;

    int rowCount = data.getRowCount();
    int resultCount = 0;
    int totalRows = 0;

    List<Integer> resultSetColumns = new ArrayList<>(1);
    for (int col=0; col < data.getColumnCount(); col ++)
    {
      Class clz = data.getColumnClass(0);
      if (clz.isAssignableFrom(ResultSet.class))
      {
        resultSetColumns.add(col);
      }
    }

    if (resultSetColumns.isEmpty()) return 0;

    for (int row=0; row < rowCount; row++)
    {
      for (int i=0; i < resultSetColumns.size(); i++)
      {
        int col = resultSetColumns.get(i);

        Object value = data.getValue(row, col);
        // this should always work
        if (value instanceof ResultSet)
        {
          resultCount++;
          ResultSet rs = (ResultSet)value;
          if (readData)
          {
            totalRows += addDataStore(result, rs);
          }
          else
          {
            result.addResultSet(rs);
          }
        }
      }
    }

    if (totalRows > 0)
    {
      result.setRowsProcessed(totalRows);
    }

    return resultCount;
  }

  private int addDataStore(StatementRunnerResult result, ResultSet rs)
  {
    try
    {
      DataStore ds = new DataStore(rs, true);
      result.addDataStore(ds);
      return ds.getRowCount();
    }
    catch (Exception ex)
    {
      LogMgr.logError("ResultSetExtractor.extractEmbeddedResults()", "Could not read result", ex);
    }
    finally
    {
      SqlUtil.closeResult(rs);
    }
    return 0;
  }

}
