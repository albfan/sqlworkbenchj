/*
 * DerbySynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.derby;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Retrieve synonyms and their definition from a Derby database.
 *
 * @author Thomas Kellerer
 */
public class DerbySynonymReader
	implements SynonymReader
{
	public DerbySynonymReader()
	{
	}

	/**
	 * The DB2 JDBC driver returns Alias' automatically, so there
	 * is no need to retrieve them here
	 */
	@Override
	public List<TableIdentifier> getSynonymList(WbConnection con, String owner, String namePattern)
		throws SQLException
	{
		List<TableIdentifier> result = new ArrayList<TableIdentifier>();
		String sql = "SELECT s.schemaname, a.alias \n" +
             " FROM sys.sysaliases a, sys.sysschemas s \n" +
             " WHERE a.schemaid = s.schemaid \n" +
			       "  AND a.aliastype = 'S' \n" +
			       "  AND s.schemaname = ? \n";

		if (StringUtil.isNonBlank(namePattern))
		{
			sql += " AND a.alias LIKE ?";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymList()", "Using SQL: " + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, owner);
			if (StringUtil.isNonBlank(namePattern)) stmt.setString(2, namePattern);

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String schema = rs.getString(1);
				String alias = rs.getString(2);
				if (!rs.wasNull())
				{
					TableIdentifier tbl = new TableIdentifier(schema, alias);
					tbl.setType(SYN_TYPE_NAME);
					tbl.setNeverAdjustCase(true);
					result.add(tbl);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	public TableIdentifier getSynonymTable(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		String sql = "select a.aliasinfo \n" +
             "from sys.sysaliases a, sys.sysschemas s \n" +
             "where a.schemaid = s.schemaid \n" +
             " and a.alias = ?" +
			       " and s.schemaname = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymTable()", "Using SQL: " + sql);
		}

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql);
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner);
		ResultSet rs = stmt.executeQuery();
		String table = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				table = rs.getString(1);
				if (table != null)
				{
					result = new TableIdentifier(table);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}

		if (result != null)
		{
			String type = con.getMetadata().getObjectType(result);
			result.setType(type);
		}

		return result;
	}

	public String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append(nl + "   FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		result.append(nl);

		return result.toString();
	}

}
