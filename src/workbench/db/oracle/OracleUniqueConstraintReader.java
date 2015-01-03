/*
 * OracleUniqueConstraintReader.java
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ConstraintDefinition;
import workbench.db.IndexDefinition;
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
	public void processIndexList(List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		StringBuilder sql = new StringBuilder(500);
		sql.append(
			"select /* SQL Workbench */ index_name, constraint_name, deferrable, deferred, status, validated \n" +
			"from all_constraints \n" +
			"where constraint_type = 'U' \n" +
			" AND (");

		boolean first = true;

		for (IndexDefinition idx : indexList)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sql.append(" OR ");
			}
			String schema = con.getMetadata().removeQuotes(idx.getSchema());
			String idxName = con.getMetadata().removeQuotes(idx.getObjectName());
			sql.append(" (nvl(index_owner, '");
			sql.append(schema);
			sql.append("') = '");
			sql.append(schema);
			sql.append("' AND index_name = '");
			sql.append(idxName);
			sql.append("') ");
		}
		sql.append(')');

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleUniqueConstraintReader.processIndexList()", "Using:\n" + sql);
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
	}
}
