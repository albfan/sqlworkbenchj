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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.SequenceDefinition;
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
public class PostgresDependencyReader
  implements DependencyReader
{

  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view");

  @Override
  public List<DbObject> getObjectDependencies(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();


    String viewSql =
      "select table_schema, \n" +
      "       table_name, \n" +
      "       'VIEW' as type, \n" +
      "       obj_description((table_schema||'.'||table_name)::regclass) as remarks\n" +
      "from information_schema.view_table_usage \n" +
      "where (view_schema, view_name) = (?, ?)" +
      "order by view_schema, view_name";

    String baseSql =
      "SELECT DISTINCT nsp2.nspname, dependee.relname,\n" +
      "       CASE dependee.relkind \n" +
      "          WHEN 'r' THEN 'TABLE'::text \n" +
      "          WHEN 'i' THEN 'INDEX'::text \n" +
      "          WHEN 'S' THEN 'SEQUENCE'::text \n" +
      "          WHEN 'v' THEN 'VIEW'::text \n" +
      "          WHEN 'm' THEN 'MATERIALIZED VIEW'::text \n" +
      "          WHEN 'c' THEN 'TYPE'::text      -- COMPOSITE type \n" +
      "          WHEN 't' THEN 'TOAST'::text \n" +
      "          WHEN 'f' THEN 'FOREIGN TABLE'::text \n" +
      "       END AS object_type, \n" +
      "       obj_description(dependee.oid) as remarks\n" +
      "FROM pg_depend dep  \n" +
      "  JOIN pg_rewrite ON dep.objid = pg_rewrite.oid  \n" +
      "  JOIN pg_class as dependee ON pg_rewrite.ev_class = dependee.oid  \n" +
      "  JOIN pg_class as dependent ON dep.refobjid = dependent.oid  \n" +
      "  JOIN pg_namespace nsp on dependent.relnamespace = nsp.oid \n" +
      "  JOIN pg_namespace nsp2 on dependee.relnamespace = nsp2.oid  \n" +
      "WHERE dependent.oid <> dependee.oid \n" +
      "  AND nsp.nspname = ? \n" +
      "  AND dependent.relname = ?";

    String sql = baseSql;
    if (connection.getMetadata().isViewType(base.getObjectType()))
    {
      sql = viewSql;
    }
    return retrieveObjects(connection, base, sql);
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s = SqlUtil.replaceParameters(sql, base.getSchema(), base.getObjectName(), base.getObjectType());
			LogMgr.logDebug("PostgresDependencyReader.retrieveObjects()", "Retrieving dependent objects using query:\n" + s);
		}

    try
    {
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, base.getSchema());
      pstmt.setString(2, base.getObjectName());

      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String schema = rs.getString(1);
        String name = rs.getString(2);
        String type = rs.getString(3);
        String remarks = rs.getString(4);
        if (type.equals("SEQUENCE"))
        {
          SequenceDefinition seq = new SequenceDefinition(null, schema, name);
          seq.setComment(remarks);
          result.add(seq);
        }
        else
        {
          TableIdentifier tbl = new TableIdentifier(null, schema, name);
          tbl.setComment(remarks);
          tbl.setType(type);
          result.add(tbl);
        }
      }
    }
    catch (Exception ex)
    {
			String s = SqlUtil.replaceParameters(sql, base.getSchema(), base.getObjectName(), base.getObjectType());
      LogMgr.logError("PostgresDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    DbObjectSorter sorter = new DbObjectSorter(true);
    Collections.sort(result, sorter);
    return result;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

}
