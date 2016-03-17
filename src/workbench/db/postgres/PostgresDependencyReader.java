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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
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
  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view", "sequence");

  private final String typeCase =
      "       CASE cl.relkind \n" +
      "          WHEN 'r' THEN 'TABLE'\n" +
      "          WHEN 'i' THEN 'INDEX'\n" +
      "          WHEN 'S' THEN 'SEQUENCE'\n" +
      "          WHEN 'v' THEN 'VIEW'\n" +
      "          WHEN 'm' THEN 'MATERIALIZED VIEW'\n" +
      "          WHEN 'c' THEN 'TYPE'\n" +
      "          WHEN 't' THEN 'TOAST'\n" +
      "          WHEN 'f' THEN 'FOREIGN TABLE'\n" +
      "       END AS object_type, \n";

  private final String searchUsed =
      "select vtu.table_schema, \n" +
      "       vtu.table_name, \n" + typeCase +
      "       obj_description(cl.oid) as remarks\n" +
      "from information_schema.view_table_usage vtu \n" +
      "  join pg_class cl on cl.oid = (quote_ident(vtu.table_schema)||'.'||quote_ident(vtu.table_name))::regclass \n" +
      "where (view_schema, view_name) = (?, ?) \n" +
      "order by view_schema, view_name";

  private final String searchUsedBy =
        "select vtu.view_schema, \n" +
        "       vtu.view_name, \n" + typeCase +
        "       obj_description(cl.oid) as remarks\n" +
        "from information_schema.view_table_usage vtu \n" +
        "  join pg_class cl on cl.oid = (quote_ident(vtu.view_schema)||'.'||quote_ident(vtu.view_name))::regclass \n" +
        "where (table_schema, table_name) = (?, ?) \n" +
        "order by view_schema, view_name";

  private final String tableSequenceSql =
    "select n.nspname as sequence_schema, s.relname as sequence_name, 'SEQUENCE', obj_description(s.oid) as remarks\n" +
    "from pg_class s\n" +
    "  join pg_depend d on d.objid=s.oid and d.classid='pg_class'::regclass and d.refclassid='pg_class'::regclass\n" +
    "  join pg_class t on t.oid=d.refobjid\n" +
    "  join pg_namespace n on n.oid=t.relnamespace\n" +
    "  join pg_attribute a on a.attrelid=t.oid and a.attnum=d.refobjsubid\n" +
    "where s.relkind='S' \n" +
    "  and d.deptype='a' \n " +
    "  and n.nspname = ? \n" +
    "  and t.relname = ?";

  private final String sequenceUsageSql =
    "select n.nspname as table_schema, cl.relname as table_name, " + typeCase + " obj_description(cl.oid) as remarks\n" +
    "from pg_class s\n" +
    "  join pg_depend d on d.objid=s.oid and d.classid='pg_class'::regclass and d.refclassid='pg_class'::regclass\n" +
    "  join pg_class cl on cl.oid = d.refobjid \n" +
    "  join pg_namespace n on n.oid = cl.relnamespace\n" +
    "  join pg_attribute a on a.attrelid = cl.oid and a.attnum=d.refobjsubid\n" +
    "where s.relkind='S' \n" +
    "  and d.deptype='a' \n " +
    "  and n.nspname = ? \n" +
    "  and s.relname = ?";

  public PostgresDependencyReader()
  {
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    List<DbObject> objects = retrieveObjects(connection, base, searchUsed);

    List<DbObject> sequences = retrieveObjects(connection, base, tableSequenceSql);
    objects.addAll(sequences);

    PostgresInheritanceReader reader = new PostgresInheritanceReader();
    if (base instanceof TableIdentifier && base.getObjectType().equalsIgnoreCase("table"))
    {
      List<TableIdentifier> parents = reader.getParents(connection, (TableIdentifier)base);
      for (TableIdentifier tbl : parents)
      {
        objects.add(tbl);
      }
    }

    DbObjectSorter.sort(objects, true);

    return objects;
  }

  @Override
  public List<DbObject> getUsedBy(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();
    List<DbObject> objects = retrieveObjects(connection, base, searchUsedBy);

    List<DbObject> tables = retrieveObjects(connection, base, sequenceUsageSql);
    objects.addAll(tables);

    PostgresInheritanceReader reader = new PostgresInheritanceReader();
    if (base instanceof TableIdentifier && base.getObjectType().equalsIgnoreCase("table"))
    {
      List<InheritanceEntry> children = reader.getChildren(connection, (TableIdentifier)base);
      for (InheritanceEntry entry : children)
      {
        objects.add(entry.getTable());
      }
    }

    DbObjectSorter.sort(objects, true);

    return objects;
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

    Savepoint sp = null;
    try
    {
      sp = connection.setSavepoint();

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
          tbl.setNeverAdjustCase(true);
          tbl.setComment(remarks);
          tbl.setType(type);
          result.add(tbl);
        }
      }
      connection.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      connection.rollback(sp);
      String s = SqlUtil.replaceParameters(sql, base.getSchema(), base.getObjectName(), base.getObjectType());
      LogMgr.logError("PostgresDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    return result;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

}
