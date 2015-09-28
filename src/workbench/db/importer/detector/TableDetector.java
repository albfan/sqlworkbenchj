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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.ValueConverter;
import workbench.util.WbStringTokenizer;

/**
 * A class to detect a table structure from a CSV file.
 *
 * @author Thomas Kellerer
 */
public class TableDetector
{
  private List<ColumnStatistics> columns;
  private File inputFile;
  private String delimiter;
  private String quoteChar;
  private boolean withHeader;
  private int sampleSize;
  private String encoding;
  private ValueConverter converter;
  private WbStringTokenizer tokenizer;
  private boolean success;

  public TableDetector(File csvFile, String delim, String quote, String dateFmt, String timestampFmt, boolean containsHeader, int numLines, String fileEncoding)
  {
    this.inputFile = csvFile;
    this.delimiter = delim;
    this.quoteChar = quote;
    this.withHeader = containsHeader;
    this.sampleSize = numLines;
    converter = new ValueConverter(dateFmt, timestampFmt);
		tokenizer = new WbStringTokenizer(delimiter, this.quoteChar, false);
		tokenizer.setDelimiterNeedsWhitspace(false);
  }

  public boolean isSuccess()
  {
    return success;
  }

  public List<ColumnIdentifier> getDBColumns()
  {
    List<ColumnIdentifier> result = new ArrayList<>(columns.size());
    for (ColumnStatistics colStat : columns)
    {
      ColType type = colStat.getBestType();
      ColumnIdentifier col = new ColumnIdentifier(colStat.getName(), type.getJDBCType());
      if (type == ColType.Integer && colStat.getMaxLength() > 14)
      {
        col.setDataType(Types.BIGINT);
      }

      col.setColumnSize(colStat.getMaxLength());
      if (type == ColType.Decimal)
      {
        col.setDecimalDigits(colStat.getMaxDigits());
      }
      result.add(col);
    }
    return result;
  }

  public void analyzeFile()
    throws IOException
  {
    success = false;
    BufferedReader reader = EncodingUtil.createBufferedReader(inputFile, encoding);
    List<String> lines = FileUtil.getLines(reader, false, false, sampleSize);

    int minSize = withHeader ? 2 : 1;
    int line = 0;

    if (lines.size() < minSize) return;

    initColumns(lines.get(line));

    if (columns.isEmpty()) return;

    if (withHeader)
    {
      line++;
    }

    String firstDataLine = lines.get(line);

    // try to find the data type of each column from the first line
    // the type detected there will be the first type to be tested for the subsequent lines
    initTypes(firstDataLine);

    line ++;

    for (int ln=line; ln < lines.size(); ln++)
    {
      parseLine(lines.get(ln));
    }
  }


  private void parseLine(String line)
  {
    tokenizer.setSourceString(line);
    List<String> values = tokenizer.getAllTokens();

    if (values.size() != columns.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      if (values.get(i) == null) continue;

      String value = values.get(i);

      ColumnStatistics stats = columns.get(i);
      ColType currentType = stats.getMostFrequentType();

      if (currentType == null)
      {
        checkOneValue(value, stats);
        continue;
      }

      boolean typeMatched = false;
      // by first validating the "previous" type we avoid the exceptions
      // that the isXXX() functions will generate for the wrong types
      // because usually all rows have the same type of values and thus
      // avoiding the exceptions makes the parsing faster (or so I hope)
      switch (currentType)
      {
        case Date:
          if (isDate(value))
          {
            stats.addValidType(currentType, 0, 0);
            typeMatched = true;
          }
          break;
        case Timestamp:
          if (isTimestamp(value))
          {
            stats.addValidType(currentType, 0, 0);
            typeMatched = true;
          }
          break;
        case Integer:
          if (isInteger(value))
          {
            stats.addValidType(currentType, value.length(), 0);
            typeMatched = true;
          }
          break;
        case Decimal:
          if (isDecimal(value))
          {
            stats.addValidType(currentType, value.length(), 0);
            typeMatched = true;
          }
          break;
        case String:
          stats.addValidType(currentType, value.length(), 0);
          typeMatched = true;
          break;
      }
      if (!typeMatched)
      {
        checkOneValue(value, stats);
      }
    }
  }

  private void checkOneValue(String value, ColumnStatistics stats)
  {
    if (value == null) return;

    ColType type = getType(value);
    int digits = 0;
    if (type == ColType.Decimal)
    {
      digits = getDigits(value);
    }
    stats.addValidType(type, value.length(), digits);
  }

  private void initTypes(String firstLine)
  {
    tokenizer.setSourceString(firstLine);
    List<String> values = tokenizer.getAllTokens();
    if (values.size() != columns.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      checkOneValue(values.get(i), columns.get(i));
    }
  }

  private int getDigits(String value)
  {
    try
    {
      BigDecimal nr = converter.getBigDecimal(value, Types.DECIMAL);
      return nr.scale();
    }
    catch (Exception ex)
    {
      return 0;
    }
  }

	private void initColumns(String headerLine)
	{
    tokenizer.setSourceString(headerLine);
    List<String> values = tokenizer.getAllTokens();
    columns = new ArrayList<>(values.size());

    for (int i=0; i < values.size(); i++)
    {
      String colName;
      if (withHeader)
      {
        colName = values.get(i);
      }
      else
      {
        colName = "column_" + Integer.valueOf(i+1);
      }
      columns.add(new ColumnStatistics(colName));
    }
	}

  private ColType getType(String value)
  {
    if (isInteger(value))
    {
      return ColType.Integer;
    }
    if (isDecimal(value))
    {
      return ColType.Decimal;
    }
    if (isTimestamp(value))
    {
      return ColType.Timestamp;
    }
    if (isDate(value))
    {
      return ColType.Date;
    }
    return ColType.String;
  }

  private boolean isTimestamp(String value)
  {
    try
    {
      converter.parseTimestamp(value);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }

  private boolean isDate(String value)
  {
    try
    {
      converter.parseDate(value);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }

  private boolean isDecimal(String value)
  {
    try
    {
      converter.getBigDecimal(value, java.sql.Types.DECIMAL);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }

  private boolean isInteger(String value)
  {
    try
    {
      converter.getLong(value);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }
}
