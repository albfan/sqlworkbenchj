/*
 * HsqlColumnEnhancer.java
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
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import workbench.log.LogMgr;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.SequenceDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve information about computed/generated columns in HSQLDB 2.0
 *
 * @author Thomas Kellerer
 */
public class HsqlColumnEnhancer
	implements ColumnDefinitionEnhancer
{
	private boolean supportColumnSequence = true;

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		updateComputedColumns(table, conn);
	}

	private void updateComputedColumns(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = table.getTable().getRawTableName();
		String schema = table.getTable().getRawSchema();

		String sql = "select column_name, " +
								"        generation_expression, " +
								"        is_generated, " +
								"        is_identity, " +
								"        identity_generation, " +
								"        identity_start, " +
								"        identity_increment " +
								"from information_schema.columns \n" +
								"where table_name = ? \n" +
								"and table_schema = ? \n" +
								"and (is_generated = 'ALWAYS' OR (is_identity = 'YES' AND identity_generation IS NOT NULL))  \n";

		Map<String, String> expressions = new HashMap<>();
		Map<String, SequenceDefinition> sequences = getColumnSequences(conn, table.getTable());

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tablename);
			stmt.setString(2, schema);
			rs = stmt.executeQuery();

			while (rs.next())
			{
				String colname = rs.getString(1);
				String computedExpr = rs.getString(2);
				String isComputed = rs.getString(3);
				String isIdentity = rs.getString(4);
				String identityExpr = rs.getString(5);

				String columnExpression = null;
				if (sequences.containsKey(colname))
				{
					SequenceDefinition def = sequences.get(colname);
					columnExpression = "GENERATED " + identityExpr + " AS SEQUENCE " + def.getSequenceName();
				}
				else if (StringUtil.equalString(isComputed, "ALWAYS"))
				{
					// HSQL apparently stores a fully qualified column name
					// Using that when re-creating the source looks a bit weird, so
					// we'll simply strip off the schema and table name
					int pos = computedExpr.lastIndexOf('.');
					if (pos > 1)
					{
						computedExpr = computedExpr.substring(pos + 1);
					}
					columnExpression = "GENERATED ALWAYS AS (" + computedExpr + ")";
				}
				else if (StringUtil.equalString(isIdentity, "YES"))
				{
					String start = rs.getString(6);
					String inc = rs.getString(7);
					columnExpression = "GENERATED " + identityExpr + " AS IDENTITY";
					if (StringUtil.stringsAreNotEqual("0", start) || StringUtil.stringsAreNotEqual("1", inc))
					{
						columnExpression += " (START WITH " + start + " INCREMENT BY " + inc + ")";
					}
				}
				if (StringUtil.isBlank(columnExpression)) continue;
				expressions.put(colname, columnExpression);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("HsqlColumnEnhancer.updateComputedColumns()", "Error retrieving remarks", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		for (ColumnIdentifier col : table.getColumns())
		{
			String expr = expressions.get(col.getColumnName());
			if (StringUtil.isNonBlank(expr))
			{
				col.setDefaultValue(null);
				col.setComputedColumnExpression(expr);
			}
		}
	}

	/**
	 * Returns information about sequences mapped to table columns.
	 * @param conn the connection to use
	 * @param tbl the table to check
	 */
	private Map<String, SequenceDefinition> getColumnSequences(WbConnection conn, TableIdentifier tbl)
	{
		if (!supportColumnSequence || !JdbcUtils.hasMinimumServerVersion(conn, "2.2"))
		{
			supportColumnSequence = false;
			return Collections.emptyMap();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Map<String, SequenceDefinition> result = new HashMap<>();

		try
		{
			String sql =
				"SELECT column_name, sequence_catalog, sequence_schema, sequence_name \n" +
				"FROM information_schema.system_column_sequence_usage \n" +
				"WHERE table_catalog = ? \n" +
				"  AND table_schema = ? \n " +
				"  AND table_name = ? ";

			pstmt = conn.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getCatalog());
			pstmt.setString(2, tbl.getSchema());
			pstmt.setString(3, tbl.getTableName());

			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String col = rs.getString(1);
				String cat = rs.getString(2);
				String schema = rs.getString(3);
				String name = rs.getString(4);
				SequenceDefinition def = new SequenceDefinition(cat, schema, name);
				result.put(col, def);
			}
		}
		catch (SQLException sql)
		{
			supportColumnSequence = false;
			LogMgr.logError("HsqlColumnEnhancer.getColumnSequence()", "Error retrieving sequence for column", sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return result;
	}
}
