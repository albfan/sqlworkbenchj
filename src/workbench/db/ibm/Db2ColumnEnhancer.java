/*
 * Db2ColumnEnhancer.java
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
package workbench.db.ibm;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		updateComputedColumns(table, conn);
	}

	private void updateComputedColumns(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String tablename = table.getTable().getTableName();
		String schema = table.getTable().getSchema();

		String columns = "SELECT c.colname, \n" +
								 "       c.generated, \n" +
								 "       c.text, \n" +
								 "       a.start, \n" +
								 "       a.increment, \n" +
								 "       a.minvalue, \n" +
								 "       a.maxvalue, \n" +
								 "       a.cycle, \n" +
								 "       a.cache, \n" +
								 "       a.order \n";

		String from = "FROM syscat.columns c  \n" +
								 "     LEFT JOIN syscat.colidentattributes a ON c.tabname = a.tabname AND c.tabschema = a.tabschema AND c.colname = a.colname \n" +
								 "WHERE c.generated <> ' ' \n" +
								 "AND   c.tabname = ? \n" +
								 "AND   c.tabschema = ? ";

		boolean checkHistory = false;

		if (JdbcUtils.hasMinimumServerVersion(conn, "10.1"))
		{
				columns += ", \n" +
					"       c.rowbegin, \n" +
					"       c.rowend, \n" +
					"       c.transactionstartid \n";
				checkHistory = true;
		}

		String sql = columns + from;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2ColumnEnhancer.updateComputedColumns()", "Query to retrieve column details:\n" + SqlUtil.replaceParameters(sql, schema, tablename));
		}

		Map<String, String> expressions = new HashMap<>();
		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tablename);
			stmt.setString(2, schema);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String gentype = rs.getString(2);
				String computedCol = rs.getString(3);
				BigDecimal start = rs.getBigDecimal(4);
				BigDecimal inc = rs.getBigDecimal(5);
				BigDecimal min = rs.getBigDecimal(6);
				BigDecimal max = rs.getBigDecimal(7);
				boolean cycle = StringUtil.stringToBool(rs.getString(8));
				Integer cache = rs.getInt(9);
				boolean order = StringUtil.stringToBool(rs.getString(10));

				String rowbegin = "N";
				String rowend = "N";
				String transid = "N";
				if (checkHistory)
				{
					rowbegin = rs.getString(11);
					rowend = rs.getString(12);
					transid = rs.getString(13);
				}

				boolean isHistoryTCol= "Y".equals(rowbegin) || "Y".equals(rowend) || "Y".equals(transid);

				String expr = "GENERATED";

				if ("A".equals(gentype))
				{
					expr += " ALWAYS";
				}
				else
				{
					expr += " BY DEFAULT";
				}

				if (computedCol == null && !isHistoryTCol)
				{
					// IDENTITY column
					expr += " AS IDENTITY (" + Db2SequenceReader.buildSequenceDetails(false, start, min, max, inc, cycle, order, cache) + ")";
				}
				else if (isHistoryTCol)
				{
					if ("Y".equals(rowbegin))
					{
						expr += " AS ROW BEGIN";
					}
					else if ("Y".equals(rowend))
					{
						expr += " AS ROW END";
					}
					else if ("Y".equals(transid))
					{
						expr += " AS TRANSACTION START ID";
					}
				}
				else
				{
					expr += " " + computedCol;
				}
				expressions.put(colname, expr);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2ColumnEnhancer.updateComputedColumns()", "Error retrieving generated column info", e);
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
				if (expr.contains("AS ROW") || expr.contains("AS TRANSACTION"))
				{
					col.setGeneratorExpression(expr);
				}
				else
				{
					col.setComputedColumnExpression(expr);
				}

				if (expr.indexOf("IDENTITY") > -1)
				{
					col.setIsAutoincrement(true);
				}
			}
		}
	}

}
