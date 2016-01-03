/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.SequenceAdjuster;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 * A class to sync the sequences related to the columns of a table with the current values.
 *
 * This is intended to be used after doing bulk inserts into the database.
 *
 * @author Thomas Kellerer
 */
public class HsqlSequenceAdjuster
	implements SequenceAdjuster
{
	public HsqlSequenceAdjuster()
	{
	}

	@Override
	public int adjustTableSequences(WbConnection connection, TableIdentifier table, boolean includeCommit)
		throws SQLException
	{
		List<String> columns = getIdentityColumns(connection, table);

		for (String column : columns)
		{
			syncSingleSequence(connection, table, column);
		}

		if (includeCommit && !connection.getAutoCommit())
		{
			connection.commit();
		}
		return columns.size();
	}

	private void syncSingleSequence(WbConnection dbConnection, TableIdentifier table, String column)
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
				String ddl = "alter table " + table.getTableExpression(dbConnection) + " alter column " + column + " restart with " + Long.toString(maxValue);
				LogMgr.logDebug("HsqlSequenceAdjuster.syncSingleSequence()", "Syncing sequence using: " + ddl);
				stmt.execute(ddl);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("HsqlSequenceAdjuster.getColumnSequences()", "Could not read sequences", ex);
			throw ex;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	private List<String> getIdentityColumns(WbConnection dbConnection, TableIdentifier table)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select column_name " +
			"from information_schema.columns \n" +
			"where table_name = ? \n" +
			" and table_schema = ? \n" +
			" and (is_generated = 'ALWAYS' OR (is_identity = 'YES' AND identity_generation IS NOT NULL))";

		List<String> result = new ArrayList<String>(1);
		try
		{
			pstmt = dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, table.getRawTableName());
			pstmt.setString(2, table.getRawSchema());

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String column = rs.getString(1);
				result.add(column);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("HsqlSequenceAdjuster.getIdentityColumns()", "Could not read sequence columns", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}

}
