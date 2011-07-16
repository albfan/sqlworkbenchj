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
package workbench.db.oracle;

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
public class OracleUniqueConstraintReader
	implements UniqueConstraintReader
{

	@Override
	public void processIndexList(List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		StringBuilder sql = new StringBuilder(200);
		sql.append(
			"select index_name, constraint_name \n" +
			"from all_constraints \n" +
			"where constraint_type = 'U' \n" +
			" AND (");

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
			sql.append(" (nvl(index_owner, '");
			sql.append(schema);
			sql.append("') = '");
			sql.append(schema);
			sql.append("' AND index_name = '");
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
				IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, null);
				if (def != null)
				{
					def.setUniqueConstraintName(consName);
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
