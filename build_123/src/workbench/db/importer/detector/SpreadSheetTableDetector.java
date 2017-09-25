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
package workbench.db.importer.detector;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.db.WbConnection;
import workbench.db.importer.SpreadsheetReader;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class SpreadSheetTableDetector
  extends TableDetector
{
  private int sheetIndex;
  private String sheetName;
  private Map<String, List<ColumnStatistics>> sheetMap = new HashMap<>();

  public SpreadSheetTableDetector(File spreadSheet, boolean containsHeader, int sheet)
  {
    inputFile = new WbFile(spreadSheet);
    withHeader = containsHeader;
    sheetIndex = sheet < 0 ? -1 : sheet;
  }

  @Override
  protected void processFile()
  {
    analyzeSpreadSheet();
  }

  @Override
  protected void checkResults()
  {
    for (Map.Entry<String, List<ColumnStatistics>> entry : sheetMap.entrySet())
    {
      super.checkResults(entry.getValue(), entry.getKey());
    }
  }

  @Override
  protected String getDisplayFilename()
  {
    String name = super.getDisplayFilename();
    if (StringUtil.isNonBlank(sheetName))
    {
      name += ":" + SqlUtil.cleanupIdentifier(sheetName);
    }
    return name;
  }

  @Override
  public String getCreateTable(WbConnection conn)
    throws SQLException
  {
    if (sheetMap.size() == 1)
    {
      return super.getCreateTable(conn, sheetMap.values().iterator().next());
    }

    StringBuilder sql = new StringBuilder(sheetMap.size() * 100);
    for (Map.Entry<String, List<ColumnStatistics>> entry : sheetMap.entrySet())
    {
      String table = super.getCreateTable(conn, entry.getValue(), entry.getKey());
      sql.append(table);
      sql.append("\n\n");
    }
    return sql.toString();
  }

  @Override
  protected String getTableNameToUse()
  {
    if (StringUtil.isNonBlank(sheetName))
    {
      return SqlUtil.cleanupIdentifier(sheetName);
    }
    return super.getTableNameToUse();
  }

  private void analyzeSpreadSheet()
  {
    SpreadsheetReader reader = SpreadsheetReader.Factory.createReader(inputFile, sheetIndex, null);
    if (reader == null) return;

    try
    {
      reader.load();
      List<String> sheets = reader.getSheets();
      int start = -1;
      int end = -1;

      if (sheetIndex == -1)
      {
        start = 0;
        end = sheets.size() - 1;
      }
      else
      {
        start = sheetIndex;
        end = sheetIndex;
      }

      for (int i=start; i <= end; i++)
      {
        String name = sheets.get(i);
        reader.setActiveWorksheet(i);

        List<String> cols = reader.getHeaderColumns();

        if (withHeader == false)
        {
          for (int c=0; c < cols.size(); c++)
          {
            cols.set(c, "column_" + Integer.toString(i+1));
          }
        }

        List<ColumnStatistics> sheetColumns = new ArrayList<>(cols.size());
        for (String col : cols)
        {
          sheetColumns.add(new ColumnStatistics(col));
        }

        int numLines = Math.min(reader.getRowCount(), sampleSize);

        int startRow = withHeader ? 1 : 0;
        for (int row=startRow; row < numLines; row++)
        {
          List<Object> values = reader.getRowValues(row);
          analyzeValues(values, sheetColumns);
        }
        sheetMap.put(name, sheetColumns);
      }

      if (sheetMap.size() == 1)
      {
        this.columns = sheetMap.values().iterator().next();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("SpreadSheetTableDetector.analyzeSpreadSheet()", "Error reading spreadsheet", th);
      messages.append(ExceptionUtil.getDisplay(th));
    }
    finally
    {
      reader.done();
    }
  }
}
