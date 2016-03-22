/*
 * OracleUniqueConstraintReader.java
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ConstraintDefinition;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.UniqueConstraintReader;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleUniqueConstraintReader
  implements UniqueConstraintReader
{

  @Override
  public void readUniqueConstraints(TableIdentifier table, List<IndexDefinition> indexList, WbConnection con)
  {
    if (CollectionUtil.isEmpty(indexList))  return;
    if (con == null) return;

    boolean hasMultipleSchemas = hasMultipleOwners(indexList);

    String consView = "all_constraints";
    if (!hasMultipleSchemas)
    {
      if (OracleUtils.optimizeCatalogQueries())
      {
        String schema = indexList.get(0).getSchema();
        if (StringUtil.isEmptyString(schema) || schema.equalsIgnoreCase(con.getCurrentUser()))
        {
          consView = "user_constraints";
        }
      }
    }
    StringBuilder sql = new StringBuilder(500);
    sql.append(
      "-- SQL Workbench \n" +
      "select " + OracleUtils.getCacheHint() + " index_name, constraint_name, deferrable, deferred, status, validated \n" +
      "from " + consView + " \n" +
      "where constraint_type = 'U' \n" +
      "  and ");

    if (hasMultipleOwners(indexList))
    {
      appendMultiOwnerQuery(sql, indexList);
    }
    else
    {
      appendSingleOwnerQuery(sql, indexList);
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("OracleUniqueConstraintReader.processIndexList()", "Retrieving unique constraints using:\n" + sql);
    }

    long start = System.currentTimeMillis();
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        String idxName = rs.getString(1);
        String consName = rs.getString(2);
        String deferrable = rs.getString("deferrable");
        String deferred = rs.getString("deferred");
        String status = rs.getString("status");
        String validated = rs.getString("validated");

        IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, null);
        if (def == null) continue;

        if (def.isPrimaryKeyIndex())
        {
          def.setEnabled(StringUtil.equalStringIgnoreCase(status, "ENABLED"));
          def.setValid(StringUtil.equalStringIgnoreCase(validated, "VALIDATED"));
        }
        else
        {
          ConstraintDefinition cons = ConstraintDefinition.createUniqueConstraint(consName);
          cons.setDeferrable(StringUtil.equalStringIgnoreCase("DEFERRABLE", deferrable));
          cons.setInitiallyDeferred(StringUtil.equalStringIgnoreCase("DEFERRED", deferred));
          cons.setEnabled(StringUtil.equalStringIgnoreCase(status, "ENABLED"));
          cons.setValid(StringUtil.equalStringIgnoreCase(validated, "VALIDATED"));
          def.setUniqueConstraint(cons);
        }
      }
    }
    catch (SQLException se)
    {
      LogMgr.logError("OracleUniqueConstraintReader.processIndexList()", "Could not retrieve definition", se);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("OracleUniqueConstraintReader.processIndexList()", "Retrieving unique constraints took: " + duration + "ms");
  }

  private boolean hasMultipleOwners(List<IndexDefinition> indexList)
  {
    Set<String> owners = CollectionUtil.caseInsensitiveSet();
    for (IndexDefinition idx : indexList)
    {
      owners.add(idx.getSchema());
    }
    return owners.size() > 1;
  }

  private void appendMultiOwnerQuery(StringBuilder sql, List<IndexDefinition> indexList)
  {
    int count = 0;
    sql.append(" (");
    // I have to check the constraints for all indexes regardless if the index is defined
    // as unique or not, because a unique (or primary key) constraint can be enforced by a non-unique index
    // So retrieving this only for unique indexes is not reliable
    for (IndexDefinition idx : indexList)
    {
      if (count > 0)
      {
        sql.append(" OR ");
      }
      String schema = SqlUtil.removeObjectQuotes(idx.getSchema());
      String idxName = SqlUtil.removeObjectQuotes(idx.getObjectName());
      sql.append(" (nvl(index_owner, '");
      sql.append(schema);
      sql.append("'), index_name) = (('");
      sql.append(schema);
      sql.append("', '");
      sql.append(idxName);
      sql.append("')) ");

      count ++;
    }
    sql.append(')');
  }

  private void appendSingleOwnerQuery(StringBuilder sql, List<IndexDefinition> indexList)
  {
    String schema = SqlUtil.removeObjectQuotes(indexList.get(0).getSchema());
    sql.append("nvl(index_owner,'");
    sql.append(schema);
    sql.append("') = '");
    sql.append(schema);
    sql.append("'\n  AND index_name IN (");

    int nr = 0;
    // I have to check the constraints for all indexes regardless if the index is defined
    // as unique or not, because a unique (or primary key) constraint can be enforced by a non-unique index
    // So retrieving this only for unique indexes is not reliable
    for (IndexDefinition idx : indexList)
    {
      if (nr > 0)
      {
        sql.append(',');
      }
      String idxName = SqlUtil.removeObjectQuotes(idx.getObjectName());
      sql.append('\'');
      sql.append(idxName);
      sql.append('\'');
      nr ++;
    }
    sql.append(')');
  }
}
