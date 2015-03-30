/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.QuoteHandler;
import workbench.db.TableIdentifier;
import workbench.db.TypeMapper;
import workbench.db.WbConnection;

import workbench.sql.formatter.FormatterUtil;
import workbench.sql.syntax.SqlKeywordHelper;

import workbench.util.CollectionUtil;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.ValueConverter;
import workbench.util.WbFile;

/**
 * A class to detect a table structure from an import file.
 *
 * @see TextFileTableDetector
 * @see SpreadSheetTableDetector
 *
 * @author Thomas Kellerer
 */
public abstract class TableDetector
{
  public static final int DEFAULT_SAMPLE_SIZE = 1000;

  protected List<ColumnStatistics> columns;
  protected WbFile inputFile;
  protected boolean withHeader;
  protected int sampleSize = DEFAULT_SAMPLE_SIZE;
  protected ValueConverter converter;
  protected MessageBuffer messages = new MessageBuffer();
  private SqlKeywordHelper helper;
  private boolean alwaysUseVarchar;
  private String tableName;

  public MessageBuffer getMessages()
  {
    return messages;
  }

  public void setTableName(String name)
  {
    if (StringUtil.isNonBlank(name))
    {
      this.tableName = name.trim();
    }
  }

  public boolean hasMessages()
  {
    return messages.getLength() > 0;
  }

  public void setAlwaysUseVarchar(boolean flag)
  {
    this.alwaysUseVarchar = flag;
  }

  public void setSampleSize(int lines)
  {
    if (lines <= 0)
    {
      sampleSize = Integer.MAX_VALUE;
    }
    else
    {
      sampleSize = lines;
    }
  }

  protected String getDisplayFilename()
  {
    if (inputFile == null) return "";
    return inputFile.getName();
  }

  protected String getTableNameToUse()
  {
    if (StringUtil.isNonBlank(tableName)) return tableName.trim();
    if (inputFile == null) return "import_table";
    return SqlUtil.cleanupIdentifier(inputFile.getFileName());
  }

  public String getCreateTable(WbConnection conn)
    throws SQLException
  {
    return getCreateTable(conn, columns);
  }

  public String getCreateTable(WbConnection conn, List<ColumnStatistics> colStats)
    throws SQLException
  {
    return getCreateTable(conn, colStats, getTableNameToUse());
  }

  public String getCreateTable(WbConnection conn, List<ColumnStatistics> colStats, String table)
    throws SQLException
  {
    if (CollectionUtil.isEmpty(colStats)) return null;

    TableIdentifier tbl = new TableIdentifier(table);

    List<ColumnIdentifier> dbColumns = getDBColumns(colStats);

    String result = FormatterUtil.getKeyword("CREATE TABLE ");
    result += FormatterUtil.getIdentifier(tbl.getTableExpression(conn));
    result += "\n(\n";

    TypeMapper mapper = new TypeMapper(conn);

    String unbounded = conn == null ? null : conn.getDbSettings().getUnboundedVarcharType();

    int defaultMaxLength = 32767;
    int maxLength = conn == null ? defaultMaxLength : conn.getDbSettings().getMaxVarcharLength();

    if (maxLength < 0) maxLength = defaultMaxLength;

    int maxColNameLength = getMaxColumNameLength(conn, dbColumns) + 3;

    for (int i=0; i < dbColumns.size(); i++)
    {
      ColumnIdentifier col = dbColumns.get(i);
      int size = col.getColumnSize();
      int type = alwaysUseVarchar ? Types.VARCHAR : col.getDataType();

      if (SqlUtil.isCharacterType(type))
      {
        if (size <= maxLength)
        {
          size = maxLength;
        }
        else
        {
          type = Types.CLOB;
        }
      }

      if (i > 0) result +=",\n";
      result += "  " + StringUtil.padRight(getColumnName(conn, col), maxColNameLength);

      String typeName = null;
      if (SqlUtil.isCharacterType(type) && unbounded != null)
      {
        typeName = unbounded;
      }
      else
      {
        typeName = mapper.getTypeName(type, size, col.getDecimalDigits());
      }
      result += FormatterUtil.getDataType(typeName);
    }

    result += "\n);";
    return result;
  }

