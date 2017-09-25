/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
import workbench.db.ProcedureDefinition;
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
  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view", "sequence", "trigger", "function", "type");

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

  private final String proArgs =
    "       coalesce(array_to_string(p.proallargtypes, ';'), array_to_string(p.proargtypes, ';')) as arg_types, \n" +
    "       array_to_string(p.proargnames, ';') as arg_names, \n" +
    "       array_to_string(p.proargmodes, ';') as arg_modes, \n" +
    "       p.oid::text as proc_id \n";

  private final String tablesUsedByView =
      "select vtu.table_schema, \n" +
      "       vtu.table_name, \n" + typeCase +
      "       obj_description(cl.oid) as remarks\n" +
      "from information_schema.view_table_usage vtu \n" +
      "  join pg_class cl on cl.oid = (quote_ident(vtu.table_schema)||'.'||quote_ident(vtu.table_name))::regclass \n" +
      "where (view_schema, view_name) = (?, ?) \n" +
      "order by view_schema, view_name";

  private final String viewsUsingTable =
        "select vtu.view_schema, \n" +
        "       vtu.view_name, \n" + typeCase +
        "       obj_description(cl.oid) as remarks\n" +
        "from information_schema.view_table_usage vtu \n" +
        "  join pg_class cl on cl.oid = (quote_ident(vtu.view_schema)||'.'||quote_ident(vtu.view_name))::regclass \n" +
        "where (table_schema, table_name) = (?, ?) \n" +
        "order by view_schema, view_name";

  private String typesUsedByFunction =
    "select distinct ts.nspname as type_schema, typ.typname as type_name, 'TYPE', obj_description(typ.oid) as remarks \n" +
    "from pg_proc c \n" +
    "  join pg_namespace n on n.oid = c.pronamespace \n" +
    "  join pg_depend d on d.objid = c.oid and d.refclassid = 'pg_type'::regclass \n" +
    "  join pg_type typ on typ.oid = d.refobjid \n" +
    "  join pg_namespace ts on ts.oid = typ.typnamespace \n" +
    "where n.nspname = ? \n" +
    "  and c.proname = ?";

  private final String functionsUsingType =
    "select distinct n.nspname as function_schema, p.proname as function_name, 'FUNCTION', obj_description(p.oid) as remarks, \n" + proArgs +
    "from pg_proc p \n" +
    "  join pg_namespace n on n.oid = p.pronamespace \n" +
    "  join pg_depend d on d.objid = p.oid and d.classid = 'pg_proc'::regclass \n" +
    "  join pg_type typ on typ.oid = d.refobjid \n" +
    "  join pg_namespace ts on ts.oid = typ.typnamespace \n" +
    "where ts.nspname = ? \n" +
    "  and typ.typname = ? \n";

  private final String tablesUsingType =
    "select distinct n.nspname as table_schema, \n " +
    "       cl.relname as table_name, \n" +
    "       " + typeCase +
    "       obj_description(cl.oid) as remarks  \n" +
    "from pg_class cl  \n" +
    "  join pg_namespace n on n.oid = cl.relnamespace  \n" +
    "  join pg_depend d on d.objid = cl.oid and d.classid = 'pg_class'::regclass  \n" +
    "  join pg_type t on t.oid = d.refobjid  \n" +
    "  join pg_namespace tn on tn.oid = t.typnamespace \n" +
    "where d.deptype in ('a', 'n')" +
    "  and cl.relkind in ('r', 'v', 'f') \n" +
    "  and tn.nspname = ? \n" +
    "  and t.typname = ? \n";

  private final String typesUsedByTable =
    "select distinct tn.nspname as type_schema, t.typname as type_name, 'TYPE' as object_type, obj_description(t.oid) as remarks \n" +
    "from pg_class c \n" +
    "  join pg_namespace n on n.oid = c.relnamespace \n" +
    "  join pg_depend d on d.objid = c.oid and d.classid = 'pg_class'::regclass \n" +
    "  join pg_type t on t.oid = d.refobjid \n" +
    "  join pg_namespace tn on tn.oid = t.typnamespace\n" +
    "where d.deptype in ('a', 'n') \n" +
    "  and n.nspname = ? \n"+
    "  and c.relname = ? ";

  private static final String sequencesUsedByTable =
    "select distinct sn.nspname as sequence_schema, s.relname as sequence_name, 'SEQUENCE', obj_description(s.oid) as remarks\n" +
    "from pg_class s\n" +
    "  join pg_namespace sn on sn.oid = s.relnamespace \n" +
    "  join pg_depend d on d.refobjid = s.oid and d.refclassid='pg_class'::regclass \n" +
    "  join pg_attrdef ad on ad.oid = d.objid and d.classid = 'pg_attrdef'::regclass\n" +
    "  join pg_attribute col on col.attrelid = ad.adrelid and col.attnum = ad.adnum\n" +
    "  join pg_class tbl on tbl.oid = ad.adrelid \n" +
    "  join pg_namespace ts on ts.oid = tbl.relnamespace \n" +
    "where s.relkind = 'S' \n" +
    "  and d.deptype in ('a', 'n') \n " +
    "  and ts.nspname = ? \n" +
    "  and tbl.relname = ?";

  private final String tablesUsingSequence =
    "select distinct n.nspname as table_schema, \n" +
    "       cl.relname as table_name, \n" +
    "       " + typeCase +
    "       obj_description(cl.oid) as remarks\n" +
    "from pg_class s\n" +
    "  join pg_depend d on d.refobjid = s.oid and d.refclassid = 'pg_class'::regclass\n" +
    "  join pg_attrdef ad on ad.oid = d.objid and d.classid = 'pg_attrdef'::regclass\n" +
    "  join pg_attribute col on col.attrelid = ad.adrelid and col.attnum = ad.adnum\n" +
    "  join pg_class cl on cl.oid = ad.adrelid \n" +
    "  join pg_namespace n on n.oid = cl.relnamespace\n " +
    "where s.relkind = 'S' \n" +
    "  and d.deptype in ('a', 'n') \n " +
    "  and n.nspname = ? \n" +
    "  and s.relname = ?";

  private final String triggerImplementationFunction =
    "SELECT trgsch.nspname as function_schema, proc.proname as function_name, 'FUNCTION', obj_description(proc.oid) as remarks, " + proArgs +
    "FROM pg_trigger trg  \n" +
    "  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
    "  JOIN pg_proc p ON p.oid = trg.tgfoid \n" +
    "  JOIN pg_namespace trgsch ON trgsch.oid = p.pronamespace \n" +
    "  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n" +
    "WHERE tblsch.nspname =  ? \n" +
    "  AND trg.tgname = ? ";

  private static final String triggerTable =
    "SELECT tblsch.nspname as table_schema, tbl.relname as table_name, 'TABLE', obj_description(tbl.oid) as remarks \n" +
    "FROM pg_trigger trg  \n" +
    "  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
    "  JOIN pg_proc proc ON proc.oid = trg.tgfoid \n" +
    "  JOIN pg_namespace trgsch ON trgsch.oid = proc.pronamespace \n" +
    "  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n" +
    "WHERE tblsch.nspname =  ? \n" +
    "  AND trg.tgname = ? ";

  private static final String triggersUsingFunction =
    "SELECT trgsch.nspname as trigger_schema, trg.tgname as trigger_name, 'TRIGGER', obj_description(trg.oid) as remarks \n" +
    "FROM pg_trigger trg  \n" +
    "  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
    "  JOIN pg_proc proc ON proc.oid = trg.tgfoid \n" +
    "  JOIN pg_namespace trgsch ON trgsch.oid = proc.pronamespace \n" +
    "  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n" +
    "WHERE tblsch.nspname = ? \n" +
    "  and proc.proname = ? ";

  private final PostgresProcedureReader procReader;

  public PostgresDependencyReader(WbConnection conn)
  {
    procReader = new PostgresProcedureReader(conn);
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    String objectType = base.getObjectType().toLowerCase();

    if (objectType.equals("function"))
    {
      return retrieveObjects(connection, base, typesUsedByFunction);
    }

    if (objectType.equals("trigger"))
    {
      return getTriggerFunction(connection, base);
    }

    List<DbObject> objects = retrieveObjects(connection, base, tablesUsedByView);

    List<DbObject> sequences = retrieveObjects(connection, base, sequencesUsedByTable);
    objects.addAll(sequences);

    if (objectType.equals("table") ||objectType.equals("view") || objectType.equals("materialized view"))
    {
      List<DbObject> types = retrieveObjects(connection, base, typesUsedByTable);
      objects.addAll(types);
    }

    PostgresInheritanceReader reader = new PostgresInheritanceReader();
    if (base instanceof TableIdentifier && objectType.equals("table"))
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

    String objectType = base.getObjectType().toLowerCase();

    if (objectType.equals("trigger"))
    {
      return retrieveObjects(connection, base, triggerTable);
    }

    if (objectType.equals("function"))
    {
      return retrieveObjects(connection, base, triggersUsingFunction);
    }

    List<DbObject> objects = retrieveObjects(connection, base, viewsUsingTable);

    List<DbObject> tables = retrieveObjects(connection, base, tablesUsingSequence);
    objects.addAll(tables);

    if (objectType.equals("type"))
    {
      tables = retrieveObjects(connection, base, tablesUsingType);
      objects.addAll(tables);
      List<DbObject> types = retrieveObjects(connection, base, functionsUsingType);
      objects.addAll(types);
    }

    PostgresInheritanceReader reader = new PostgresInheritanceReader();
    if (base instanceof TableIdentifier && objectType.equals("table"))
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

  private List<DbObject> getTriggerFunction(WbConnection connection, DbObject base)
  {
    return retrieveObjects(connection, base, triggerImplementationFunction);
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
        else if (type.equals("FUNCTION"))
        {
          String types = rs.getString("arg_types");
          String args = rs.getString("arg_names");
          String modes = rs.getString("arg_modes");
          String procId = rs.getString("proc_id");
          ProcedureDefinition proc = procReader.createDefinition(schema, name, args, types, modes, procId);
          proc.setComment(remarks);
          result.add(proc);
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
  public boolean supportsUsedByDependency(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

  @Override
  public boolean supportsIsUsingDependency(String objectType)
  {
    if ("sequence".equalsIgnoreCase(objectType)) return false;
    return supportedTypes.contains(objectType);
  }
}
