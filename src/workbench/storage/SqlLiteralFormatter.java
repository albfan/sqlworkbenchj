/*
 * SqlLiteralFormatter.java
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

import java.io.File;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;
import java.util.TreeMap;

import workbench.interfaces.DataFileWriter;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.exporter.InfinityLiterals;
import workbench.db.postgres.HstoreSupport;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;

/**
 *
 * @author  Thomas Kellerer
 */
public class SqlLiteralFormatter
{
  /**
   * The "product" for the jdbc date literal format
   */
  public static final String JDBC_DATE_LITERAL_TYPE = "jdbc";

  /**
   * The "product" for the ansi date literal format
   */
  public static final String ANSI_DATE_LITERAL_TYPE = "ansi";

  /**
   * The "product" for the standard date literal format
   */
  public static final String STANDARD_DATE_LITERAL_TYPE = "default";

  /**
   * The "product" for the dbms specific date literal format
   */
  public static final String DBMS_DATE_LITERAL_TYPE = "dbms";

  private WbDateFormatter dateFormatter;
  private WbDateFormatter timestampFormatter;
  private WbDateFormatter timeFormatter;
  private BlobLiteralFormatter blobFormatter;
  private DataFileWriter blobWriter;
  private DataFileWriter clobWriter;
  private boolean treatClobAsFile = false;
  private String clobEncoding = Settings.getInstance().getDefaultFileEncoding();
  private boolean isDbId;
  private boolean isOracle;
  private DbSettings dbSettings;
  private boolean checkDBMSTypes;

  private final Map<String, WbDateFormatter> dbmsFormatters = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

  /**
   * Create a new formatter with default formatting.
   */
  public SqlLiteralFormatter()
  {
    this((String)null);
  }

  /**
   * Create  new formatter specifically for the DBMS identified
   * by the connection.
   *
   * The type of date literals used, can be changed to a different
   * "product" using {@link #setDateLiteralType(String)}
   *
   * @param con the connection identifying the DBMS
   *
   * @see workbench.db.DbMetadata#getProductName()
  */
  public SqlLiteralFormatter(WbConnection con)
  {
    String dbid = null;
    isDbId = false;
    if (con != null && con.getMetadata() != null)
    {
      dbid = con.getMetadata().getDbId();
      isDbId = true;
      dbSettings = con.getDbSettings();
      isOracle = con.getMetadata().isOracle();
    }
    setDateLiteralType(dbid);
    checkDBMSTypes = true;
  }

  public SqlLiteralFormatter(String dbid)
  {
    isDbId = true;
    isOracle = DbMetadata.DBID_ORA.equals(dbid);
    setDateLiteralType(dbid);
    checkDBMSTypes = true;
  }

  /**
   * Select the DBMS specific date literal according to the
   * DBMS identified by the connection.
   * @param con the connection to identify the DBMS
   * @see #setDateLiteralType(String)
   */
  public void setProduct(WbConnection con)
  {
    if (con != null)
    {
      String dbid = con.getMetadata().getDbId();
      isDbId = true;
      isOracle = con.getMetadata().isOracle();
      dbSettings = con.getDbSettings();
      setDateLiteralType(dbid);
    }
    checkDBMSTypes = true;
  }

  /**
   * Use a specific product name for formatting date and timestamp values.
   *
   * This call is ignored if the passed value is DBMS and this instance has
   * been initialised with a Connection (thus the DBMS specific formatter is already
   * selected).
   *
   * @param type the literal type to use. This is the key to the map defining the formats
   *
   * @see workbench.db.DbMetadata#getProductName()
   */
  public final void setDateLiteralType(String type)
  {
    checkDBMSTypes = false;
    dbmsFormatters.clear();

    // If the DBMS specific format is selected and we already have a DBID
    // then this call is simply ignored.
    if (DBMS_DATE_LITERAL_TYPE.equalsIgnoreCase(type))
    {
      if (this.isDbId)
      {
        return;
      }
      // no DBMS selected, using null for the type will select ANSI literals
      type = null;
    }

    dateFormatter = createFormatter(type, "date", "''yyyy-MM-dd''");
    timestampFormatter = createFormatter(type, "timestamp", "''yyyy-MM-dd HH:mm:ss''");
    timeFormatter = createFormatter(type, "time", "''HH:mm:ss''");
  }

  public void setInfinityLiterals(InfinityLiterals literals)
  {
    dateFormatter.setInfinityLiterals(literals);
    timestampFormatter.setInfinityLiterals(literals);
  }

  /**
   * Do not write BLOBs as SQL Literals.
   */
  public void noBlobHandling()
  {
    this.blobWriter = null;
    this.blobFormatter = null;
  }

  public void setBlobFormat(BlobLiteralType type)
  {
    blobWriter = null;
    blobFormatter = BlobFormatterFactory.createInstance(type);
  }
  /**
   * Create ANSI compatible BLOB literals
   */
  public void createAnsiBlobLiterals()
  {
    blobFormatter = BlobFormatterFactory.createAnsiFormatter();
    this.blobWriter = null;
  }

