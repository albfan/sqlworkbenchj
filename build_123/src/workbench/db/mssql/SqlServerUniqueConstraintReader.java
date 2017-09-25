/*
 * SqlServerUniqueConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ConstraintDefinition;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.UniqueConstraintReader;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerUniqueConstraintReader
  implements UniqueConstraintReader
{

  @Override
  public void readUniqueConstraints(TableIdentifier table, List<IndexDefinition> indexList, WbConnection con)
  {
    if (!SqlServerUtil.isSqlServer2005(con)) return;

    if (CollectionUtil.isEmpty(indexList))  return;
    if (con == null) return;

    StringBuilder sql = new StringBuilder(500);
    sql.append(
      "select ind.name as indname, sch.name as indschema, cons.name as consname \n" +
      "from sys.indexes ind with (nolock) \n" +
      "  join sys.objects obj with (nolock) on ind.object_id = obj.object_id \n" +
      "  join sys.schemas sch with (nolock) on sch.schema_id = obj.schema_id \n" +
      "  join sys.key_constraints cons with (nolock) on ind.object_id = cons.parent_object_id and ind.index_id = cons.unique_index_id \n" +
      "where is_unique = 1  \n" +
      "and is_unique_constraint = 1 \n" +
      "and (");

    boolean first = true;
    int idxCount = 0;

    for (IndexDefinition idx : indexList)
    {
      if (!idx.isUnique() || idx.isPrimaryKeyIndex())
      {
        continue;
      }
      if (first)
      {
        first = false;
      }
      else
      {
        sql.append(" OR ");
      }
      idxCount ++;
      String schema = con.getMetadata().removeQuotes(idx.getSchema());
      String idxName = con.getMetadata().removeQuotes(idx.getObjectName());
      sql.append(" (sch.name = '");
      sql.append(schema);
      sql.append("' AND ind.name = '");
      sql.append(idxName);
      sql.append("') ");
    }
    sql.append(')');
    if (idxCount == 0)
    {
      return;
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("SqlServerUniqueConstraintReader.processIndexList()", "Using:\n" + sql);
    }

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        String idxName = rs.getString(1);
        String idxSchema = rs.getString(2);
        String consName = rs.getString(3);
        IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, idxSchema);
        if (def != null)
        {
          ConstraintDefinition cons = ConstraintDefinition.createUniqueConstraint(consName);
          def.setUniqueConstraint(cons);
        }
      }
    }
    catch (SQLException se)
    {
      LogMgr.logError("SqlServerUniqueConstraintReader.processIndexList()", "Could not retrieve definition", se);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
