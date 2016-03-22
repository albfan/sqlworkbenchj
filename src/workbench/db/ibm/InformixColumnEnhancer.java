/*
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.log.LogMgr;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class InformixColumnEnhancer
  implements ColumnDefinitionEnhancer
{
  private final Map<Integer, String> limits = new HashMap<>(11);

  public InformixColumnEnhancer()
  {
    limits.put(0, "YEAR");
    limits.put(2, "MONTH");
    limits.put(4, "DAY");
    limits.put(6, "HOUR");
    limits.put(8, "MINUTE");
    limits.put(10, "SECOND");
    limits.put(11, "FRACTION(1)");
    limits.put(12, "FRACTION(2)");
    limits.put(13, "FRACTION(3)");
    limits.put(14, "FRACTION(4)");
    limits.put(15, "FRACTION(5)");
  }

  @Override
  public void updateColumnDefinition(TableDefinition table, WbConnection conn)
  {
    String typeNames = conn.getDbSettings().getProperty("qualifier.typenames", "datetime,interval");

    Set<String> types = CollectionUtil.caseInsensitiveSet();
    types.addAll(StringUtil.stringToList(typeNames, ",", true, true, false, false));

    boolean checkRequired = false;

    for (ColumnIdentifier col : table.getColumns())
    {
      String plainType = SqlUtil.getPlainTypeName(col.getDbmsType());
      if (types.contains(plainType))
      {
        checkRequired = true;
      }

      int type = col.getDataType();
      String val = col.getDefaultValue();
      if (defaultNeedsQuotes(val, type, plainType))
      {
        val = "'" + val + "'";
        col.setDefaultValue(val);
      }
    }

    if (checkRequired)
    {
      updateDateColumns(table, conn);
    }
  }

  private boolean defaultNeedsQuotes(String defaultValue, int jdbcType, String typeName)
  {
    if (defaultValue == null) return false;
    if (SqlUtil.isNumberType(jdbcType)) return false;
    if (SqlUtil.isCharacterType(jdbcType)) return true;
    if ("boolean".equalsIgnoreCase(typeName)) return true;
    if (SqlUtil.isDateType(jdbcType))
    {
      Set<String> keyWords = CollectionUtil.caseInsensitiveSet("today", "current");
      if (keyWords.contains(defaultValue)) return false;
      return true;
    }
    return false;
  }

  private void updateDateColumns(TableDefinition table, WbConnection conn)
  {
    String catalog = table.getTable().getRawCatalog();

    String systemSchema = conn.getDbSettings().getProperty("systemschema", "informix");
    TableIdentifier sysTabs = new TableIdentifier(catalog, systemSchema, "systables");
    TableIdentifier sysCols = new TableIdentifier(catalog, systemSchema, "syscolumns");

    String systables = sysTabs.getFullyQualifiedName(conn);
    String syscolumns = sysCols.getFullyQualifiedName(conn);

    String typeValues = conn.getDbSettings().getProperty("qualifier.typevalues", "10,14,266,270");

    String sql =
      "select c.colname, c.collength \n" +
      "from " + systables + " t \n"+
      "  join " + syscolumns + " c on t.tabid = c.tabid \n" +
      "where t.tabname = ? \n" +
      "  and t.owner = ? \n" +
      "  and c.coltype in (" + typeValues + ")";

    String tablename = table.getTable().getRawTableName();
    String schema = table.getTable().getRawSchema();

    LogMgr.logDebug("InformixColumnEnhancer.updateDateColumns()", "Query to retrieve column details:\n" + SqlUtil.replaceParameters(sql, tablename, schema));

    PreparedStatement stmt = null;
    ResultSet rs = null;

    Map<String, ColumnIdentifier> cols = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    for (ColumnIdentifier col : table.getColumns())
    {
      cols.put(col.getColumnName(), col);
    }

    try
    {
      stmt = conn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, tablename);
      stmt.setString(2, schema);
      rs = stmt.executeQuery();

      while (rs.next())
      {
        String colname = rs.getString(1);
        int colLength = rs.getInt(2);
        ColumnIdentifier col = cols.get(colname);
        if (col != null)
        {
          String typeDesc = getQualifier(colLength);

          String dbms = SqlUtil.getPlainTypeName(col.getDbmsType());
          String newType = dbms + " " + typeDesc;
          LogMgr.logDebug("InformixColumnEnhancer.updateDateColumns()",
            "Column " + tablename + "." + colname + " has collength of: " + colLength + ". Changing type '" + col.getDbmsType() + "' to '" + newType + "'");
          col.setDbmsType(newType);
        }
        else
        {
          LogMgr.logError("InformixColumnEnhancer.updateDateColumns()","The query returned a column name (" + colname + ") that was not part of the passed table definition!",null);
        }
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("InformixColumnEnhancer.updateDateColumns()", "Error retrieving datetime qualifiers using:\n" +
        SqlUtil.replaceParameters(sql, tablename, schema), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  String getQualifier(int collength)
  {
    int len = collength / 256;
    int baseValue = collength - (len * 256);
    int from = baseValue / 16;
    int to = baseValue - (from * 16);

    return limits.get(from) + " TO " + limits.get(to);
  }
}
