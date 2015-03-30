/*
 * NuoDbColumnEnhancer.java
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
package workbench.db.nuodb;


import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 * A class to read additional column level information for a table.
 *
 * @author Thomas Kellerer
 */
public class NuoDbColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		readIdentityColumns(table, conn);
	}

	private void readIdentityColumns(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		TableIdentifier tblId = table.getTable();

		String sql =
			"select field \n" +
			"from system.fields \n " +
			"where tablename = ? \n" +
			"and schema = ? \n" +
			"and generator_sequence is not null ";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("NuoDbColumnEnhancer.readIdentityColumns()", "Query to retrieve identity columns:\n" + sql);
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tblId.getRawTableName());
			stmt.setString(2, tblId.getRawSchema());

			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				ColumnIdentifier col = table.findColumn(colname);
				if (col != null)
				{
					col.setIsAutoincrement(true);
					col.setComputedColumnExpression("GENERATED ALWAYS AS IDENTITY");
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("NuoDbColumnEnhancer.readIdentityColumns()", "Error retrieving computed columns", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
}
