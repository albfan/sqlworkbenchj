/*
 * MySQLColumnCollationReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLColumnCollationReader
{

	public void readCollations(TableDefinition table, WbConnection conn)
	{
		String defaultCharacterSet = null;
		String defaultCollation = null;
		Statement info = null;
		ResultSet rs = null;
		try
		{
			String variables = "show variables where variable_name in ('collation_database', 'character_set_database')";
			info = conn.createStatement();
			rs = info.executeQuery(variables);
			while (rs.next())
			{
				String name = rs.getString(1);
				String value = rs.getString(2);
				if ("character_set_database".equals(name))
				{
					defaultCharacterSet = value;
				}
				if ("collation_database".equals(name))
				{
					defaultCollation = value;
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("MySQLColumnEnhancer.readCollations()", "Could not read default collation", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, info);
		}

		PreparedStatement stmt = null;

		HashMap<String, String> collations = new HashMap<>(table.getColumnCount());
		HashMap<String, String> expressions = new HashMap<>(table.getColumnCount());
		String sql =
			"SELECT column_name, \n" +
			"       character_set_name, \n" +
			"       collation_name \n" +
			"FROM information_schema.columns \n" +
			"WHERE table_name = ? \n" +
			"AND   table_schema = ? ";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("MySQLColumnEnhancer.readCollations()", "Using SQL=\n" + sql);
		}


		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTable().getTableName());
			stmt.setString(2, table.getTable().getCatalog());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String charset = rs.getString(2);
				String collation = rs.getString(3);
				String expression = null;
				if (isNonDefault(collation, defaultCollation) && isNonDefault(charset, defaultCharacterSet))
				{
					expression = "CHARSET " + charset + " COLLATE " + collation;
				}
				else if (isNonDefault(collation, defaultCollation))
				{
					expression = "COLLATE " + collation;
				}
				else if (isNonDefault(charset, defaultCharacterSet))
				{
					expression = "CHARSET " + charset;
				}

				if (expression != null)
				{
					expressions.put(colname, expression);
				}
				if (collation != null)
				{
					collations.put(colname, collation);
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("MySQLColumnEnhancer.readCollations()", "Could not read column collations", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		for (ColumnIdentifier col : table.getColumns())
		{
			String expression = expressions.get(col.getColumnName());
			if (expression != null)
			{
				String dataType = col.getDbmsType() + " " + expression;
				col.setDbmsType(dataType);
			}
			String collation = collations.get(col.getColumnName());
			if (collation != null)
			{
				col.setCollation(collation);
			}
		}
	}

	private boolean isNonDefault(String value, String defaultValue)
	{
		if (StringUtil.isEmptyString(value)) return false;
		return !value.equals(defaultValue);
	}

}
