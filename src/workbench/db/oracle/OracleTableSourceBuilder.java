/*
 * OracleTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.db.*;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableSourceBuilder
	extends TableSourceBuilder
{
	private static final String INDEX_USAGE_PLACEHOLDER = "%pk_index_usage%";
	private String defaultTablespace;

	public OracleTableSourceBuilder(WbConnection con)
	{
		super(con);
		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.check_default_tablespace", false))
		{
			readDefaultTableSpace();
		}
	}

	private void readDefaultTableSpace()
	{
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "select default_tablespace from user_users";

		try
		{
			stmt = this.dbConnection.createStatementForQuery();
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readDefaultTableSpace()", "Using sql: " + sql);
			}

			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				this.defaultTablespace = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readDefaultTableSpace()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Override
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		StringBuilder result = new StringBuilder(100);
		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_partitions", true))
		{
			try
			{
				OracleTablePartition reader = new OracleTablePartition(this.dbConnection);
				reader.retrieve(table, dbConnection);
				String sql = reader.getSourceForTableDefinition();
				if (sql != null)
				{
					result.append(sql);
				}
			}
			catch (SQLException sql)
			{
				LogMgr.logError("OracleTableSourceBuilder.getAdditionalTableOptions()", "Error retrieving partitions", sql);
			}
		}

		if (StringUtil.isNonEmpty(table.getTableConfigOptions()))
		{
			if (result.length() > 0)
			{
				result.append('\n');
			}
			result.append(table.getTableConfigOptions());
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_tablespace", true))
		{
			String tablespace = table.getTablespace();
			if (StringUtil.isNonEmpty(tablespace) && !tablespace.equals(defaultTablespace))
			{
				if (result.length() > 0)
				{
					result.append('\n');
				}
				result.append("TABLESPACE ");
				result.append(tablespace);
			}
		}
		return result.toString();
	}


	/**
	 * Read additional options for the CREATE TABLE part.
	 *
	 * @param tbl
	 */
	@Override
	public void readTableConfigOptions(TableIdentifier tbl)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select tablespace_name, degree, row_movement \n" +
			"from all_tables  \n" +
			"where owner = ? \n" +
			"and table_name = ? ";

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getSchema());
			pstmt.setString(2, tbl.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readTableConfigOptions()", "Using sql: " + pstmt.toString());
			}

			String options = "";

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String tablespace = rs.getString(1);
				tbl.setTablespace(tablespace);
				String degree = rs.getString(2);
				if (degree != null) degree = degree.trim();
				if (!StringUtil.equalString("1", degree))
				{
					if ("DEFAULT".equals(degree))
					{
						options = "PARALLEL";
					}
					else
					{
						options = "PARALLEL " + degree;
					}
				}
				String movement = rs.getString(3);
				if (movement != null) movement = movement.trim();
				if (StringUtil.equalString("ENABLED", movement))
				{
					if (options.length() > 0) options += "\n";
					options += "ENABLE ROW MOVEMENT";
				}
			}
			tbl.setTableConfigOptions(options);
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readTableConfigOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

	/**
	 * Generate the SQL to create the primary key for the table.
	 *
	 * If the primary key is supported by an index that does not have the same name
	 * as the primary key, it is assumed that the index is defined as an additional
	 * option to the ADD CONSTRAINT SQL...
	 *
	 * @param table the table for which the PK source should be created
	 * @param pkCols a List of PK column names
	 * @param pkName the name of the primary key
	 * @return the SQL to re-create the primary key
	 */
	@Override
	public CharSequence getPkSource(TableIdentifier table, List<String> pkCols, String pkName)
	{
		OracleIndexReader reader = (OracleIndexReader)dbConnection.getMetadata().getIndexReader();
		String pkIndex = reader.getPrimaryKeyIndex(table);
		String sql = super.getPkSource(table, pkCols, pkName).toString();
		IndexDefinition idx = getIndexDefinition(table, pkIndex);
		boolean isPartitioned = false;

		try
		{
			OracleIndexPartition partIndex =  new OracleIndexPartition(this.dbConnection);
			partIndex.retrieve(idx, dbConnection);
			isPartitioned = partIndex.isPartitioned();
		}
		catch (SQLException ex)
		{
			isPartitioned = false;
		}

		if (pkIndex.equals(pkName) && !isPartitioned)
		{
			sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, "");
		}
		else
		{
			String indexSql = reader.getExtendedIndexSource(table, idx, null, "    ").toString();
			if ("NORMAL/REV".equals(idx.getIndexType()))
			{
				indexSql = indexSql.replace("\n    REVERSE", " REVERSE"); // cosmetic cleanup
			}
			StringBuilder using = new StringBuilder(indexSql.length() + 20);
			using.append("\n   USING INDEX (\n     ");
			using.append(SqlUtil.trimSemicolon(indexSql).trim().replace("\n", "\n  "));
			using.append("\n   )");
			sql = sql.replace(INDEX_USAGE_PLACEHOLDER, using);
		}
		return sql;
	}

	private IndexDefinition getIndexDefinition(TableIdentifier table, String indexName)
	{
		OracleIndexReader reader = (OracleIndexReader)dbConnection.getMetadata().getIndexReader();
		try
		{
			IndexDefinition index = reader.getIndexDefinition(table, indexName, null);
			return index;
		}
		catch (SQLException sql)
		{
			LogMgr.logError("OracleTableSourceBuilder.getIndexDefinition()", "Could not retrieve index", sql);
		}
		return null;
	}

}
