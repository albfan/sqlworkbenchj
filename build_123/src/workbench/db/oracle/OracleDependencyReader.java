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
package workbench.db.oracle;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbObjectComparator;
import workbench.db.PackageDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;
import workbench.gui.dbobjects.objecttree.DbObjectSorter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDependencyReader
  implements DependencyReader
{
  private final String searchUsedBySql =
        "select owner, name, type  \n" +
        "from all_dependencies \n" +
        "where referenced_owner = ? \n" +
        "  and referenced_name = ? \n" +
        "  and referenced_type = ? \n" +
        "  and owner not in ('SYS', 'SYSTEM', 'PUBLIC')";

  private final String searchUsedSql =
        "select referenced_owner, referenced_name, referenced_type \n" +
        "from all_dependencies \n" +
        "where owner = ? \n" +
        "  and name = ? \n" +
        "  and type = ? \n" +
        "  and referenced_owner not in ('SYS', 'SYSTEM', 'PUBLIC')";

  private final Set<String> types = CollectionUtil.caseInsensitiveSet("TABLE", "VIEW", "MATERIALIZED VIEW", "PROCEDURE", "TYPE", "FUNCTION", "TRIGGER", "PACKAGE");

  public OracleDependencyReader()
  {
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();
    List<DbObject> objects = retrieveObjects(connection, base, searchUsedSql);
    DbObjectSorter.sort(objects, true, true);

    // remove the entry with the the table that is created for the MVIEW
    if (base.getObjectType().equals(DbMetadata.MVIEW_NAME))
    {
      objects.removeIf(dbo -> DbObjectComparator.namesAreEqual(base, dbo, false));
    }
    return objects;
  }

  @Override
  public List<DbObject> getUsedBy(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    List<DbObject> objects = retrieveObjects(connection, base, searchUsedBySql);
    DbObjectSorter.sort(objects, true, true);
    return objects;
  }

  /**
   * all_dependencies will return the "bodies" for types and packages.
   *
   * This method returns those objects elements with "XXX BODY"
   * where both elements are present (TYPE and TYPE BODY or PACKAGE and PACKAGE BODY)
   */
  private List<DbObject> removeBodies(List<DbObject> objects)
  {
    List<DbObject> result = new ArrayList<>(objects.size());
    for (DbObject dbo : objects)
    {
      if (dbo.getObjectType().endsWith("BODY"))
      {
        // don't add the package/type body if we already have the declaration
        if (containsDeclaration(objects, dbo)) continue;
      }
      result.add(dbo);
    }
    return result;
  }

  private boolean containsDeclaration(List<DbObject> objects, DbObject body)
  {
    String base = body.getObjectType().replace("BODY", "").trim();
    for (DbObject dbo : objects)
    {
      if (dbo.getObjectType().equals(base) &&
          dbo.getObjectName().equals(body.getObjectName()) &&
          dbo.getSchema().equals(body.getSchema())) return true;
    }
    return false;
  }

  @Override
  public boolean supportsUsedByDependency(String objectType)
  {
    return types.contains(objectType);
  }

  @Override
  public boolean supportsIsUsingDependency(String objectType)
  {
    if ("table".equalsIgnoreCase(objectType)) return false;
    return types.contains(objectType);
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

    if (Settings.getInstance().getDebugMetadataSql())
    {
      String s = SqlUtil.replaceParameters(sql, base.getSchema(), base.getObjectName(), base.getObjectType());
      LogMgr.logDebug("OracleDependencyReader.retrieveObjects()", "Retrieving object dependency using query:\n" + s);
    }

    try
    {
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, base.getSchema());
      pstmt.setString(2, base.getObjectName());
      pstmt.setString(3, base.getObjectType());
      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String owner = rs.getString(1);
        String name = rs.getString(2);
        String type = rs.getString(3);
        DbObject dbo = null;
        if (type.equals("PROCEDURE"))
        {
          dbo = new ProcedureDefinition(null, owner, name);
        }
        else if (type.equals("FUNCTION"))
        {
          dbo = new ProcedureDefinition(null, owner, name, DatabaseMetaData.procedureReturnsResult);
        }
        else if (type.equals("TRIGGER"))
        {
          dbo = new TriggerDefinition(null, owner, name);
        }
        else if (type.equals("TYPE BODY") || type.equals("TYPE"))
        {
          dbo = new OracleObjectType(owner, name);
        }
        else if (type.equals("PACKAGE BODY") || type.equals("PACKAGE"))
        {
          dbo = new PackageDefinition(owner, name);
        }
        else
        {
          TableIdentifier tbl = new TableIdentifier(null, owner, name);
          tbl.setType(type);
          tbl.setNeverAdjustCase(true);
          dbo = tbl;
        }
        if (!DbObjectComparator.namesAreEqual(base, dbo))
        {
          result.add(dbo);
        }
      }
    }
    catch (Exception ex)
    {
      String s = SqlUtil.replaceParameters(sql, base.getSchema(), base.getObjectName(), base.getObjectType());
      LogMgr.logError("OracleDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    return removeBodies(result);
  }
}
