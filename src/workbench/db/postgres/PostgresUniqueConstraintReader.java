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
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import workbench.db.ConstraintDefinition;

import workbench.db.IndexDefinition;
import workbench.db.JdbcUtils;
import workbench.db.UniqueConstraintReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUniqueConstraintReader
	implements UniqueConstraintReader
{

	@Override
	public void readUniqueConstraints(List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		StringBuilder sql = new StringBuilder(500);
		String baseSql = null;

		if (JdbcUtils.hasMinimumServerVersion(con, "9.0"))
		{
			baseSql =
				"  select ind.relname as index_name, \n" +
				"         indschem.nspname as index_schema, \n" +
				"         cons.conname as constraint_name, \n" +
				"         cons.condeferrable as deferrable, \n" +
				"         cons.condeferred as deferred \n" +
				"  from pg_constraint cons \n" +
				"    join pg_class tbl ON tbl.oid = cons.conrelid \n" +
				"    join pg_namespace ns ON ns.oid = tbl.relnamespace \n" +
				"    join pg_class ind ON ind.oid = cons.conindid \n" +
				"    join pg_namespace indschem ON indschem.oid = ind.relnamespace \n" +
				"  where cons.contype = 'u'";
		}
		else
		{
			// Prior to 9.0 the unique index supporting the constraint could not be named differently
			// than the constraint itself (and pg_constraint.conindid does not exist there)
			baseSql =
				"  select cons.conname as index_name,  \n" +
				"         cns.nspname as index_schema, \n" +
				"         cons.conname as constraint_name, \n" +
				"         false as deferrable, \n" +
				"         false as deferred \n" +
				"  from pg_constraint cons  \n" +
				"    join pg_class tbl ON tbl.oid = cons.conrelid  \n" +
				"    join pg_namespace cns on cns.oid = cons.connamespace \n" +
				"  where cons.contype = 'u'";
		}
		sql.append(
			"select *  \n" +
			"from ( \n");
		sql.append(baseSql);
		sql.append(
			") t \n" +
			"where (index_name, index_schema) in (");

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
				sql.append(", ");
			}
			idxCount ++;
			String schema = con.getMetadata().removeQuotes(idx.getSchema());
			String idxName = con.getMetadata().removeQuotes(idx.getObjectName());
			sql.append(" ('");
			sql.append(idxName);
			sql.append("', '");
			sql.append(schema);
			sql.append("') ");
		}
		sql.append(')');

		if (idxCount == 0)
		{
			return;
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresUniqueConstraintReader.processIndexList()", "Using:\n" + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		try
		{
			sp = con.setSavepoint();
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next())
			{
				String idxName = rs.getString(1);
				String idxSchema = rs.getString(2);
				String consName = rs.getString(3);
				boolean deferrable = rs.getBoolean("deferrable");
				boolean deferred = rs.getBoolean("deferred");
				IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, idxSchema);
				if (def != null)
				{
					ConstraintDefinition cons = ConstraintDefinition.createUniqueConstraint(consName);
					cons.setDeferrable(deferrable);
					cons.setInitiallyDeferred(deferred);
					def.setUniqueConstraint(cons);
				}
			}
			con.releaseSavepoint(sp);
		}
		catch (SQLException se)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresUniqueConstraintReader.processIndexList()", "Could not retrieve definition", se);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

}
