/*
 * Db2SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to retrieve synonym definitions from a DB2 database.
 * @author support@sql-workbench.net
 */
public class Db2SynonymReader
	implements SynonymReader
{
	
	public Db2SynonymReader()
	{
	}

	/**
	 * Returns an empty list, as the standard JDBC driver 
	 * alread returns synonyms in the getTables() method.
	 * 
	 * @return an empty list
	 */
	public List<String> getSynonymList(Connection con, String owner) 
		throws SQLException
	{
		return Collections.emptyList();
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);

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
		result.append("CREATE ALIAS ");
		result.append(aSynonym);
		result.append(nl + "       FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		return result.toString();
	}

}
