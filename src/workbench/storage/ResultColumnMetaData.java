/*
 * ResultColumnMetaData.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.Alias;
import workbench.util.CollectionUtil;
import workbench.util.SelectColumn;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.TableAlias;

/**
 * A class to retrieve additional column meta data for result (query)
 * columns from a datastore.
 *
 * Currently this only retrieves the remarks for queries based on a single
 * table select statement
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaData
{
  private List<String> tables;
  private List<String> columns;
  private WbConnection connection;

  public ResultColumnMetaData(DataStore ds)
  {
    this(ds.getGeneratingSql(), ds.getOriginalConnection());
  }

  public ResultColumnMetaData(String sql, WbConnection conn)
  {
    connection = conn;
    if (StringUtil.isBlank(sql)) return;

    List<Alias> list = SqlUtil.getTables(sql, true, conn);
    if (CollectionUtil.isEmpty(list)) return;

    tables = new ArrayList<>(list.size());
    for (Alias a : list)
    {
      tables.add(a.getName());
    }
    columns = SqlUtil.getSelectColumns(sql, true, conn);
  }

  public void retrieveColumnRemarks(ResultInfo info)
    throws SQLException
  {
    retrieveColumnRemarks(info, null);
  }

  public void retrieveColumnRemarks(ResultInfo info, TableDefinition tableDef)
    throws SQLException
  {
    if (CollectionUtil.isEmpty(columns)) return;

    DbMetadata meta = connection.getMetadata();

    Map<String, TableDefinition> tableDefs = new HashMap<>(tables.size());
    for (String table : tables)
    {
      if (StringUtil.isBlank(table)) continue;

      if (tableDef != null && tableDef.getTable().getTableName().equals(table))
      {
        tableDefs.put(tableDef.getTable().getTableName().toLowerCase(), tableDef);
      }
      else
      {
        TableAlias alias = new TableAlias(table);
        TableIdentifier tbl = new TableIdentifier(alias.getObjectName(), connection);
        TableDefinition def = meta.getTableDefinition(tbl);
        tableDefs.put(alias.getNameToUse().toLowerCase(), def);
      }
    }

    for (String col : columns)
    {
      SelectColumn c = new SelectColumn(col);
      String table = c.getColumnTable();
      if (table == null)
      {
        TableAlias alias = new TableAlias(tables.get(0));
        table = alias.getNameToUse();
      }
      if (table == null) continue;

      TableDefinition def = tableDefs.get(table.toLowerCase());
      if (c.getObjectName().equals("*"))
      {
        processTableColumns(def, info);
      }
      else if (def != null)
      {
        ColumnIdentifier id = def.findColumn(c.getObjectName());
        setColumnComment(def, id, info);
      }
    }
  }

  private void processTableColumns(TableDefinition def, ResultInfo info)
  {
    for (ColumnIdentifier col : def.getColumns())
    {
      setColumnComment(def, col, info);
    }
  }

  private void setColumnComment(TableDefinition def, ColumnIdentifier col, ResultInfo info)
  {
    if (def == null) return;
    if (col == null) return;

    int index = info.findColumn(col.getColumnName());
    if (index > -1)
    {
      info.getColumn(index).setComment(col.getComment());
      info.getColumn(index).setSourceTableName(def.getTable().getTableName());
    }
  }
}
