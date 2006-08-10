/*
 * Db2SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;

/**
 * @author support@sql-workbench.net
 */
public class Db2SynonymReader
	implements SynonymReader
{
	
	public Db2SynonymReader()
	{
	}

	/**
	 * The DB2 JDBC driver returns Alias' automatically, so there 
	 * is no need to retrieve them here
	 */
	public List getSynonymList(String owner) 
		throws SQLException
	{
		return Collections.EMPTY_LIST;
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(200);

		sql.append("SELECT base_tabschema, base_tabname FROM syscat.tables ");
		sql.append(" WHERE TYPE = 'A' and tabname = ? and tabschema = ?");

		PreparedStatement stmt = con.prepareStatement(sql.toString());
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
				result = new TableIdentifier(null, owner, table);
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
		result.append("CREATE ALIAS ");
		result.append(aSynonym);
		result.append("\n       FOR ");
		result.append(id.getTableExpression());
		result.append(";\n");
		return result.toString();
	}

}