  /**
   * Create BLOB literals that are compatible with the
   * DBMS identified by the connection.
   * If no specific formatter for the given DMBS can be found, the generic
   * ANSI formatter will be used.
   * @param con the connection (i.e. the DBMS) for which the literals should be created
   */
  public void createDbmsBlobLiterals(WbConnection con)
  {
    if (con != null)
    {
      blobFormatter = BlobFormatterFactory.createInstance(con.getMetadata());
      this.blobWriter = null;
    }
  }

  /**
   * Create external BLOB files instead of BLOB literals.
   *
   * This will reset any literal formatting selected with createAnsiBlobLiterals()
   * or createDbmsBlobLiterals().
   * The generated SQL Literal will be compatible with SQL Workbench extended
   * blob handling and will generate literals in the format <code>{$blobfile=...}</code>
   *
   * @param bw the writer to be used for writing the BLOB content
   */
  public void createBlobFiles(DataFileWriter bw)
  {
    this.blobFormatter = null;
    this.blobWriter = bw;
  }

  /**
   * Create external files for CLOB columns (instead of String literals).
   * The generated SQL Literal will be compatible with SQL Workbench extended
   * LOB handling and will generate literals in the format <code>{$clobfile='...' encoding='encoding'}</code>
   *
   * @param writer the writer to be used for writing the BLOB content
   * @param encoding the encoding to be used to write the CLOB files
   */
  public void setTreatClobAsFile(DataFileWriter writer, String encoding)
  {
    this.treatClobAsFile = true;
    this.clobWriter = writer;
    if (!StringUtil.isEmptyString(encoding)) this.clobEncoding = encoding;
  }

  private static String getPattern(String format, String dataType)
  {
    String key = "workbench.sql.literals." + (format == null ? STANDARD_DATE_LITERAL_TYPE : format) + "." + dataType + ".pattern";
    String pattern = Settings.getInstance().getProperty(key, null);
    if (pattern != null && pattern.startsWith("${") && pattern.endsWith("}"))
    {
      format = pattern.substring(2, pattern.length() - 1);
      key = "workbench.sql.literals." + format + "." + dataType + ".pattern";
      pattern = Settings.getInstance().getProperty(key, null);
    }
    return pattern;
  }

  public static WbDateFormatter createFormatter(String format, String dataType, String defaultPattern)
  {
    WbDateFormatter f = null;
    String pattern = getPattern(format, dataType);
    try
    {
      if (pattern == null)
      {
        String key = "workbench.sql.literals." + STANDARD_DATE_LITERAL_TYPE + "." + dataType + ".pattern";
        pattern = Settings.getInstance().getProperty(key, defaultPattern);
      }

      f = new WbDateFormatter(pattern);
    }
    catch (Exception e)
    {
      LogMgr.logError("SqlLiteralFormatter.createFormatter()", "Could not create formatter with pattern [" + pattern + "], using default [" + defaultPattern + "]", e);
      f = new WbDateFormatter(defaultPattern);
    }
    return f;
  }

  private String quoteString(int jdbcType, String t)
  {
    if (t == null) return t;
    String prefix;
    if (jdbcType == Types.NVARCHAR || jdbcType == Types.NCHAR || jdbcType == Types.NCLOB || jdbcType == Types.LONGNVARCHAR)
    {
      prefix = "N'";
    }
    else
    {
       prefix = "'";
    }
    return prefix + t.replace("'", "''") + "'";
  }

