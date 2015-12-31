/*
 * InformixSynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SynonymReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 * A class to retrieve synonym definitions from an Informix database.
 *
 * @author Thomas Kellerer
 */
public class InformixSynonymReader
	implements SynonymReader
{

	/**
	 * Returns an empty list, as the standard JDBC driver
	 * alread returns synonyms in the getObjects() method.
	 *
	 * @return an empty list
	 */
	@Override
	public List<TableIdentifier> getSynonymList(WbConnection con, String catalog, String owner, String namePattern)
		throws SQLException
	{
		return Collections.emptyList();
	}

	@Override
	public String getSynonymTypeName()
	{
		return SYN_TYPE_NAME;
	}

	@Override
	public TableIdentifier getSynonymTable(WbConnection con, String catalog, String schema, String synonymName)
		throws SQLException
	{
		String systemSchema = con.getDbSettings().getProperty("systemschema", "informix");
		TableIdentifier sysTabs = new TableIdentifier(catalog, systemSchema, "systables");
		TableIdentifier synTabs = new TableIdentifier(catalog, systemSchema, "syssyntable");

		String systables = sysTabs.getFullyQualifiedName(con);
		String syntable = synTabs.getFullyQualifiedName(con);

		String sql =
			"select bt.owner as table_schema, \n" +
			"       bt.tabname as table_name \n" +
			"from " + systables + " syn \n" +
			"  join " + syntable + " lnk on lnk.tabid = syn.tabid \n" +
			"  join " + systables + " bt on bt.tabid = lnk.btabid \n" +
			"and syn.tabname = ? \n" +
			"and syn.owner = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("InformixSynonymReader.getSynonymTable()", "Query to retrieve synonym:\n" + SqlUtil.replaceParameters(sql, synonymName, schema));
		}

		TableIdentifier result = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, synonymName);
			stmt.setString(2, schema);

			rs = stmt.executeQuery();
			String table = null;
			String owner = null;
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
		catch (SQLException ex)
		{
			LogMgr.logError("InformixSynonymReader.getSynonymTable()", "Could not retrieve synonym table using:\n + sql", ex);
			throw ex;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	@Override
	public String getSynonymSource(WbConnection con, String catalog, String synonymSchema, String synonymName)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, catalog, synonymSchema, synonymName);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append("CREATE SYNONYM ");
		TableIdentifier syn = new TableIdentifier(catalog, synonymSchema, synonymName);
		result.append(syn.getTableExpression(con));
		result.append(nl).append("   FOR ");
		result.append(id.getTableExpression(con));
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
