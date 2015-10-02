/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;

import workbench.db.importer.SpreadsheetReader;
import workbench.log.LogMgr;
import workbench.util.ExceptionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SpreadSheetTableDetector
  extends TableDetector
{
  private int sheetIndex;

  public SpreadSheetTableDetector(File spreadSheet, boolean containsHeader, int sheet)
  {
    inputFile = spreadSheet;
    withHeader = containsHeader;
    sheetIndex = sheet > -1 ? sheet : 0;
  }

  @Override
  protected void processFile()
  {
    analyzeSpreadSheet();
  }

  private void analyzeSpreadSheet()
  {
    SpreadsheetReader reader = SpreadsheetReader.Factory.createReader(inputFile, sheetIndex, null);
    if (reader == null) return;

    try
    {
      reader.load();
      List<String> cols = reader.getHeaderColumns();
      columns = new ArrayList<>(cols.size());
      for (String col : cols)
      {
        columns.add(new ColumnStatistics(col));
      }

      int numLines = Math.min(reader.getRowCount(), sampleSize);

      for (int row=1; row < numLines; row++)
      {
        List<Object> values = reader.getRowValues(row);
        analyzeValues(values);
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
