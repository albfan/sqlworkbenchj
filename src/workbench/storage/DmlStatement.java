/*
 * DmlStatement.java
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
package workbench.storage;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Array;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbSettings;
import workbench.db.DmlExpressionBuilder;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.sql.formatter.WbSqlFormatter;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 * A class to execute a SQL Statement and to create the statement
 * from a given list of values.
 *
 * @author  Thomas Kellerer
 */
public class DmlStatement
{
  private String sql;
  private List<ColumnData> values;
  private String chrFunc;
  private String concatString;
  private String concatFunction;
  private boolean formatInserts;
  private boolean formatUpdates;
  private boolean formatDeletes;
  private Map<String, Object> generatedKeys;

  /**
   *	Create a new DmlStatement with the given SQL template string
   *	and the given values.
   *
   *	The SQL string is expected to contain a ? for each value
   *	passed in aValueList. The SQL statement will be executed
   *	using a prepared statement.
   *
   * @param aStatement
   * @param aValueList
   */
  public DmlStatement(String aStatement, List<ColumnData> aValueList)
  {
    if (aStatement == null) throw new NullPointerException();

    int count = this.countParameters(aStatement);
    if (count > 0 && aValueList != null && count != aValueList.size())
    {
      throw new IllegalArgumentException("Number of parameter tokens does not match number of parameters passed.");
    }

    this.sql = aStatement;

    if (aValueList == null)
    {
      this.values = Collections.emptyList();
    }
    else
    {
      this.values = aValueList;
    }
    initFormattingFlags();
  }

  public void setFormatSql(boolean flag)
  {
    formatInserts = flag;
    formatUpdates = flag;
    formatDeletes = flag;
  }

  private void initFormattingFlags()
  {
    formatInserts = Settings.getInstance().getDoFormatInserts();
    formatUpdates = Settings.getInstance().getDoFormatUpdates();
    formatDeletes = Settings.getInstance().getDoFormatDeletes();
  }

