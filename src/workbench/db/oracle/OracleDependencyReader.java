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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDependencyReader
  implements DependencyReader
{
  private final String searchRef =
        "select owner, name, type  \n" +
        "from all_dependencies \n" +
        "where referenced_owner = ? \n" +
        "  and referenced_name = ? \n" +
        "  and referenced_type = ? \n" +
        "  and owner not in ('SYS', 'SYSTEM', 'PUBLIC') \n" +
        "order by owner, name";

  private final String searchOwner =
        "select referenced_owner, referenced_name, referenced_type \n" +
        "from all_dependencies \n" +
        "where owner = ? \n" +
        "  and name = ? \n" +
        "  and type = ? \n" +
        "  and referenced_owner not in ('SYS', 'SYSTEM', 'PUBLIC') \n" +
        " order by owner, name";

  private final Set<String> types = CollectionUtil.caseInsensitiveSet("TABLE", "VIEW", "MATERIALIZED VIEW", "PROCEDURE", "TYPE", "FUNCTION", "TRIGGER", "PACKAGE");
  private final Set<String> searchBoth = CollectionUtil.caseInsensitiveSet();

  public OracleDependencyReader()
  {
    List<String> typeList = Settings.getInstance().getListProperty("workbench.db.oracle.dependencies.full", true, "procedure,function,trigger,package,type");
    searchBoth.addAll(typeList);
  }

  @Override
  public List<DbObject> getObjectDependencies(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s = SqlUtil.replaceParameters(searchRef, base.getSchema(), base.getObjectName(), base.getObjectType());
			LogMgr.logDebug("OracleDependencyReader.getObjectDependencies()", "Retrieving object dependency using query:\n " + s);
		}

    List<DbObject> result = retrieveObjects(connection, base, searchRef);

    if (searchBoth.contains(base.getObjectType()))
    {
      if (Settings.getInstance().getDebugMetadataSql())
      {
        String s = SqlUtil.replaceParameters(searchOwner, base.getSchema(), base.getObjectName(), base.getObjectType());
        LogMgr.logDebug("OracleDependencyReader.getObjectDependencies()", "Retrieving object dependency using query:\n " + s);
      }

      List<DbObject> result2 = retrieveObjects(connection, base, searchOwner);
      result.addAll(result2);
    }

    return result;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return types.contains(objectType);
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

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
        if (type.equals("PROCEDURE"))
        {
          result.add(new ProcedureDefinition(null, owner, name));
        }
        else if (type.equals("TRIGGER"))
        {
          result.add(new TriggerDefinition(null, owner, name));
        }
        else if (type.equals("TYPE BODY") || type.equals("TYPE"))
        {
          OracleObjectType obj = new OracleObjectType(owner, name);
          result.add(obj);
        }
        else
        {
          TableIdentifier tbl = new TableIdentifier(null, owner, name);
          tbl.setType(type);
          result.add(tbl);
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
    return result;
  }
}