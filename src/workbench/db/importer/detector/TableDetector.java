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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.TableCreator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.formatter.FormatterUtil;

import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.ValueConverter;

/**
 * A class to detect a table structure from a CSV file.
 *
 * @author Thomas Kellerer
 */
public abstract class TableDetector
{
  protected List<ColumnStatistics> columns;
  protected File inputFile;
  protected boolean withHeader;
  protected int sampleSize;
  protected boolean success;
  protected ValueConverter converter;
  private MessageBuffer messages = new MessageBuffer();

  public boolean isSuccess()
  {
    return success;
  }

  public MessageBuffer getMessages()
  {
    return messages;
  }

  public String getCreateTable(WbConnection conn, String tableName)
    throws SQLException

  {
    if (CollectionUtil.isEmpty(columns)) return null;

    TableIdentifier tbl = new TableIdentifier(tableName);
    TableCreator creator = new TableCreator(conn, null, tbl, getDBColumns());
    creator.setUseFormatterSettings(true);
    creator.useDbmsDataType(false);

    return creator.getCreateTableSQL();
  }

  public List<ColumnIdentifier> getDBColumns()
  {
    if (CollectionUtil.isEmpty(columns)) return Collections.emptyList();

    List<ColumnIdentifier> result = new ArrayList<>(columns.size());
    for (ColumnStatistics colStat : columns)
    {
      ColType type = colStat.getBestType();
      String name = FormatterUtil.getIdentifier(colStat.getName());
      ColumnIdentifier col = new ColumnIdentifier(name, type.getJDBCType());

      if (type == ColType.Integer && colStat.getMaxLength() > 9)
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

  public abstract void analyzeFile();

  protected void analyzeValues(List<? extends Object> values)
  {
    if (values.size() != columns.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      if (values.get(i) == null) continue;

      Object value = values.get(i);

      ColumnStatistics stats = columns.get(i);
      ColType currentType = stats.getMostFrequentType();

      if (currentType == null)
      {
        checkOneValue(value, stats);
        continue;
      }

      int len = value.toString().length();

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
            stats.addValidType(currentType, len, 0);
            typeMatched = true;
          }
          break;
        case Decimal:
          if (isDecimal(value))
          {
            stats.addValidType(currentType, len, 0);
            typeMatched = true;
          }
          break;
        case String:
          stats.addValidType(currentType, len, 0);
          typeMatched = true;
          break;
      }
      if (!typeMatched)
      {
        checkOneValue(value, stats);
      }
    }
  }

  protected void checkOneValue(Object value, ColumnStatistics stats)
  {
    if (value == null) return;

    ColType type = getType(value);
    int digits = 0;
    if (type == ColType.Decimal)
    {
      digits = getDigits(value);
    }
    stats.addValidType(type, value.toString().length(), digits);
  }

  protected int getDigits(Object value)
  {
    if (value instanceof BigDecimal)
    {
      BigDecimal nr = (BigDecimal)value;
      return nr.scale();
    }
    try
    {
      BigDecimal nr = converter.getBigDecimal(value.toString(), Types.DECIMAL);
      return nr.scale();
    }
    catch (Exception ex)
    {
      return 0;
    }
  }

  protected ColType getType(Object value)
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

  private boolean isTimestamp(Object value)
  {
    if (value instanceof java.sql.Timestamp) return true;
    if (value instanceof java.util.Date) return true;
    try
    {
      converter.parseTimestamp(value.toString());
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }

  private boolean isDate(Object value)
  {
    if (value == null) return false;
    if (value instanceof java.util.Date) return true;
    if (value instanceof java.sql.Date) return true;

    try
    {
      converter.parseDate(value.toString());
      return true;
    }
    catch (Throwable ex)
    {
      return false;
    }
  }

  private boolean isDecimal(Object value)
  {
    if (value == null) return false;
    if (value instanceof BigDecimal) return true;
    if (value instanceof Number) return true;

    try
    {
      converter.getBigDecimal(value.toString(), java.sql.Types.DECIMAL);
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }

  private boolean isInteger(Object value)
  {
    if (value == null) return false;
    if (value instanceof Integer) return true;
    if (value instanceof Long) return true;

    try
    {
      converter.getLong(value.toString());
      return true;
    }
    catch (Exception ex)
    {
      return false;
    }
  }
}
