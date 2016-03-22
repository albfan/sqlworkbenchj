/*
 * JsonRowDataConverter.java
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
package workbench.db.exporter;


import java.text.SimpleDateFormat;

import workbench.db.TableIdentifier;

import workbench.storage.RowData;

import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;
import workbench.util.WbFile;
import workbench.util.WbNumberFormatter;

/**
 * Converts data from the database into JSON format.
 *
 * @author  Thomas Kellerer
 */
public class JsonRowDataConverter
  extends RowDataConverter
{
  public JsonRowDataConverter()
  {
    defaultDateFormatter = new WbDateFormatter(StringUtil.ISO_DATE_FORMAT);
    defaultTimestampFormatter = new WbDateFormatter(StringUtil.ISO_TIMESTAMP_FORMAT);
    defaultNumberFormatter = new WbNumberFormatter(-1, '.');
    defaultTimeFormatter = new SimpleDateFormat("HH:mm:ss");
  }

  @Override
  public StringBuilder getStart()
  {
    String resultName;
    TableIdentifier updateTable = this.metaData.getUpdateTable();
    if (updateTable != null)
    {
      resultName = updateTable.getRawTableName();
    }
    else
    {
      WbFile f = new WbFile(getOutputFile());
      resultName = f.getFileName();
    }

    StringBuilder header = new StringBuilder(20);
    header.append("{\n  \"");
    header.append(resultName.toLowerCase());
    header.append("\":\n  [\n");
    return header;
  }

  @Override
  public StringBuilder getEnd(long totalRows)
  {
    return new StringBuilder("\n  ]\n}");
  }

  @Override
  public StringBuilder convertRowData(RowData row, long rowIndex)
  {
    int count = this.metaData.getColumnCount();

    StringBuilder result = new StringBuilder(count * 30);

    int currentColIndex = 0;

    if (rowIndex > 0)
    {
      result.append(",\n");
    }
    result.append("    {");
    for (int c=0; c < count; c++)
    {
      if (!this.includeColumnInExport(c)) continue;

      if (currentColIndex > 0)
      {
        result.append(", ");
      }

      int colType = this.metaData.getColumnType(c);

      String value = this.getValueAsFormattedString(row, c);

      boolean isNull = (value == null);
      if (isNull)
      {
        value = "null";
      }

      if (SqlUtil.isCharacterType(colType) && !isNull)
      {
        value = StringUtil.escapeText(value, CharacterRange.RANGE_CONTROL, "");
        if (value.indexOf('"') > -1)
        {
          value = value.replace("\"", "\\\"");
        }
      }

      result.append("\"");
      result.append(this.metaData.getColumnName(c));
      result.append("\": ");
      if (!isNull) result.append('"');
      result.append(value);
      if (!isNull) result.append('"');

      currentColIndex ++;
    }
    result.append("}");
    return result;
  }

}
