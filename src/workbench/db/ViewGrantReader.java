/*
 * ViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.derby.DerbyViewGrantReader;
import workbench.db.firebird.FirebirdViewGrantReader;
import workbench.db.hsqldb.HsqlViewGrantReader;
import workbench.db.ibm.Db2ViewGrantReader;
import workbench.db.oracle.OracleViewGrantReader;
import workbench.db.postgres.PostgresViewGrantReader;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public abstract class ViewGrantReader
{

	public static ViewGrantReader createViewGrantReader(WbConnection conn)
	{
		DbMetadata meta = conn.getMetadata();
		String dbid = meta.getDbId();

		if (meta.isOracle())
		{
			return new OracleViewGrantReader();
		}
		else if (meta.isPostgres() || "h2".equals(dbid) || meta.isMySql() )
		{
			return new PostgresViewGrantReader();
		}
		else if (meta.isHsql())
		{
			return new HsqlViewGrantReader(conn);
		}
		else if (meta.isSqlServer() && JdbcUtils.hasMinimumServerVersion(conn, "8.0"))
		{
			return new PostgresViewGrantReader();
		}
		else if (meta.isFirebird())
		{
			return new FirebirdViewGrantReader();
		}
		else if (dbid.startsWith("db2"))
		{
			return new Db2ViewGrantReader(dbid);
		}
		else if (meta.isApacheDerby())
		{
			return new DerbyViewGrantReader();
		}
		return null;
	}

	public abstract String getViewGrantSql();

	public int getIndexForSchemaParameter()
	{
		return -1;
	}

	public int getIndexForCatalogParameter()
	{
		return -1;
	}

	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	/**
	 *	Return the GRANTs for the given view

	 *	@return a List with TableGrant objects.
	 */
	public Collection<TableGrant> getViewGrants(WbConnection dbConnection, TableIdentifier viewName)
	{
		Collection<TableGrant> result = new HashSet<>();

		String sql = this.getViewGrantSql();
		if (sql == null) return Collections.emptyList();
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getViewGrants()", "Using SQL: " + sql);
		}

		ResultSet rs = null;
		PreparedStatement stmt = null;
		try
		{
			TableIdentifier view = viewName.createCopy();
			view.adjustCase(dbConnection);

			stmt = dbConnection.getSqlConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			int index = this.getIndexForSchemaParameter();
			if (index > 0) stmt.setString(index, view.getSchema());

			index = this.getIndexForCatalogParameter();
			if (index > 0) stmt.setString(index, view.getCatalog());

			index = this.getIndexForTableNameParameter();
			if (index > 0) stmt.setString(index, view.getTableName());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String to = rs.getString(1);
				String what = rs.getString(2);
				boolean grantable = StringUtil.stringToBool(rs.getString(3));
				TableGrant grant = new TableGrant(to, what, grantable);
				result.add(grant);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("ViewGrantReader", "Error when reading view grants", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;

	}

	/**
	 *	Creates an SQL Statement which can be used to re-create the GRANTs on the
	 *  given table.
	 *
	 *	@return SQL script to GRANT access to the table.
	 */
	public StringBuilder getViewGrantSource(WbConnection dbConnection, TableIdentifier view)
	{
		Collection<TableGrant> grantList = this.getViewGrants(dbConnection, view);
		StringBuilder result = new StringBuilder(200);
		int count = grantList.size();

		// as several grants to several users can be made, we need to collect them
		// first, in order to be able to build the complete statements
		Map<String, List<String>> grants = new HashMap<>(count);

		for (TableGrant grant : grantList)
		{
			String grantee = grant.getGrantee();
			String priv = grant.getPrivilege();
			if (priv == null) continue;
			List<String> privs = grants.get(grantee);
			if (privs == null)
			{
				privs = new LinkedList<>();
				grants.put(grantee, privs);
			}
			privs.add(priv.trim());
		}
		Iterator<Entry<String, List<String>>> itr = grants.entrySet().iterator();

		while (itr.hasNext())
		{
			Entry<String, List<String>> entry = itr.next();
			String grantee = entry.getKey();

			List<String> privs = entry.getValue();
			result.append("GRANT ");
			result.append(StringUtil.listToString(privs, ", ", false));
			result.append(" ON ");
			result.append(view.getTableExpression(dbConnection));
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
		return result;
	}
}
