/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db.hsqldb;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import workbench.db.ArrayValueHandler;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.BatchedStatement;

import workbench.util.ConverterException;
import workbench.util.CsvLineParser;
import workbench.util.QuoteEscapeType;
import workbench.util.ValueConverter;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlArrayHandler
  implements ArrayValueHandler
{
  private final WbConnection conn;

  public HsqlArrayHandler(WbConnection connection)
  {
    this.conn = connection;
  }

  @Override
  public void setValue(BatchedStatement stmt, int columnIndex, Object data, ColumnIdentifier col)
    throws SQLException
  {
    if (data == null)
    {
      stmt.setNull(columnIndex, java.sql.Types.ARRAY);
    }
    else
    {
      Array array = parseArrayString(data, col);
      stmt.setObject(columnIndex, array, java.sql.Types.ARRAY);
    }
  }

  @Override
  public void setValue(PreparedStatement stmt, int columnIndex, Object data, ColumnIdentifier col)
    throws SQLException
  {
    if (data == null)
    {
      stmt.setNull(columnIndex, java.sql.Types.ARRAY);
    }
    else
    {
      Array array = parseArrayString(data, col);
      stmt.setArray(columnIndex, array);
    }
  }

  public Array parseArrayString(Object value, ColumnIdentifier col)
    throws SQLException
  {
    if (value instanceof Array)
    {
      return (Array)value;
    }
    String baseType = getBaseType(col);
    Object[] data = createArray(value, baseType);

    Array array = conn.getSqlConnection().createArrayOf(baseType, data);

    return array;
  }

  public Object[] createArray(Object value, String baseType)
    throws SQLException
  {
    String data = value.toString();

    if (data.length() < 2) return null;
    data = data.replaceFirst("(?i)^ARRAY", "");

    data = data.substring(1, data.length() - 1);
    CsvLineParser parser  = new CsvLineParser(',', '\'');
    parser.setQuoteEscaping(QuoteEscapeType.duplicate);
    parser.setLine(data);
    List<String> values = parser.getAllElements();

    ValueConverter converter = new ValueConverter();
    int jdbcType = getJdbcType(baseType);
    Object[] result = new Object[values.size()];
    try
    {
      for (int i=0; i < values.size(); i++)
      {
        result[i] = converter.convertValue(values.get(i), jdbcType);
      }
    }
    catch (ConverterException cve)
    {
      throw new SQLException("Invalid literal", cve);
    }
    return result;
  }

  protected int getJdbcType(String baseType)
  {
    switch (baseType)
    {
      case "VARCHAR":
      case "CHARACTER VARYING":
      case "LONGVARCHAR":
        return Types.VARCHAR;
      case "CHARACTER":
      case "CHAR":
        return Types.CHAR;
      case "CLOB":
        return Types.CLOB;
      case "BLOB":
      case "BINARY LARGE OBJECT":
        return Types.BLOB;
      case "VARBINARY":
        return Types.VARBINARY;
      case "BIT":
        return Types.BIT;
      case "BOOLEAN":
        return Types.BOOLEAN;
      case "INTEGER":
        return Types.INTEGER;
      case "TINYINT":
        return Types.TINYINT;
      case "BIGINT":
        return Types.BIGINT;
      case "SMALLINT":
        return Types.SMALLINT;
      case "TIMESTAMP":
        return Types.TIMESTAMP;
      case "TIME":
        return Types.TIME;
      case "DATE":
        return Types.DATE;
      case "REAL":
        return Types.REAL;
      case "DOUBLE":
        return Types.DOUBLE;
      case "NUMERIC":
      case "DECIMAL":
        return Types.DECIMAL;
    }
    return Types.OTHER;
  }

  public String getBaseType(ColumnIdentifier col)
  {
    String dbms = col.getDbmsType();
    int pos = dbms.indexOf("ARRAY");
    String baseType = dbms.substring(0, pos - 1);
    pos = baseType.indexOf('(');
    if (pos > 0)
    {
      baseType = baseType.substring(0, pos);
    }
    return baseType;
  }
}
