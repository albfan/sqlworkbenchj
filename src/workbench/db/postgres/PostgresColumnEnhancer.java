/*
 * PostgresColumnEnhancer.java
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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A column enhancer to update column definitions for PostgreSQL.
 *
 * The following additional information is updated:
 * <ul>
 * <li>The column collation if the the version is &gt;= 9.1</li>
 * <li>Storage definition</li>
 * <li>The array dimensions so that arrays are displayed correctly.</li>
 * <li>Inheritance information</li>
 * <li>Converts serial columns back to "serial"</li>
 * </ul>
 *
 * @author Thomas Kellerer
 */
public class PostgresColumnEnhancer
  implements ColumnDefinitionEnhancer
{
  public static final int STORAGE_PLAIN = 1;
  public static final int STORAGE_MAIN = 2;
  public static final int STORAGE_EXTERNAL = 3;
  public static final int STORAGE_EXTENDED = 4;

  public static final String PROP_SHOW_REAL_SERIAL_DEF = "workbench.db.postgresql.serial.show";

  @Override
  public void updateColumnDefinition(TableDefinition table, WbConnection conn)
  {
    if (JdbcUtils.hasMinimumServerVersion(conn, "8.0"))
    {
      readColumnInfo(table, conn);
    }

    updateSerials(table);
  }

  public static String getStorageOption(int storage)
  {
    switch (storage)
    {
      case STORAGE_EXTENDED:
        return "EXTENDED";
      case STORAGE_EXTERNAL:
        return "EXTERNAL";
      case STORAGE_MAIN:
        return "MAIN";
      case STORAGE_PLAIN:
        return "PLAIN";
    }
    return null;
  }

  private void updateSerials(TableDefinition table)
  {
    for (ColumnIdentifier col : table.getColumns())
    {
      String dbmsType = col.getDbmsType();
      String defaultValue = col.getDefaultValue();
      if (dbmsType.endsWith("serial") && defaultValue != null)
      {
        // The nextval() call is returned with a fully qualified name if the sequence is not in the current schema.
        // to avoid calling WbConnection.getCurrentSchema() for each default value
        // I'm just checking for a . in the default value which would indicate a fully qualified sequence
        String expectedDefault = "nextval('" ;
        if (defaultValue.indexOf('.') > -1)
        {
          expectedDefault += table.getTable().getRawSchema()+ ".";
        }
        expectedDefault += table.getTable().getRawTableName() + "_" + col.getColumnName() + "_seq'::regclass)";

        if (Settings.getInstance().getBoolProperty(PROP_SHOW_REAL_SERIAL_DEF, true) && (defaultValue.equals(expectedDefault)))
        {
          col.setDefaultValue(null);
        }
        else
        {
          if (dbmsType.equals("serial"))
          {
            col.setDbmsType("integer");
          }
          if (dbmsType.equals("bigserial"))
          {
            col.setDbmsType("bigint");
          }
        }
      }
    }
  }

  /**
   * Read additional information about the table's columns that are not provided by the JDBC API.
   *
   * The following additional information is retrieved from the database:
   * <ul>
   * <li>Collation</li>
   * <li>Inheritance information</li>
   * <li>Storage information</li>
   * <li>Original array dimensions</li>
   * </ul>
   *
   * All the above is retrieved from <tt>pg_attribute</tt> and <tt>pg_collation</tt>.
   *
   * @param table - the table for which the information should be retrieved
   * @param conn - the connection to use
   */
  private void readColumnInfo(TableDefinition table, WbConnection conn)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;

    boolean is91 = JdbcUtils.hasMinimumServerVersion(conn, "9.1");

    String sql =
      "select att.attname, \n" +
      (is91 ?
      "       col.collcollate, \n " :
      "       null as collcollate, \n" ) +
      "       case \n" +
      "          when att.attlen = -1 then att.attstorage \n" +
      "          else null \n" +
      "       end as attstorage, \n" +
      "       att.attinhcount, \n" +
      "       att.attndims,\n " +
      "       pg_catalog.format_type(att.atttypid, NULL) as display_type \n" +
      "from pg_attribute att  \n" +
      "  join pg_class tbl on tbl.oid = att.attrelid   \n" +
      "  join pg_namespace ns on tbl.relnamespace = ns.oid   \n" +
      (is91 ?
      "  left join pg_collation col on att.attcollation = col.oid \n" : "" ) +
      "where tbl.relname = ? \n" +
      "  and ns.nspname = ? \n" +
      "  and not att.attisdropped";

    String tname = table.getTable().getRawTableName();
    String tschema = table.getTable().getRawSchema();
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("PostgresColumnEnhancer.readColumnInfo()", "Retrieving column information using :\n" + SqlUtil.replaceParameters(sql, tname, tschema));
    }

    try
    {
      stmt = conn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, tname);
      stmt.setString(2, tschema);

      rs = stmt.executeQuery();
      while (rs.next())
      {
        String colname = rs.getString(1);
        String collation = rs.getString(2);
        String storage = rs.getString(3);
        int ancestorCount = rs.getInt(4);

        int arrayDims = rs.getInt(5);
        if (rs.wasNull()) arrayDims = -1;
        String formattedType = rs.getString(6);

        ColumnIdentifier col = table.findColumn(colname);
        if (col == null) continue; // should not happen

        if (StringUtil.isNonEmpty(collation))
        {
          col.setCollation(collation);
          col.setCollationExpression(" COLLATE \"" + collation + "\"");
        }

        if (arrayDims > 0)
        {
          updateArrayType(col, formattedType, arrayDims);
        }

        col.setInherited(ancestorCount > 0);

        if (storage != null && !storage.isEmpty())
        {
          switch (storage.charAt(0))
          {
            case 'p':
              col.setPgStorage(STORAGE_PLAIN);
              break;
            case 'm':
              col.setPgStorage(STORAGE_MAIN);
              break;
            case 'e':
              col.setPgStorage(STORAGE_EXTERNAL);
              break;
            case 'x':
              col.setPgStorage(STORAGE_EXTENDED);
              break;
          }
        }
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("PostgresColumnEnhancer.readColumnInfo()", "Could not read column information using: " + SqlUtil.replaceParameters(sql, tname, tschema), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  private void updateArrayType(ColumnIdentifier column, String formattedType, int numDims)
  {
    if (numDims <= 0) return;

    String type = StringUtil.replace(formattedType, "character varying", "varchar");
    for (int i = 0; i < numDims - 1; i++)
    {
      type += "[]";
    }
    column.setDbmsType(type);
  }

}
