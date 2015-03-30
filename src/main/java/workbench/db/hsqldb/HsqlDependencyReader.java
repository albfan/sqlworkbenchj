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
package workbench.db.hsqldb;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;

import workbench.gui.dbobjects.objecttree.DbObjectSorter;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlDependencyReader
  implements DependencyReader
{

  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view");

  private final String searchUsedBy =
      "select vtu.table_catalog, vtu.table_schema, vtu.table_name, \n" +
      "       case when v.table_name is not null then 'VIEW' " +
      "            when t.table_name is not null then 'TABLE' \n" +
      "       end as type\n " +
      "from information_schema.view_table_usage vtu \n" +
      "  left join information_schema.views v on (v.table_catalog, v.table_schema, v.table_name) = (vtu.table_catalog, vtu.table_schema, vtu.table_name) \n" +
      "  left join information_schema.tables t on (t.table_catalog, t.table_schema, t.table_name) = (vtu.table_catalog, vtu.table_schema, vtu.table_name) \n" +
      "where vtu.view_catalog = ? \n" +
      "  and vtu.view_schema = ? \n" +
      "  and vtu.view_name = ? ";

   private final String searchUsedSql =
      "select vtu.view_catalog, vtu.view_schema, vtu.view_name, \n" +
      "       case when v.table_name is not null then 'VIEW' " +
      "            when t.table_name is not null then 'TABLE' \n" +
      "       end as type\n " +
      "from information_schema.view_table_usage vtu \n" +
      "  left join information_schema.views v on (v.table_catalog, v.table_schema, v.table_name) = (vtu.table_catalog, vtu.table_schema, vtu.table_name) \n" +
      "  left join information_schema.tables t on (t.table_catalog, t.table_schema, t.table_name) = (vtu.table_catalog, vtu.table_schema, vtu.table_name) \n" +
      "where vtu.table_catalog = ? \n" +
      "  and table_schema = ? \n" +
      "  and table_name = ?";

  public HsqlDependencyReader()
  {
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    return retrieveObjects(connection, base, searchUsedBy);
  }

  @Override
  public List<DbObject> getUsedBy(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    return retrieveObjects(connection, base, searchUsedSql);
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s = SqlUtil.replaceParameters(sql, base.getCatalog(), base.getSchema(), base.getObjectName(), base.getObjectType());
			LogMgr.logDebug("HsqlDependencyReader.retrieveObjects()", "Retrieving dependent objects using query:\n" + s);
		}

    try
    {
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, base.getCatalog());
      pstmt.setString(2, base.getSchema());
      pstmt.setString(3, base.getObjectName());

      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String catalog = rs.getString(1);
        String schema = rs.getString(2);
        String name = rs.getString(3);
        String type = rs.getString(4);
        TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
        tbl.setNeverAdjustCase(true);
        tbl.setType(type);
        result.add(tbl);
      }
    }
    catch (Exception ex)
    {
			String s = SqlUtil.replaceParameters(sql, base.getCatalog(), base.getSchema(), base.getObjectName(), base.getObjectType());
      LogMgr.logError("HsqlDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    DbObjectSorter.sort(result, true);
    return result;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

}
