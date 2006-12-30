/*
 * OracleSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import workbench.db.SynonymReader;

import workbench.db.TableIdentifier;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class OracleSynonymReader
	implements SynonymReader
{
	public OracleSynonymReader()
	{
	}

	/**
	 * The Oracle driver already returns the SYNONYMS in the getTables() call
	 */
	public List getSynonymList(Connection con, String owner) 
		throws SQLException
	{
		return Collections.EMPTY_LIST;
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_name, table_owner, table_name, db_link FROM all_synonyms ");
		sql.append(" WHERE synonym_name = ? AND owner = ? ");

		PreparedStatement stmt = con.prepareStatement(sql.toString());
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner == null ? "PUBLIC" : anOwner);

		ResultSet rs = stmt.executeQuery();
		String table = null;
		String owner = null;
		String dblink = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				owner = rs.getString(2);
				table = rs.getString(3);
				dblink = rs.getString(4);
				if (dblink != null) table = table + "@" + dblink;
				result = new TableIdentifier(null, owner, table);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	public String getSynonymSource(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append(nl + "       FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		return result.toString();
	}

}
