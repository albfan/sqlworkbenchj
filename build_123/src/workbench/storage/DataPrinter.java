/*
 * DataPrinter.java
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
package workbench.storage;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.exporter.TextRowDataConverter;

import workbench.util.CharacterRange;
import workbench.util.StringUtil;
import workbench.util.WbNumberFormatter;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream
 *
 * @author Thomas Kellerer
 */
public class DataPrinter
{
  private final DataStore data;
  private TextRowDataConverter converter;
  private int[] columnMap;

  public DataPrinter(DataStore source)
  {
    this.data = source;
    initConverter("\t", StringUtil.LINE_TERMINATOR, null, true);
  }

  public DataPrinter(DataStore source, boolean includeHeaders)
  {
    this.data = source;
    initConverter("\t", StringUtil.LINE_TERMINATOR, null, includeHeaders);
  }

  public DataPrinter(DataStore source, String delimiter, String lineEnd, List<ColumnIdentifier> columns, boolean includeHeader)
  {
    this.data = source;
    initConverter(delimiter, lineEnd, columns, includeHeader);
  }

  public void setEscapeRange(CharacterRange range)
  {
    if (this.converter != null)
    {
      converter.setEscapeRange(range);
    }
  }

  private void initConverter(String delimiter, String lineEnd, List<ColumnIdentifier> columns, boolean includeHeader)
  {
    converter = new TextRowDataConverter();
    converter.setResultInfo(data.getResultInfo());
    converter.setWriteBlobToFile(false);
    converter.setWriteHeader(includeHeader);
    converter.setLineEnding(lineEnd);
    converter.setDelimiter(delimiter);
    converter.setColumnsToExport(columns);
    converter.setEscapeRange(CharacterRange.RANGE_NONE);
    WbNumberFormatter formatter = Settings.getInstance().createDefaultDecimalFormatter();
    converter.setDefaultNumberFormatter(formatter);
  }

  public void setNullString(String value)
  {
    converter.setNullString(value);
  }

  /**
   * Define a mapping from the stored order of columns to the order
   * that is visible to the user.
   *
   * @param map
   */
  public void setColumnMapping(int[] map)
  {
    columnMap = map;
  }

  /**
   * Write the contents of the DataStore into the writer but only the rows
   * that have been passed in the rows[] parameter
   *
   * @param out the writer to use
   * @param rows the rows to print, if this is null all rows are printed
   */
  public void writeDataString(Writer out, int[] rows)
    throws IOException
  {
    CharSequence header = converter.getStart(columnMap);
    if (header != null)
    {
      out.write(header.toString());
      out.flush();
    }

    int count = (rows == null ? data.getRowCount() : rows.length);

    for (int i=0; i < count; i++)
    {
      int row = (rows == null ? i : rows[i]);
      CharSequence line = converter.convertRowData(data.getRow(row), row, columnMap);
      out.write(line.toString());
      out.flush();
    }
  }

}
