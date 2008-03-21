/*
 * TableGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class TableGrantReader
{
	public TableGrantReader()
	{
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
		Collection<TableGrant> result = new HashSet<TableGrant>();
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(dbConnection);
			rs = dbConnection.getSqlConnection().getMetaData().getTablePrivileges(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
			while (rs.next())
			{
				String to = rs.getString(5);
				String what = rs.getString(6);
				boolean grantable = StringUtil.stringToBool(rs.getString(7));
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
		Map<String, List<String>> grants = new HashMap<String, List<String>>(count);

		for (TableGrant grant : grantList)
		{
			String grantee = grant.getGrantee();
			String priv = grant.getPrivilege();
			if (priv == null) continue;
			List<String> privs = grants.get(grantee);
			if (privs == null)
			{
				privs = new LinkedList<String>();
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
			if (user.equalsIgnoreCase(grantee)) continue;
			
			List<String> privs = entry.getValue();
			result.append("GRANT ");
			result.append(StringUtil.listToString(privs, ','));
			result.append(" ON ");
			result.append(table.getTableExpression(dbConnection));
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
		return result;
	}	
}
