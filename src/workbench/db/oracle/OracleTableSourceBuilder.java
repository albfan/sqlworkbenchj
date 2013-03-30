/*
 * OracleTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

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
		if (OracleUtils.checkDefaultTablespace())
		{
			defaultTablespace = OracleUtils.getDefaultTablespace(con);
		}
	}

	@Override
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
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

		String tablespace = table.getTablespace();
		if (OracleUtils.shouldAppendTablespace(tablespace, defaultTablespace))
		{
			if (result.length() > 0)
			{
				result.append('\n');
			}
			result.append("TABLESPACE ");
			result.append(tablespace);
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_externaltables", true))
		{
			OracleExternalTableReader reader = new OracleExternalTableReader();
			CharSequence ext = reader.getDefinition(table, dbConnection);
			if (ext != null)
			{
				result.append(ext);
			}
		}

		// retrieving the nested table options requires another query to the Oracle
		// catalogs which is always horribly slow. In order to prevent this,
		// we first check if the table only contains "standard" data types.
		// as the number of tables containing nested tables is probably quite small
		// we prevent firing additional queries by checking if at least one column
		// might be a nested table
		boolean hasUserType = false;
		for (ColumnIdentifier col : columns)
		{
			String type = SqlUtil.getPlainTypeName(col.getDbmsType());
			if (!OracleUtils.STANDARD_TYPES.contains(type))
			{
				hasUserType = true;
				break;
			}
		}

		if (hasUserType)
		{
			String options = readNestedTableOptions(table);
			if (options.trim().length() > 0)
			{
				result.append('\n');
				result.append(options);
			}
		}
		return result.toString();
	}

	private String readNestedTableOptions(TableIdentifier tbl)
	{
		String sql =
			"SELECT /* SQLWorkbench */ 'NESTED TABLE '||parent_table_column||' STORE AS '||table_name \n" +
			"FROM all_nested_tables \n" +
			"WHERE parent_table_name = ? \n" +
			"  AND owner = ?";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder options = new StringBuilder();

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getTableName());
			pstmt.setString(2, tbl.getSchema());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readNestedTableOptions()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, tbl.getTableName(), tbl.getSchema()));
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String option = rs.getString(1);
				if (options.length() > 0)
				{
					options.append('\n');
				}
				options.append(option);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readNestedTableOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return options.toString();
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
			"select /* SQLWorkbench */ tablespace_name, degree, row_movement, temporary, duration \n" +
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
				LogMgr.logDebug("OracleTableSourceBuilder.readTableConfigOptions()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()));
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

				String tempTable = rs.getString(4);
				String duration = rs.getString(5);
				if (StringUtil.equalString("Y", tempTable))
				{
					tbl.setTableTypeOption("GLOBAL TEMPORARY");
					if (options.length() > 0) options += "\n";
					if (StringUtil.equalString("SYS$TRANSACTION", duration))
					{
						options += "ON COMMIT DELETE ROWS";
					}
					else if (StringUtil.equalString("SYS$SESSION", duration))
					{
						options += "ON COMMIT PRESERVE ROWS";
					}
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
	public CharSequence getPkSource(TableIdentifier table, PkDefinition def, boolean forInlineUse)
	{
		OracleIndexReader reader = (OracleIndexReader)dbConnection.getMetadata().getIndexReader();
		String sql = super.getPkSource(table, def, false).toString();
		if (StringUtil.isEmptyString(sql)) return sql;

		PkDefinition pk = def == null ? table.getPrimaryKey() : def;

		// The name used by the index is not necessarily the same as the one used by the constraint.
		String pkIndexName = pk.getPkIndexName();
		IndexDefinition idx = getIndexDefinition(table, pkIndexName);
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

		if (pkIndexName.equals(pk.getPkName()) && !isPartitioned)
		{
			sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, "");
		}
		else
		{
			String indexSql = reader.getExtendedIndexSource(table, idx, "    ").toString();
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
		IndexDefinition index = null;
		try
		{
			index = reader.getIndexDefinition(table, indexName, null);
		}
		catch (SQLException sql)
		{
			LogMgr.logError("OracleTableSourceBuilder.getIndexDefinition()", "Could not retrieve index", sql);
			index = null;
		}
		return index;
	}

}
