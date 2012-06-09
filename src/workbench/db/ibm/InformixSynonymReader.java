/*
 * Db2SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to retrieve synonym definitions from an Informix database.
 *
 * @author Thomas Kellerer
 */
public class InformixSynonymReader
	implements SynonymReader
{

	/**
	 * Returns an empty list, as the standard JDBC driver
	 * alread returns synonyms in the getObjects() method.
	 *
	 * @return an empty list
	 */
	@Override
	public List<TableIdentifier> getSynonymList(WbConnection con, String owner, String namePattern)
		throws SQLException
	{
		return Collections.emptyList();
	}

	@Override
	public TableIdentifier getSynonymTable(WbConnection con, String schema, String synonymName)
		throws SQLException
	{
		String sql =
			"select bt.owner as table_schema, \n" +
			"       bt.tabname as table_name \n" +
			"from systables syn \n" +
			"  join syssyntable lnk on lnk.tabid = syn.tabid \n" +
			"  join systables bt on bt.tabid = lnk.btabid \n" +
			"and syn.tabname = ? \n" +
			"and syn.owner = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("InformixSynonymReader.getSynonymTable()", "Using query=\n" + sql);
		}

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql.toString());
		stmt.setString(1, synonymName);
		stmt.setString(2, schema);

		ResultSet rs = stmt.executeQuery();
		String table = null;
		String owner = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				owner = rs.getString(1);
				table = rs.getString(2);
				if (table != null)
				{
					result = new TableIdentifier(null, owner, table);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	@Override
	public String getSynonymSource(WbConnection con, String synonymSchema, String synonymName)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, synonymSchema, synonymName);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		result.append(SqlUtil.buildExpression(con, null, synonymSchema, synonymName));
		result.append(nl).append("   FOR ");
		result.append(id.getTableExpression(con));
		result.append(';');
		result.append(nl);
		return result.toString();
	}

}