  /**
   * Execute the statement as a prepared statement
   *
   * @param connection      the Connection to be used
   * @param retrieveKeys    a flag indicating if generated keys should be retrieved
   *
   * @return the number of rows affected
   *
   * @see DbSettings#getRetrieveGeneratedKeys()
   */
  public int execute(WbConnection connection, boolean retrieveKeys)
    throws SQLException
  {
    List<Closeable> streamsToClose = new ArrayList<>();

    PreparedStatement stmt = null;
    int rows = -1;

    DbSettings dbs = connection.getDbSettings();
    boolean useSetNull = dbs.useSetNull();
    boolean useXmlApi = dbs.useXmlAPI();
    boolean useClobSetString = dbs.useSetStringForClobs();
    boolean useBlobSetBytes = dbs.useSetBytesForBlobs();
    boolean padCharColumns = dbs.padCharColumns();

    DmlExpressionBuilder builder = DmlExpressionBuilder.Factory.getBuilder(connection);

    this.generatedKeys = null;

    try
    {
      if (retrieveKeys)
      {
        stmt = connection.getSqlConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      }
      else
      {
        stmt = connection.getSqlConnection().prepareStatement(sql);
      }

      for (int i=0; i < this.values.size(); i++)
      {
        ColumnData data = this.values.get(i);
        int type = data.getIdentifier().getDataType();
        String dbmsType = data.getIdentifier().getDbmsType();
        Object value = data.getValue();
        if (value == null)
        {
          if (useSetNull)
          {
            stmt.setNull(i+1, type);
          }
          else
          {
            stmt.setObject(i+1, null);
          }
        }
        else if (SqlUtil.isClobType(type) && value instanceof String)
        {
          if (useClobSetString)
          {
            stmt.setString(i+1, (String)value);
          }
          else
          {
            String s = (String)value;
            Reader in = new StringReader(s);
            stmt.setCharacterStream(i + 1, in, s.length());
            streamsToClose.add(in);
          }
        }
        else if (useXmlApi && SqlUtil.isXMLType(type) && !builder.isDmlExpressionDefined(dbmsType) && (value instanceof String || value instanceof Clob))
        {
          SQLXML xml = null;
          if (value instanceof Clob)
          {
            xml = JdbcUtils.createXML((Clob)value, connection);
          }
          else
          {
            xml = JdbcUtils.createXML((String)value, connection);
          }
          stmt.setSQLXML(i+ 1, xml);
        }
        else if (value instanceof File)
        {
          // Wenn storing data into a blob field, the GUI will
          // put a File object into the DataStore
          File f = (File)value;
          try
          {
            InputStream in = new FileInputStream(f);
            if (useBlobSetBytes)
            {
              byte[] array = FileUtil.readBytes(in);
              stmt.setBytes(i + 1, array);
            }
            else
            {
              stmt.setBinaryStream(i + 1, in, (int)f.length());
              streamsToClose.add(in);
            }
          }
          catch (IOException e)
          {
            throw new SQLException("Input file (" + f.getAbsolutePath() + ") for LOB not found!");
          }
        }
        else if (padCharColumns && (type == Types.CHAR || type == Types.NCHAR))
        {
          stmt.setString(i + 1, getCharValue(value, data.getIdentifier().getColumnSize()));
        }
        else if (type == Types.ARRAY)
        {
          handleArray(stmt, i+1, dbmsType, value, connection);
        }
        else
        {
          stmt.setObject(i + 1, value);
        }
      }
      rows = stmt.executeUpdate();
      if (retrieveKeys)
      {
        retrieveKeys(stmt);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("DmlStatement.execute()", "Error executing statement " + sql, e);
      throw e;
    }
    finally
    {
      FileUtil.closeStreams(streamsToClose);
      SqlUtil.closeStatement(stmt);
    }

    return rows;
  }

  private String getCharValue(Object value, int length)
  {
    if (value == null) return null;
    if (value instanceof String)
    {
      return StringUtil.padRight((String)value, length);
    }
    return value.toString();
  }

  private void retrieveKeys(PreparedStatement stmt)
  {
    ResultSet rs = null;
    try
    {
      rs = stmt.getGeneratedKeys();
      if (rs != null && rs.next())
      {
        ResultSetMetaData meta = rs.getMetaData();
        int colcount = meta.getColumnCount();
        this.generatedKeys = new HashMap<>(colcount);
        for (int col=1; col <= colcount; col++)
        {
          generatedKeys.put(meta.getColumnName(col), rs.getObject(col));
        }
        LogMgr.logDebug("DmlStatement.retrieveKeys()", "Driver returned generated keys: "  + generatedKeys);
      }
    }
    catch (Throwable e)
    {
      LogMgr.logWarning("DmlStatement.retrieveKeys()", "Could not retrieve generated key", e);
      this.generatedKeys = null;
    }
    finally
    {
      SqlUtil.closeResult(rs);
    }
  }

  public boolean hasGeneratedKeys()
  {
    return generatedKeys != null && generatedKeys.size() > 0;
  }

  /**
   * Return the generated key value for the specified column.
   *
   * If the column name supplied is not found in the returned keys, the first
   * key provided by the driver will be used.
   *
   * Postgres seems to be the only DBMS that can handle multiple auto-generated keys properly.
   * The JDBC driver will return the correct column name for each generated value therefor
   * the lookup for the column name will work properly.
   *
   * For all other DBMS the name returned from getGeneratedKeys() does not match the
   * table's column name, but as all the others only support a single auto generated
   * column (identity, auto_increment, ...) anyway this shouldn't be a problem
   *
   * @param colName  the colname for which the generated value should be returned
   * @return the generated value, may be null
   */
  public Object getGeneratedKey(String colName)
  {
    if (CollectionUtil.isEmpty(generatedKeys)) return null;
    Object value = this.generatedKeys.get(colName);

    if (value != null) return value;

    // the column name was not found so return the first value returned from the statement
    // this should only happen for DBMS that only support a single autoincrement/identity column
    // per table (MySQL, SQL Server, ...)
    if (generatedKeys.size() > 1)
    {
      LogMgr.logWarning("DmlStatement.getGeneratedKey()", "Multiple keys returned, but column name " + colName + " not found in returned key information: " + generatedKeys);
    }

    // iterator().next() is safe to use because we have at least one value in the map.
    return generatedKeys.values().iterator().next();
  }

  /**
   * Handle an array column.
   *
   * This is currently only tested on Postgres.
   * But as I'm not aware of any other DBMS with a decent array support anyway this shouldn't do much harm.
   *
   * @param stmt           the statement to use
   * @param index          the column index for the prepared statement
   * @param dbmsType       the DBMS typename as returned by the driver
   * @param value          the value to use
   * @param connection     the connection to use
   *
   * @throws SQLException
   */
  private void handleArray(PreparedStatement stmt, int index, String dbmsType, Object value, WbConnection connection)
    throws SQLException
  {
    if (value instanceof Array)
    {
      stmt.setArray(index, (Array)value);
      return;
    }

    String valueString = value.toString().trim();
    if (StringUtil.isEmptyString(valueString))
    {
      stmt.setNull(index, Types.ARRAY);
      return;
    }

    if (connection.getMetadata().isPostgres())
    {
      // this is an array of a custom type created with "CREATE TYPE"
      if (dbmsType.startsWith("_"))
      {
        // just assume the User entered a valid expression for this.
        stmt.setObject(index, valueString, Types.OTHER);
        return;
      }
      else if (valueString.startsWith("{") && valueString.endsWith("}"))
      {
        valueString = valueString.substring(1,valueString.length() - 1);
      }
    }

    WbStringTokenizer tok = new WbStringTokenizer(",", true, "\"'", false);
    tok.setDelimiterNeedsWhitspace(false);
    tok.setSourceString(valueString);

    List<Object> arrayValues = new ArrayList<>();
    while (tok.hasMoreTokens())
    {
      String s = tok.nextToken().trim();
      arrayValues.add(s);
    }

    Object[] data = arrayValues.toArray();

    try
    {
      if (connection.getDbSettings().supportsCreateArray())
      {
        String baseType = SqlUtil.getBaseTypeName(dbmsType);
        Array array = connection.getSqlConnection().createArrayOf(baseType, data);
        stmt.setArray(index, array);
      }
      else
      {
        stmt.setObject(index, data);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("DmlStatement.handleArray()", "Error setting array value", ex);
      stmt.setObject(index, data);
    }
  }

  public void setConcatString(String concat)
  {
    if (concat == null) return;
    this.concatString = concat;
    this.concatFunction = null;
  }

  public void setConcatFunction(String func)
  {
    if (func == null) return;
    this.concatFunction = func;
    this.concatString = null;
  }

  public void setChrFunction(String aFunc)
  {
    this.chrFunc = aFunc;
  }

  /**
   * Returns a "real" SQL Statement which can be executed
   * directly. The statement contains the parameter values
   * as literals. No placeholders are used.
   *
   * @param literalFormatter the Formatter for date and other literals
   * @return a SQL statement that can be executed
   */
  public CharSequence getExecutableStatement(SqlLiteralFormatter literalFormatter)
  {
    return getExecutableStatement(literalFormatter, null);
  }

  public CharSequence getExecutableStatement(SqlLiteralFormatter literalFormatter, WbConnection con)
  {
    CharSequence toUse = this.sql;
    if (this.values.size() > 0)
    {
      StringBuilder result = new StringBuilder(this.sql.length() + this.values.size() * 10);
      boolean inQuotes = false;
      int parmIndex = 0;
      for (int i = 0; i < this.sql.length(); ++i)
      {
        char c = sql.charAt(i);

        if (c == '\'') inQuotes = !inQuotes;
        if (c == '?' && !inQuotes && parmIndex < this.values.size())
        {
          ColumnData data = this.values.get(parmIndex);
          CharSequence literal = literalFormatter.getDefaultLiteral(data);
          if (this.chrFunc != null && SqlUtil.isCharacterType(data.getIdentifier().getDataType()))
          {
            literal = this.createInsertString(literal);
          }
          result.append(literal);
          parmIndex ++;
        }
        else
        {
          result.append(c);
        }
      }
      toUse = result;
    }

    boolean isInsert = toUse.subSequence(0, "INSERT".length()).equals("INSERT");
    boolean isUpdate = !isInsert && toUse.subSequence(0, "UPDATE".length()).equals("UPDATE");
    boolean isDelete = !isUpdate && !isInsert && toUse.subSequence(0, "DELETE".length()).equals("DELETE");

    if ((isInsert && formatInserts) || (isUpdate && formatUpdates) || (isDelete && formatDeletes))
    {
      WbSqlFormatter f = new WbSqlFormatter(toUse, con == null ? null : con.getDbId());
      if (con != null)
      {
        f.setCatalogSeparator(con.getMetadata().getCatalogSeparator());
      }
      return f.getFormattedSql();
    }
    return toUse;
  }

  private CharSequence createInsertString(CharSequence aValue)
  {
    if (aValue == null) return null;
    if (this.chrFunc == null) return aValue;
    boolean useConcatFunc = (this.concatFunction != null);

    if (!useConcatFunc && this.concatString == null) this.concatString = "||";
    StringBuilder result = new StringBuilder();
    boolean funcAppended = false;
    boolean quotePending = false;

    char last = 0;

    int len = aValue.length();
    for (int i=0; i < len; i++)
    {
      char c = aValue.charAt(i);
      if (c < 32)
      {
        if (useConcatFunc)
        {
          if (!funcAppended)
          {
            StringBuilder temp = new StringBuilder(concatFunction);
            temp.append('(');
            temp.append(result);
            result = temp;
            funcAppended = true;
          }
          if (quotePending && last >= 32)
          {
            result.append(",\'");
          }
          if (last >= 32) result.append('\'');
          result.append(',');
          result.append(this.chrFunc);
          result.append('(');
          result.append(NumberStringCache.getNumberString(c));
          result.append(')');
          quotePending = true;
        }
        else
        {
          if (last >= 32)
          {
            result.append('\'');
            result.append(this.concatString);
          }
          result.append(this.chrFunc);
          result.append('(');
          result.append(NumberStringCache.getNumberString(c));
          result.append(')');
          result.append(this.concatString);
          quotePending = true;
        }
      }
      else
      {
        if (quotePending)
        {
          if (useConcatFunc) result.append(',');
          result.append('\'');
        }
        result.append(c);
        quotePending = false;
      }
      last = c;
    }
    if (funcAppended)
    {
      result.append(')');
    }
    return result;
  }

  private int countParameters(String aSql)
  {
    if (aSql == null) return -1;
    boolean inQuotes = false;
    int count = 0;
    for (int i = 0; i < aSql.length(); i++)
    {
      char c = aSql.charAt(i);

      if (c == '\'') inQuotes = !inQuotes;
      if (c == '?' && !inQuotes)
      {
        count ++;
      }
    }
    return count;
  }

  public String getSql()
  {
    return sql;
  }

  @Override
  public String toString()
  {
    return getSql();
  }
}
