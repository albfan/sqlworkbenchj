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
package workbench.db.mssql;


import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.db.CatalogChanger;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;

import workbench.gui.dbobjects.objecttree.DbObjectSorter;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerDependencyReader
  implements DependencyReader
{

  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view", "procedure", "function", "trigger");

  private final String typeDesc =
      "       case ao.type_desc \n" +
      "          when 'USER_TABLE' then 'TABLE'\n" +
      "          when 'SYSTEM_TABLE' then 'SYSTEM TABLE'\n" +
      "          when 'INTERNAL_TABLE' then 'SYSTEM TABLE'\n" +
      "          when 'SQL_STORED_PROCEDURE' then 'PROCEDURE'\n" +
      "          when 'CLR_STORED_PROCEDURE' then 'PROCEDURE'\n" +
      "          when 'EXTENDED_STORED_PROCEDURE' then 'PROCEDURE'\n" +
      "          when 'SQL_SCALAR_FUNCTION' then 'FUNCTION'\n" +
      "          when 'CLR_SCALAR_FUNCTION' then 'FUNCTION'\n" +
      "          when 'SQL_TABLE_VALUED_FUNCTION' then 'FUNCTION'\n" +
      "          when 'SQL_INLINE_TABLE_VALUED_FUNCTION' then 'FUNCTION'\n" +
      "          when 'SQL_TRIGGER' then 'TRIGGER'\n" +
      "          when 'CLR_TRIGGER' then 'TRIGGER'\n" +
      "          else type_desc \n" +
      "        end as type \n";

  private final String searchUsedByInfSchema =
      "SELECT vtu.TABLE_CATALOG, vtu.TABLE_SCHEMA, vtu.TABLE_NAME,\n" + typeDesc +
      "FROM INFORMATION_SCHEMA.VIEW_TABLE_USAGE vtu \n" +
      "  JOIN sys.all_objects ao ON ao.name = vtu.TABLE_NAME and schema_name(ao.schema_id) = vtu.TABLE_SCHEMA\n" +
      "WHERE VIEW_CATALOG = ? \n" +
      "  AND VIEW_SCHEMA = ? \n" +
      "  AND VIEW_NAME = ?";

  private final String searchUsedSqlInfSchema =
      "SELECT vtu.VIEW_CATALOG, vtu.VIEW_SCHEMA, vtu.VIEW_NAME,\n" + typeDesc +
      "FROM INFORMATION_SCHEMA.VIEW_TABLE_USAGE vtu \n" +
      "  JOIN sys.all_objects ao ON ao.name = vtu.VIEW_NAME and schema_name(ao.schema_id) = vtu.VIEW_SCHEMA\n" +
      "WHERE TABLE_CATALOG = ? \n" +
      "  AND TABLE_SCHEMA = ? \n" +
      "  AND TABLE_NAME = ?";

  private final String searchUsedByDMView =
      "SELECT distinct db_name() as catalog_name,  \n" +
      "       coalesce(re.referenced_schema_name, schema_name()) as schema_name,  \n" +
      "       re.referenced_entity_name,  \n" + typeDesc +
      "FROM sys.dm_sql_referenced_entities(?, 'OBJECT') re \n" +
      "  JOIN sys.all_objects ao on ao.object_id = re.referenced_id";

  private final String searchUsedSqlDMView =
      "SELECT db_name() as catalog,  \n" +
      "       coalesce(re.referencing_schema_name,schema_name()) as schema_name,  \n" +
      "       re.referencing_entity_name, \n" + typeDesc +
      "FROM sys.dm_sql_referencing_entities(?, 'OBJECT') re \n" +
      "  JOIN sys.all_objects ao on ao.object_id = re.referencing_id";

  private final CatalogChanger catalogChanger = new CatalogChanger();

  public SqlServerDependencyReader()
  {
    catalogChanger.setFireEvents(false);
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    if (connection.getDbSettings().getBoolProperty("dependency.use.infoschema", false))
    {
      return retrieveObjects(connection, base, searchUsedByInfSchema, false);
    }
    return retrieveObjects(connection, base, searchUsedByDMView, true);
  }

  @Override
  public List<DbObject> getUsedBy(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    if (connection.getDbSettings().getBoolProperty("dependency.use.infoschema", false))
    {
      return retrieveObjects(connection, base, searchUsedSqlInfSchema, false);
    }
    return retrieveObjects(connection, base, searchUsedSqlDMView, true);
  }

  private String changeDatabase(WbConnection conn, String catalog)
  {
    if (catalog == null || conn == null) return catalog;

    String currentCatalog = conn.getCurrentCatalog();
    try
    {
      if (StringUtil.stringsAreNotEqual(catalog, currentCatalog))
      {
        catalogChanger.setCurrentCatalog(conn, catalog);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("SqlServerDependencyReader.changeDatabase()", "Could not change database", ex);
    }
    return currentCatalog;
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql, boolean useFQN)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

    String fqName = buildFQName(connection, base);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s;
      if (useFQN)
      {
        s = SqlUtil.replaceParameters(sql, fqName);
      }
      else
      {
        s = SqlUtil.replaceParameters(sql, base.getCatalog(), base.getSchema(), base.getObjectName(), base.getObjectType());
      }

			LogMgr.logDebug("SqlServerDependencyReader.retrieveObjects()", "Retrieving dependent objects using query:\n" + s);
		}

    String oldCatalog = changeDatabase(connection, base.getCatalog());

    try
    {
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      if (useFQN)
      {
        pstmt.setString(1, fqName);
      }
      else
      {
        pstmt.setString(1, base.getCatalog());
        pstmt.setString(2, base.getSchema());
        pstmt.setString(3, base.getObjectName());
      }

      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String catalog = rs.getString(1);
        String schema = rs.getString(2);
        String name = rs.getString(3);
        String type = rs.getString(4);

        DbObject dbo = null;
        if (type.equals("PROCEDURE"))
        {
          dbo = new ProcedureDefinition(catalog, schema, name);
        }
        else if (type.equals("FUNCTION"))
        {
          dbo = new ProcedureDefinition(catalog, schema, name, DatabaseMetaData.procedureReturnsResult);
        }
        else if (type.equals("TRIGGER"))
        {
          dbo = new TriggerDefinition(catalog, schema, name);
        }
        else
        {
          TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
          tbl.setType(type);
          dbo = tbl;
        }
        result.add(dbo);
      }
    }
    catch (Exception ex)
    {
      String s;
      if (useFQN)
      {
        s = SqlUtil.replaceParameters(sql, fqName);
      }
      else
      {
        s = SqlUtil.replaceParameters(sql, base.getCatalog(), base.getSchema(), base.getObjectName(), base.getObjectType());
      }
      LogMgr.logError("SqlServerDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
      changeDatabase(connection, oldCatalog);
    }

    DbObjectSorter sorter = new DbObjectSorter(true);
    sorter.setIncludeType(true);
    Collections.sort(result, sorter);

    return result;
  }

  private String buildFQName(WbConnection conn, DbObject dbo)
  {
    if (dbo == null) return null;
    String schema = conn.getMetadata().quoteObjectname(dbo.getSchema());
    String name = conn.getMetadata().quoteObjectname(dbo.getObjectName());
    if (StringUtil.isEmptyString(schema))
    {
      schema = conn.getMetadata().quoteObjectname(conn.getCurrentSchema());
    }
    return schema + "." + name;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

}
