/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DB2TempTableReader
  implements ObjectListExtender
{
  public static final String TABLE_TYPE = "TEMPORARY TABLE";

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded("TABLE", requestedTypes)) return false;
    if (!DbMetadata.typeIncluded(TABLE_TYPE, requestedTypes)) return false;

    String sql =
      "select tabschema, \n" +
      "       tabname, \n" +
      "       remarks \n" +
      "from syscat.tables \n" +
      "where type = 'G' ";

    String escape = " escape '" + con.getMetadata().getSearchStringEscape() + "'";
    int schemaIndex = 0;
    int tableIndex = 0;
    if (schema != null)
    {
      sql += "\n  and tabschema like ? " + escape;
      schemaIndex = 1;
    }

    if (objects != null)
    {
      sql += "\n  and tabname like ? " + escape;
      tableIndex = schemaIndex + 1;
    }

    sql += "\nfor read only";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2TempTableReader.extendObjectList()", "Reading temp tables using:\n" + SqlUtil.replaceParameters(sql, schema, objects));
		}

    PreparedStatement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.getSqlConnection().prepareStatement(sql);
      if (schemaIndex > 0)
      {
        stmt.setString(schemaIndex, schema);
      }
      if (tableIndex > 0)
      {
        stmt.setString(tableIndex, objects);
      }
      rs = stmt.executeQuery();
      while (rs.next())
      {
        int row = result.addRow();
        String tabSchema = rs.getString(1);
        String tabName = rs.getString(2);
        String remarks = rs.getString(3);

        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, tabSchema);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, tabName);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remarks);
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, TABLE_TYPE);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("Db2TempTableReader.extendObjectList()", "Error reading temp tables using:\n" + SqlUtil.replaceParameters(sql, schema, objects), ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return false;
  }

  @Override
  public List<String> supportedTypes()
  {
    return Collections.singletonList(TABLE_TYPE);
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public boolean handlesType(String type)
  {
    return TABLE_TYPE.equalsIgnoreCase(type);
  }

  @Override
  public boolean handlesType(String[] types)
  {
    if (types == null) return true;
    for (String type : types)
    {
      if (type.equalsIgnoreCase(TABLE_TYPE)) return true;
    }
    return false;
  }

  @Override
  public DataStore getObjectDetails(WbConnection con, DbObject object)
  {
    try
    {
      return con.getMetadata().getObjectDetails((TableIdentifier)object);
    }
    catch (Exception ex)
    {
      LogMgr.logError("Db2TempTablereader.getObjectDetails()", "Could not retrieve object details", ex);
      return null;
    }
  }

  @Override
  public TableIdentifier getObjectDefinition(WbConnection con, DbObject name)
  {
    if (name instanceof TableIdentifier)
    {
      return (TableIdentifier)name;
    }
    return null;
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
    List<ColumnIdentifier> columns = getColumns(con, object);
    return builder.getTableSource((TableIdentifier)object, columns);
  }

  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject object)
  {
    try
    {
      return con.getMetadata().getTableColumns((TableIdentifier)object);
    }
    catch (SQLException ex)
    {
      LogMgr.logError("Db2TempTablereader.getColumns()", "Could not retrieve columns", ex);
      return null;
    }
  }

  @Override
  public boolean hasColumns()
  {
    return true;
  }

}
