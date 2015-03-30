/*
 * IngresSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

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
	public List<TableIdentifier> getSynonymList(WbConnection conn, String catalog, String owner, String namePattern)
	{
		ResultSet rs = null;
		PreparedStatement stmt = null;
		List<TableIdentifier> result = new ArrayList<>();

		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT synonym_owner, synonym_name FROM iisynonyms ");
		if (owner != null)
		{
			sql.append(" WHERE synonym_owner = ?");
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("IngresSynonymReader.getSynonymList()", "Query to retrieve synonyms:\n" + sql);
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
			LogMgr.logError("OracleMetaData.getSynonymList()", "Error when retrieving synonyms using SQL:\n" + sql,e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	@Override
	public TableIdentifier getSynonymTable(WbConnection con, String catalog, String anOwner, String aSynonym)
		throws SQLException
	{
		String sql =
		  "SELECT synonym_name, table_owner, table_name \n" +
			"FROM iisynonyms \n" +
			"WHERE synonym_name = ? \n " +
			"  AND synonym_owner = ? ";

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql);
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("IngresSynonymReader.getSynonymTable()", "Query to retrieve synonym table:\n" + sql);
		}

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

}

