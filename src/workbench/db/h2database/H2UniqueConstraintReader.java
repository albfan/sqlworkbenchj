/*
 * PostgresUniqueConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.h2database;



import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class H2UniqueConstraintReader
	implements UniqueConstraintReader
{

	@Override
	public void readUniqueConstraints(TableIdentifier table, List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		String baseSql =
				"select unique_index_name, constraint_name\n" +
        "from information_schema.constraints \n" +
        "where (table_catalog, table_schema, table_name) = (?,?,?)";

		if (Settings.getInstance().getDebugMetadataSql())
		{
      LogMgr.logDebug("H2UniqueConstraintReader.readUniqueConstraints()",
        "Using:\n" + SqlUtil.replaceParameters(baseSql, table.getCatalog(), table.getSchema(), table.getTableName()));
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
      pstmt = con.getSqlConnection().prepareStatement(baseSql);
      pstmt.setString(1, table.getCatalog());
      pstmt.setString(2, table.getSchema());
      pstmt.setString(3, table.getTableName());

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String idxName = rs.getString(1);
				String consName = rs.getString(2);

				IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, null);
				if (def != null)
				{
					ConstraintDefinition cons = ConstraintDefinition.createUniqueConstraint(consName);
					def.setUniqueConstraint(cons);
				}
			}
		}
		catch (SQLException se)
		{
			LogMgr.logError("H2UniqueConstraintReader.readUniqueConstraints()",
        "Could not retrieve unique constraint definition using:\n" + SqlUtil.replaceParameters(baseSql, table.getCatalog(), table.getSchema(), table.getTableName()), se);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

}
