/*
 * PostgresUniqueConstraintReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
public class PostgresUniqueConstraintReader
	implements UniqueConstraintReader
{

	@Override
	public void processIndexList(List<IndexDefinition> indexList, WbConnection con)
	{
		if (CollectionUtil.isEmpty(indexList))  return;
		if (con == null) return;

		StringBuilder sql = new StringBuilder(500);
		sql.append("select *  \n" +
             "from ( \n" +
             "  select ind.relname as index_name, indschem.nspname as index_schema, cons.conname as constraint_name \n" +
             "  from pg_constraint cons \n" +
             "    join pg_class tbl ON tbl.oid = cons.conrelid \n" +
             "    join pg_namespace ns ON ns.oid = tbl.relnamespace \n" +
             "    join pg_class ind ON ind.oid = cons.conindid \n" +
             "    join pg_namespace indschem ON indschem.oid = ind.relnamespace \n" +
             "  where cons.contype = 'u' \n" +
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
				IndexDefinition def = IndexDefinition.findIndex(indexList, idxName, idxSchema);
				if (def != null)
				{
					def.setUniqueConstraintName(consName);
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
