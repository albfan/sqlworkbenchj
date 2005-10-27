/*
 * OracleSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import workbench.db.SynonymReader;

import workbench.db.TableIdentifier;

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

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(200);
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
				//result.setExternalTable(dblink != null);
			}
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
			try { stmt.close(); } catch (Exception e) {}
		}

		return result;
	}

	public String getSynonymSource(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuffer result = new StringBuffer(200);
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append("\n       FOR ");
		result.append(id.getTableExpression());
		result.append(";\n");
		return result.toString();
	}

}
