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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresInheritanceReader
{
	public List<InheritanceEntry> getChildren(WbConnection dbConnection, TableIdentifier table)
	{
		if (table == null) return null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		final String sql83 =
			"select bt.relname as table_name, bns.nspname as table_schema, 0 as level \n" +
			"from pg_class ct \n" +
			"    join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
			"    join pg_inherits i on i.inhparent = ct.oid and ct.relname = ? \n" +
			"    join pg_class bt on i.inhrelid = bt.oid \n" +
			"    join pg_namespace bns on bt.relnamespace = bns.oid";

		// Recursive version for 8.4+ based Craig Ringer's statement from here: http://stackoverflow.com/a/12139506/330315
		final String sql84 =
			"with recursive inh as ( \n" +
			"\n" +
			"  select i.inhrelid, 1 as level, array[inhrelid] as path \n" +
			"  from pg_catalog.pg_inherits i  \n" +
			"    join pg_catalog.pg_class cl on i.inhparent = cl.oid \n" +
			"    join pg_catalog.pg_namespace nsp on cl.relnamespace = nsp.oid \n" +
			"  where nsp.nspname = ? \n" +
			"    and cl.relname = ? \n" +
			"" +
			"  union all \n" +
			"\n" +
			"  select i.inhrelid, inh.level + 1, inh.path||i.inhrelid \n" +
			"  from inh \n" +
			"    join pg_catalog.pg_inherits i on (inh.inhrelid = i.inhparent) \n" +
			") \n" +
			"select pg_class.relname as table_name, pg_namespace.nspname as table_schema, inh.level \n" +
			"from inh \n" +
			"  join pg_catalog.pg_class on (inh.inhrelid = pg_class.oid) \n" +
			"	 join pg_catalog.pg_namespace on (pg_class.relnamespace = pg_namespace.oid) \n" +
			"order by path";

		final boolean is84 = JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4");

    // wenn putting the "?" expression directly into the prepareStatement() call, this generates an error with Java 8
    final String sqlToUse = is84 ? sql84 : sql83;

    List<InheritanceEntry> result = new ArrayList<>();

		Savepoint sp = null;
		try
		{
			// Retrieve direct child table(s) for this table
			// this does not handle multiple inheritance
			sp = dbConnection.setSavepoint();
			pstmt = dbConnection.getSqlConnection().prepareStatement(sqlToUse);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresInheritanceReader.getChildTables()", "Retrieving child tables using:\n" + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String tableName = rs.getString(1);
				String schemaName = rs.getString(2);
        int level = rs.getInt(3);
        TableIdentifier tbl = new TableIdentifier(schemaName, tableName);
        result.add(new InheritanceEntry(tbl, level));
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresInheritanceReader.getChildTables()", "Error retrieving table options using: " + pstmt.toString(), e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}


	public List<TableIdentifier> getParents(WbConnection dbConnection, TableIdentifier table)
	{
		if (table == null) return null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String sql =
			"select bt.relname as table_name, bns.nspname as table_schema \n" +
			"from pg_class ct \n" +
			"  join pg_namespace cns on ct.relnamespace = cns.oid and cns.nspname = ? \n" +
			"  join pg_inherits i on i.inhrelid = ct.oid and ct.relname = ? \n" +
			"  join pg_class bt on i.inhparent = bt.oid \n" +
			"  join pg_namespace bns on bt.relnamespace = bns.oid";

		Savepoint sp = null;
    List<TableIdentifier> result = new ArrayList<>();
		try
		{
			// Retrieve parent table(s) for this table
			sp = dbConnection.setSavepoint();
			pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, table.getSchema());
			pstmt.setString(2, table.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresInheritanceReader.getParents()", "Reading parent tables using:\n" + pstmt);
			}
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String tableName = rs.getString(1);
        String schema = rs.getString(2);
        result.add(new TableIdentifier(schema, tableName));
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresInheritanceReader.getParents()", "Error retrieving table inheritance using:\n" + pstmt, e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

}
