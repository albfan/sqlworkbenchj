/*
 * IngresMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ingres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A SynonymReader for Ingres
 *
 * @author  Thomas Kellerer
 */
public class IngresSynonymReader
	implements SynonymReader
{
	public IngresSynonymReader()
	{
	}

	/**
	 * 	Get a list of synonyms for the given owner
	 */
	@Override
	public List<TableIdentifier> getSynonymList(WbConnection conn, String owner, String namePattern)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		List<TableIdentifier> result = new ArrayList<TableIdentifier>();

		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_owner, synonym_name FROM iisynonyms ");
		if (owner != null)
		{
			sql.append(" WHERE synonym_owner = ?");
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql.toString());
			if (owner != null) stmt.setString(1, owner);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String schema = rs.getString(1);
				String name = rs.getString(2);
				if (name == null) continue;

				TableIdentifier tbl = new TableIdentifier(schema.trim(), name.trim());
				tbl.setNeverAdjustCase(true);
				tbl.setType(SYN_TYPE_NAME);
				result.add(tbl);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSynonymList()", "Error when retrieving synonyms",e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public TableIdentifier getSynonymTable(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_name, table_owner, table_name FROM iisynonyms ");
		sql.append(" WHERE synonym_name = ? AND synonym_owner = ?");

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
				owner = rs.getString(2);
				table = rs.getString(3);
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
	public String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuilder result = new StringBuilder(200);
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append("\n   FOR ");
		result.append(id.getTableExpression());
		result.append(";\n");
		return result.toString();
	}



}

