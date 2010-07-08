/*
 * Db2SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * A class to retrieve synonym definitions from a DB2 database.
 * 
 * @author Thomas Kellerer
 */
public class Db2SynonymReader
	implements SynonymReader
{
	
	/**
	 * Returns an empty list, as the standard JDBC driver 
	 * alread returns synonyms in the getObjects() method.
	 * 
	 * @return an empty list
	 */
	public List<TableIdentifier> getSynonymList(WbConnection con, String owner, String namePattern)
		throws SQLException
	{
		return Collections.emptyList();
	}

	public TableIdentifier getSynonymTable(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);

		boolean isHostDB2 = con.getMetadata().getDbId().equals("db2h");
		boolean isAS400 = con.getMetadata().getDbId().equals("db2i");
		if (isAS400)
		{
			sql.append("SELECT base_table_schema, base_table_name FROM qsys2.systables");
			sql.append(" WHERE table_type = 'A' AND table_name = ? AND table_owner = ?");
		}
		else if (isHostDB2)
		{
			sql.append("SELECT tbcreator, tbname FROM sysibm.syssynonyms ");
			sql.append(" WHERE name = ? and creator = ?");

		}
		else
		{
			sql.append("SELECT base_tabschema, base_tabname FROM syscat.tables ");
			sql.append(" WHERE type = 'A' and tabname = ? and tabschema = ?");
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2SynonymReader.getSynonymTable()", "Using query=\n" + sql);
		}

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql.toString());
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner);

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

	public String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE ALIAS ");
		result.append(aSynonym);
		result.append(nl + "   FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		result.append(nl);
		
		return result.toString();
	}

}
