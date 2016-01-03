/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.db.SequenceAdjuster;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 * A class to sync the sequences related to the columns of a table with the current values of those columns.
 *
 * This is intended to be used after doing bulk inserts into the database.
 *
 * @author Thomas Kellerer
 */
public class H2SequenceAdjuster
	implements SequenceAdjuster
{
	public H2SequenceAdjuster()
	{
	}

	@Override
	public int adjustTableSequences(WbConnection connection, TableIdentifier table, boolean includeCommit)
		throws SQLException
	{
		Map<String, String> columns = getColumnSequences(connection, table);

		for (Map.Entry<String, String> entry : columns.entrySet())
		{
			syncSingleSequence(connection, table, entry.getKey(), entry.getValue());
		}

		if (includeCommit && !connection.getAutoCommit())
		{
			connection.commit();
		}
		return columns.size();
	}

	private void syncSingleSequence(WbConnection dbConnection, TableIdentifier table, String column, String sequence)
		throws SQLException
	{
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			stmt = dbConnection.createStatement();

			long maxValue = -1;
			rs = stmt.executeQuery("select max(" + column + ") from " + table.getTableExpression(dbConnection));

			if (rs.next())
			{
				maxValue = rs.getLong(1) + 1;
				SqlUtil.closeResult(rs);
			}

			if (maxValue > 0)
			{
				String ddl = "alter sequence " + sequence + " restart with " + Long.toString(maxValue);
				LogMgr.logDebug("H2SequenceAdjuster.syncSingleSequence()", "Syncing sequence using: " + ddl);
				stmt.execute(ddl);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("H2SequenceAdjuster.syncSingleSequence()", "Could not read sequences", ex);
			throw ex;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private Map<String, String> getColumnSequences(WbConnection dbConnection, TableIdentifier table)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select column_name,  \n" +
			"       column_default \n" +
			"from information_schema.columns \n" +
			"where table_name = ? \n" +
			" and table_schema = ? \n" +
			" and column_default like '(NEXT VALUE FOR%'";

		Map<String, String> result = new HashMap<>();
		try
		{
			pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, table.getRawTableName());
			pstmt.setString(2, table.getRawSchema());

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String column = rs.getString(1);
				String defValue = rs.getString(2);
				defValue = defValue.replace("NEXT VALUE FOR", "");
				if (defValue.startsWith("(") && defValue.endsWith(")"))
				{
					defValue = defValue.substring(1, defValue.length() -1 );
				}
				result.put(column, defValue);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("H2SequenceAdjuster.getColumnSequences()", "Could not read sequences", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

}