  /**
   * Return the default literal for the given column data.
   *
   * Date and Timestamp data will be formatted according to the
   * syntax defined by the {@link #setDateLiteralType(String)} method
   * or through the connection provided in the constructor.
   *
   * @param data the data to be converted into a literal.
   * @return the literal to be used in a SQL statement
   * @see #setDateLiteralType(String)
   */
  public CharSequence getDefaultLiteral(ColumnData data)
  {
    Object value = data.getValue();
    if (value == null) return "NULL";

    int type = data.getIdentifier().getDataType();
    String dbmsType = data.getIdentifier().getDbmsType();

    if (type == ColumnIdentifier.NO_TYPE_INFO) return value.toString();

    String formatted = formatDbmsType(data);
    if (formatted != null)
    {
      return formatted;
    }

    if (type == Types.STRUCT)
    {
      return value.toString();
    }
    else if (value instanceof String)
    {
      String t = (String)value;
      if (this.treatClobAsFile  && clobWriter != null && SqlUtil.isClobType(type, dbmsType, dbSettings))
      {
        try
        {
          File f = clobWriter.generateDataFileName(data);
          clobWriter.writeClobFile(t, f, this.clobEncoding);
          return "{$clobfile='" + f.getName() + "' encoding='" + this.clobEncoding + "'}";
        }
        catch (Exception e)
        {
          LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Could not write CLOB file", e);
          return quoteString(type, t);
        }
      }
      else
      {
        return quoteString(type, t);
      }
    }
    else if (value instanceof Time)
    {
      return this.timeFormatter.formatTime((Time)value);
    }
    else if (value instanceof Timestamp)
    {
      return fixInfinity(this.timestampFormatter.formatTimestamp((Timestamp)value));
    }
    else if (value instanceof java.sql.Date)
    {
      return fixInfinity(this.dateFormatter.formatDate((java.sql.Date)value));
    }
    else if (value instanceof java.util.Date)
    {
      return fixInfinity(this.dateFormatter.formatUtilDate((java.util.Date)value));
    }
    else if (value instanceof File)
    {
      File f = (File)value;
      String path = null;
      try
      {
        path = f.getCanonicalPath();
      }
      catch (Exception e)
      {
        path = f.getAbsolutePath();
      }
      if (SqlUtil.isBlobType(type))
      {
        return "{$blobfile='" + path + "'}";
      }
      else if (SqlUtil.isClobType(type))
      {
        return "{$clobfile='" + path + "' encoding='" + this.clobEncoding + "'}";
      }
    }
    else if (type == java.sql.Types.BIT && "bit".equalsIgnoreCase(dbmsType))
    {
      // this is for MS SQL Server
      // we cannot convert all values denoted as Types.BIT to 0/1 as
      // e.g. Postgres only accepts the literals true/false for boolean columns
      // which are reported as Types.BIT as well.
      // that's why I compare to the DBMS data type bit (hoping that
      // other DBMS's that are also using 'bit' work the same way)
      try
      {
        boolean flag = ((java.lang.Boolean)value).booleanValue();
        return (flag ? "1" : "0");
      }
      catch (Exception ex)
      {
        // no boolean value -> don't know what to do with this
        return value.toString();
      }
    }
    else if (SqlUtil.isBlobType(type))
    {
      if (blobWriter != null)
      {
        try
        {
          File f = blobWriter.generateDataFileName(data);
          blobWriter.writeBlobFile(value, f);
          return "{$blobfile='" + f.getName() + "'}";
        }
        catch (Exception e)
        {
          LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Could not write BLOB file", e);
        }
      }
      else if (blobFormatter != null)
      {
        try
        {
          return blobFormatter.getBlobLiteral(value);
        }
        catch (Exception e)
        {
          LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Error converting BLOB value", e);
        }
      }
    }
    else if ("hstore".equalsIgnoreCase(dbmsType) && value instanceof Map)
    {
      return HstoreSupport.getLiteral((Map)value);
    }
    else if (type == Types.ARRAY)
    {
      if (isOracle) return value.toString();

      // asume the value can be used inside a string literal
      return "'" + value.toString() + "'";
    }
    else if (type == Types.OTHER && "uuid".equalsIgnoreCase(dbmsType))
    {
      // this is for Postgres
      return quoteString(type, value.toString());
    }

    // Fallback, let the JDBC driver format the value
    return value.toString();
  }

  private String fixInfinity(String input)
  {
    if (input == null) return null;
    if (InfinityLiterals.isPGLiteral(input))
    {
      return "'" + input + "'";
    }
    return input;
  }

  private boolean isDateTime(int jdbcType)
  {
    return jdbcType == Types.DATE || jdbcType == Types.TIMESTAMP || jdbcType == Types.TIME ||
           jdbcType == Types.TIMESTAMP_WITH_TIMEZONE || jdbcType == Types.TIME_WITH_TIMEZONE;
  }

  private String cleanupTypeName(String type)
  {
    return StringUtil.replace(type, " ", "_").toLowerCase();
  }

  private String getDbmsFormat(String dbmsTypeName)
  {
    if (dbSettings == null) return null;

    return getPattern(dbSettings.getDbId(), cleanupTypeName(dbmsTypeName));
  }

  private synchronized String formatDbmsType(ColumnData data)
  {
    int type = data.getIdentifier().getDataType();
    if (!checkDBMSTypes || !isDateTime(type))
    {
      return null;
    }

    String dbmsType = data.getIdentifier().getDbmsType();

    WbDateFormatter formatter = dbmsFormatters.get(dbmsType);
    String format = null;

    if (formatter == null)
    {
      format = getDbmsFormat(dbmsType);
      if (StringUtil.isEmptyString(format))
      {
        return null;
      }

      format = format.replace("${dbtype}", dbmsType);

      try
      {
        formatter = new WbDateFormatter(format, true);
        dbmsFormatters.put(dbmsType, formatter);
      }
      catch (Exception ex)
      {
        LogMgr.logWarning("SqlLiteralFormatter.formatDbmsType()", "Could not create formatter for type \"" + dbmsType + "\" using '" + format + "'", ex);
      }
    }

    if (formatter == null)
    {
      return null;
    }

    Object value = data.getValue();

    try
    {

      if (value instanceof Time)
      {
        return formatter.formatTime((Time)value);
      }
      else if (value instanceof Timestamp)
      {
        return formatter.formatTimestamp((Timestamp)value);
      }
      else if (value instanceof java.sql.Date)
      {
        return formatter.formatDate((java.sql.Date)value);
      }
      else if (value instanceof java.util.Date)
      {
        return formatter.formatUtilDate((java.util.Date)value);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("SqlLiteralFormatter.formatDbmsType()", "Could not format type \"" + dbmsType + "\" using '" + format + "'", ex);
    }
    return null;
  }

}
