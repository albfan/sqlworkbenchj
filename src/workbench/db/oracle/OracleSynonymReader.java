/*
 * OracleSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
	/**
	 * The Oracle driver already returns the SYNONYMS in the getTables() call
	 */
	public List<String> getSynonymList(Connection con, String owner)
		throws SQLException
	{
		return Collections.emptyList();
	}

	public TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(400);
		sql.append("SELECT s.synonym_name, s.table_owner, s.table_name, s.db_link, o.object_type, s.owner ");
		sql.append("FROM all_synonyms s, all_objects o  ");
		sql.append("where s.table_name = o.object_name ");
		sql.append("and s.table_owner = o.owner ");
		sql.append("and ((s.synonym_name = ? AND s.owner = ?) ");
		sql.append(" or (s.synonym_name = ? AND s.owner = 'PUBLIC')) ");
		sql.append("ORDER BY decode(s.owner, 'PUBLIC',9,1)");

		PreparedStatement stmt = null;

		ResultSet rs = null;

		TableIdentifier result = null;
		try
		{
			stmt = con.prepareStatement(sql.toString());
			stmt.setString(1, aSynonym);
			stmt.setString(2, anOwner == null ? con.getMetaData().getUserName() : anOwner);
			stmt.setString(3, aSynonym);

			rs = stmt.executeQuery();
			if (rs.next())
			{
				String owner = rs.getString(2);
				String table = rs.getString(3);
				String dblink = rs.getString(4);
				String type = rs.getString(5);
				if (dblink != null) table = table + "@" + dblink;
				result = new TableIdentifier(null, owner, table);
				result.setType(type);
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
		result.append(nl);
		return result.toString();
	}

}
