/*
 * DerbySynonymReader.java
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
package workbench.db.derby;

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
import workbench.util.StringUtil;

/**
 * Retrieve synonyms and their definition from a Derby database.
 *
 * @author Thomas Kellerer
 */
public class DerbySynonymReader
	implements SynonymReader
{
	public DerbySynonymReader()
	{
	}

	@Override
	public String getSynonymTypeName()
	{
		return SYN_TYPE_NAME;
	}

	@Override
	public List<TableIdentifier> getSynonymList(WbConnection con, String catalog, String owner, String namePattern)
		throws SQLException
	{
		List<TableIdentifier> result = new ArrayList<>();
		String sql = "SELECT s.schemaname, a.alias \n" +
             " FROM sys.sysaliases a, sys.sysschemas s \n" +
             " WHERE a.schemaid = s.schemaid \n" +
			       "  AND a.aliastype = 'S' \n" +
			       "  AND s.schemaname = ? \n";

		if (StringUtil.isNonBlank(namePattern))
		{
			sql += " AND a.alias LIKE ?";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymList()", "Using SQL: " + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, owner);
			if (StringUtil.isNonBlank(namePattern)) stmt.setString(2, namePattern);

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String schema = rs.getString(1);
				String alias = rs.getString(2);
				if (!rs.wasNull())
				{
					TableIdentifier tbl = new TableIdentifier(schema, alias);
					tbl.setType(SYN_TYPE_NAME);
					tbl.setNeverAdjustCase(true);
					result.add(tbl);
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
	public TableIdentifier getSynonymTable(WbConnection con, String catalog, String owner, String synonym)
		throws SQLException
	{
		String sql = "select a.aliasinfo \n" +
             "from sys.sysaliases a, sys.sysschemas s \n" +
             "where a.schemaid = s.schemaid \n" +
             " and a.alias = ?" +
			       " and s.schemaname = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo(getClass().getName() + ".getSynonymTable()", "Using SQL: " + SqlUtil.replaceParameters(sql, synonym, owner));
		}

		PreparedStatement stmt = con.getSqlConnection().prepareStatement(sql);
		stmt.setString(1, synonym);
		stmt.setString(2, owner);
		ResultSet rs = stmt.executeQuery();
		String table = null;
		TableIdentifier result = null;
		try
		{
			if (rs.next())
			{
				table = rs.getString(1);
				if (table != null)
				{
					result = new TableIdentifier(table);
				}
			}
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}

		if (result != null)
		{
			String type = con.getMetadata().getObjectType(result);
			result.setType(type);
		}

		return result;
	}

	@Override
	public String getSynonymSource(WbConnection con, String catalog, String schema, String synonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, catalog, schema, synonym);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
    TableIdentifier syn = new TableIdentifier(catalog, schema, synonym);
		result.append(syn.getTableExpression(con));
		result.append(nl);
		result.append("   FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		result.append(nl);

		return result.toString();
	}

  @Override
  public boolean supportsReplace(WbConnection con)
  {
    return false;
  }

}
