/*
 * OracleSynonymReader.java
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
package workbench.db.hana;


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
 *
 * @author  Thomas Kellerer
 */
public class HanaSynonymReader
	implements SynonymReader
{

	@Override
	public List<TableIdentifier> getSynonymList(WbConnection con, String catalog, String owner, String namePattern)
		throws SQLException
	{
		// Nothing to do. The HANA driver already returns the SYNONYMs in the getTables() call
		return Collections.emptyList();
	}

	@Override
	public String getSynonymTypeName()
	{
		return SYN_TYPE_NAME;
	}

	@Override
	public TableIdentifier getSynonymTable(WbConnection con, String catalog, String schema, String synonym)
		throws SQLException
	{
		String sql =
      "-- SQL Workbench \n" +
      "SELECT object_schema, object_name, object_type \n" +
      "FROM sys.synonyms \n" +
      "WHERE schema_name = ? \n" +
      "  AND synonym_name = ? ";

		if (schema == null)
		{
			schema = con.getCurrentSchema();
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("HanaSynonymReader.getSynonymTable()", "Retrieving synonyms using:\n" + SqlUtil.replaceParameters(sql, schema, synonym));
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		TableIdentifier result = null;
		try
		{
			stmt = con.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, synonym);

			rs = stmt.executeQuery();
			if (rs.next())
			{
				String targetSchema = rs.getString(1);
				String targetTable = rs.getString(2);
        String type = rs.getString(3);
				result = new TableIdentifier(null, targetSchema, targetTable);
        result.setNeverAdjustCase(true);
				result.setType(type);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}

	@Override
	public String getSynonymSource(WbConnection con, String catalog, String owner, String synonym)
		throws SQLException
	{
		TableIdentifier targetTable = getSynonymTable(con, catalog, owner, synonym);
		StringBuilder result = new StringBuilder(200);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
    result.append("CREATE SYNONYM ");

    TableIdentifier syn = new TableIdentifier(catalog, owner, synonym);
		result.append(syn.getTableExpression(con));
		result.append(nl + "   FOR ");
		result.append(targetTable.getTableExpression(con));
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
