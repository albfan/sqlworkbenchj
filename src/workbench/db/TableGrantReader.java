/*
 * TableGrantReader.java
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
package workbench.db;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.oracle.OracleTableGrantReader;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class TableGrantReader
{

	public static TableGrantReader createReader(WbConnection conn)
	{
		DbMetadata meta = conn.getMetadata();

		if (meta.isOracle())
		{
			return new OracleTableGrantReader();
		}
    return new TableGrantReader();
  }

	/**
	 *	Return the GRANTs for the given table
	 *
	 *	Some JDBC drivers return all GRANT privileges separately even if the original
	 *  GRANT was a GRANT ALL ON object TO user.
	 *
	 *	@return a List with TableGrant objects.
	 */
	public Collection<TableGrant> getTableGrants(WbConnection dbConnection, TableIdentifier table)
	{
		Collection<TableGrant> result = new HashSet<>();
		ResultSet rs = null;
		Set<String> ignoreGrantors = dbConnection.getDbSettings().getGrantorsToIgnore();
		Set<String> ignoreGrantees = dbConnection.getDbSettings().getGranteesToIgnore();

		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(dbConnection);
			rs = dbConnection.getSqlConnection().getMetaData().getTablePrivileges(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
			boolean useColumnNames = dbConnection.getDbSettings().useColumnNameForMetadata();
			while (rs.next())
			{
				String from = useColumnNames ? rs.getString("GRANTOR") : rs.getString(4);
				if (ignoreGrantors.contains(from)) continue;

				String to = useColumnNames ? rs.getString("GRANTEE") : rs.getString(5);
				if (ignoreGrantees.contains(to)) continue;

				String what = useColumnNames ? rs.getString("PRIVILEGE") : rs.getString(6);
				boolean grantable = StringUtil.stringToBool(useColumnNames ? rs.getString("IS_GRANTABLE") : rs.getString(7));
				TableGrant grant = new TableGrant(to, what, grantable);
				result.add(grant);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableGrants()", "Error when retrieving table privileges",e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
		return result;
	}

	/**
	 *	Creates an SQL Statement which can be used to re-create the GRANTs on the
	 *  given table.
	 *
	 *	@return SQL script to GRANT access to the table.
	 */
	public StringBuilder getTableGrantSource(WbConnection dbConnection, TableIdentifier table)
	{
		Collection<TableGrant> grantList = this.getTableGrants(dbConnection, table);
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

		String user = dbConnection.getCurrentUser();
		while (itr.hasNext())
		{
			Entry<String, List<String>> entry = itr.next();
			String grantee = entry.getKey();
			// Ignore grants to ourself
			if (user != null && user.equalsIgnoreCase(grantee)) continue;

			List<String> privs = entry.getValue();
			result.append("GRANT ");
			result.append(StringUtil.listToString(privs, ", ", false));
			result.append(" ON ");
			result.append(table.getTableExpression(dbConnection));
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
		return result;
	}
}
