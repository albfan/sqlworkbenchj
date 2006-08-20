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
package workbench.db.derby;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
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
	public List getSynonymList(Connection con, String owner) 
		throws SQLException
	{
		LinkedList result = new LinkedList();
		String sql = "select a.alias " + 
             "from sys.sysaliases a, sys.sysschemas s \n" + 
             "where a.schemaid = s.schemaid \n" + 
			       " and a.aliastype = 'S' " +
			       " and s.schemaname = ?";		

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.prepareStatement(sql);
			stmt.setString(1, owner);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String alias = rs.getString(1);
				if (!rs.wasNull())
				{
					result.add(alias);
				}
			}
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
			try { stmt.close(); } catch (Exception e) {}
		}

		return result;
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		String sql = "select a.aliasinfo \n" + 
             "from sys.sysaliases a, sys.sysschemas s \n" + 
             "where a.schemaid = s.schemaid \n" + 
             " and a.alias = ?" +
			       " and s.schemaname = ?";		

		PreparedStatement stmt = con.prepareStatement(sql);
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
				table = rs.getString(1);
				result = new TableIdentifier(table);
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
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append(nl + "       FOR ");
		result.append(id.getTableExpression());
		result.append(";");
		return result.toString();
	}

}
