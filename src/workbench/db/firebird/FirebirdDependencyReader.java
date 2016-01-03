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
package workbench.db.firebird;

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
public class FirebirdDependencyReader
  implements DependencyReader
{
  private final Set<String> supportedTypes = CollectionUtil.caseInsensitiveSet("table", "view");

  private static final String typeCase =
    "when 0 then 'TABLE' when 1 then 'VIEW' end as type_name";

  private final String searchUsed =
    "select distinct rdb$depended_on_name, case rdb$depended_on_type " + typeCase + " \n" +
    "from rdb$dependencies \n" +
    "where rdb$dependent_name = ?";

  private final String searchUsedBy =
    "select distinct rdb$dependent_name, case rdb$dependent_type " + typeCase + " \n" +
    "from rdb$dependencies\n" +
    "where rdb$depended_on_name = ?";

  public FirebirdDependencyReader()
  {
  }

  @Override
  public List<DbObject> getUsedObjects(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();

    List<DbObject> objects = retrieveObjects(connection, base, searchUsed);

    return objects;
  }

  @Override
  public List<DbObject> getUsedBy(WbConnection connection, DbObject base)
  {
    if (base == null || connection == null) return Collections.emptyList();
    List<DbObject> objects = retrieveObjects(connection, base, searchUsedBy);

    return objects;
  }

  private List<DbObject> retrieveObjects(WbConnection connection, DbObject base, String sql)
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    List<DbObject> result = new ArrayList<>();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			String s = SqlUtil.replaceParameters(sql, base.getObjectName());
			LogMgr.logDebug("FirebirdDependencyReader.retrieveObjects()", "Retrieving dependent objects using query:\n" + s);
		}

    Savepoint sp = null;
    try
    {
      connection.setSavepoint();
      pstmt = connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, base.getObjectName());

      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String name = rs.getString(1);
        String type = rs.getString(2);
        TableIdentifier tbl = new TableIdentifier(name);
        tbl.setType(type);
        result.add(tbl);
      }
    }
    catch (Exception ex)
    {
			String s = SqlUtil.replaceParameters(sql, base.getObjectName());
      LogMgr.logError("FirebirdDependencyReader.retrieveObjects()", "Could not read object dependency using:\n" + s, ex);
    }
    finally
    {
      connection.rollback(sp);
      SqlUtil.closeAll(rs, pstmt);
    }

    DbObjectSorter.sort(result, true, true);
    return result;
  }

  @Override
  public boolean supportsDependencies(String objectType)
  {
    return supportedTypes.contains(objectType);
  }

}
