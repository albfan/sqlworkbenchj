/*
 * OracleUniqueConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.db.IndexDefinition;
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
public class DB2UniqueConstraintReader
	implements UniqueConstraintReader
{

	@Override
	public void processIndexList(List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		StringBuilder sql = new StringBuilder(500);
		if (con.getDbSettings().getDbId().equals("db2"))
		{
			// DB2 LUW
			sql.append(
				"select indname, indschema, constname from ( \n" +
				"select ind.indname, ind.indschema, tc.constname \n" +
				"from syscat.indexes ind \n" +
				"  join syscat.tabconst tc on ind.tabschema = tc.tabschema and ind.tabname = tc.tabname and tc.constname = ind.indname \n" +
				"  where type = 'U' \n" +
			  ") \n " +
				"where (");
		}
		else if (con.getDbSettings().getDbId().equals("db2h"))
		{
			// DB2 host
			sql.append(
				"select indname, indschema, constname from ( \n" +
				"  select ind.name as indname, ind.creator as indschema, tc.constname  \n" +
				"  from sysibm.sysindexes ind \n" +
				"    join sysibm.systabconst tc on ind.tbcreator = tc.tbcreator and ind.tbname = tc.tbname and tc.constname = ind.name \n" +
				"    where type = 'U' \n " +
				") \n " +
				"where (");
		}
		else
		{
			// Not supported for db2 iSeries
			return;
		}

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
			sql.append(" (indname = '");
			sql.append(idxName);
			sql.append("' AND indschema = '");
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
			LogMgr.logDebug("DB2UniqueConstraintReader.processIndexList()", "Using:\n" + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next())
			{
				String idxName = rs.getString(1).trim();
				String idxSchema = rs.getString(2).trim();
				String consName = rs.getString(3).trim();
				IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, idxSchema);
				if (def != null)
				{
					def.setUniqueConstraintName(consName);
				}
			}
		}
		catch (SQLException se)
		{
			LogMgr.logError("DB2UniqueConstraintReader.processIndexList()", "Could not retrieve definition", se);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

}
