/*
 * FirebirdColumnEnhancer
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
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

		String sql = "SELECT c.colname, \n" +
								 "       c.generated, \n" +
								 "       c.text, \n" +
								 "       a.start, \n" +
								 "       a.increment, \n" +
								 "       a.minvalue, \n" +
								 "       a.maxvalue, \n" +
								 "       a.cycle, \n" +
								 "       a.cache, \n" +
								 "       a.order  \n" +
								 "FROM syscat.columns c  \n" +
								 "     LEFT JOIN syscat.colidentattributes a ON c.tabname = a.tabname AND c.tabschema = a.tabschema AND c.colname = a.colname \n" +
								 "WHERE c.generated <> ' ' \n" +
								 "AND   c.tabname = ? \n" +
								 "AND   c.tabschema = ? ";
		Map<String, String> expressions = new HashMap<String, String>();
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
				String cycle = rs.getString(8);
				Integer cache = rs.getInt(9);
				String order = rs.getString(10);

				String expr = "GENERATED";

				if ("A".equals(gentype))
				{
					expr += " ALWAYS";
				}
				else
				{
					expr += " BY DEFAULT";
				}

				if (computedCol == null)
				{
					// IDENTITY column
					expr += " AS IDENTITY (" + Db2SequenceReader.buildSequenceDetails(false, start, min, max, inc, cycle, order, cache) + ")";
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
				col.setComputedColumnExpression(expr);
				if (expr.indexOf("IDENTITY") > -1)
				{
					col.setIsAutoincrement(true);
				}
			}
		}
	}

}