  public List<ColumnIdentifier> getDBColumns()
  {
    return getDBColumns(columns);
  }

  public List<ColumnIdentifier> getDBColumns(List<ColumnStatistics> colStats)
  {
    if (CollectionUtil.isEmpty(colStats)) return Collections.emptyList();

    List<ColumnIdentifier> result = new ArrayList<>(colStats.size());
    int pos = 0;
    for (ColumnStatistics colStat : colStats)
    {
      ColType type = colStat.getBestType();
      String name = FormatterUtil.getIdentifier(colStat.getName());
      ColumnIdentifier col = new ColumnIdentifier(name, type.getJDBCType());
      col.setPosition(pos);
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
      pos ++;
    }
    return result;
  }

  protected void checkResults(List<ColumnStatistics> colStats, String tableName)
  {
    for (ColumnStatistics col : colStats)
    {
      List<ColType> types = col.getDetectedTypes();
      if (types.isEmpty())
      {
        String colname = col.getName();
        if (tableName != null)
        {
          colname = tableName + "." + colname;
        }
        messages.append(ResourceMgr.getFormattedString("MsgImpTblNoType", colname));
        messages.appendNewLine();
      }
      else if (col.getDetectedTypes().size() > 1)
      {
        String typeNames = "";
        for (int i=0; i < types.size(); i++)
        {
          if (i > 0) typeNames += ", ";
          typeNames += types.get(i).toString();
        }
        messages.append(getDisplayFilename() + ": " + ResourceMgr.getFormattedString("MsgImpTblMultipleTypes", col.getName(), typeNames));
        messages.appendNewLine();
      }
    }
  }

  protected void checkResults()
  {
    checkResults(columns, null);
  }

  public void analyzeFile()
  {
    processFile();
    checkResults();
  }

  protected abstract void processFile();

  protected void analyzeValues(List<? extends Object> values, List<ColumnStatistics> colStats)
  {
    if (values.size() != colStats.size()) return;

    for (int i=0; i < values.size(); i ++)
    {
      Object value = values.get(i);

      // can't check the type for a null value
      if (value == null) continue;
      if (value.toString().isEmpty()) continue;

      ColumnStatistics stats = colStats.get(i);
      ColType currentType = stats.getMostFrequentType();

      int len = value.toString().length();
      if (alwaysUseVarchar)
      {
        stats.addValidType(ColType.String, len, 0);
        continue;
      }

      if (currentType == null)
      {
        checkOneValue(value, stats);
        continue;
      }

      boolean typeMatched = false;

      // by first validating the "previous" type we avoid the exceptions
      // that the isXXX() functions will generate for the wrong types
      // because usually all rows have the same type of values and
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

    int digits = 0;
    ColType type = null;
    if (alwaysUseVarchar)
    {
      type = ColType.String;
    }
    else
    {
      type = getType(value);
      if (type == ColType.Decimal)
      {
        digits = getDigits(value);
      }
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

  private String getColumnName(WbConnection conn, ColumnIdentifier col)
  {
    QuoteHandler quoter = conn == null ? QuoteHandler.STANDARD_HANDLER : conn.getMetadata();

    String name = col.getColumnName();
    if (!quoter.isLegalIdentifier(name) || isReservedWord(conn, name))
    {
      name = quoter.getIdentifierQuoteCharacter() + name + quoter.getIdentifierQuoteCharacter();
    }
    return FormatterUtil.getIdentifier(name);
  }

  private boolean isReservedWord(WbConnection conn, String name)
  {
    if (conn == null)
    {
      return getKeyWordHelper().getReservedWords().contains(name);
    }
    return conn.getMetadata().isReservedWord(name);
  }

  private SqlKeywordHelper getKeyWordHelper()
  {
    if (helper == null)
    {
      helper = new SqlKeywordHelper();
    }
    return helper;
  }

  private int getMaxColumNameLength(WbConnection conn, List<ColumnIdentifier> columns)
  {
    int maxlen = 0;
    for (ColumnIdentifier col : columns)
    {
      String name = getColumnName(conn, col);
      if (name.length() > maxlen)
      {
        maxlen = name.length();
      }
    }
    return maxlen;
  }
}
