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

	/**
	 * Read additional options for the CREATE TABLE part.
	 *
	 * @param tbl        the table for which the options should be retrieved
	 * @param columns    the table's columns
	 */
	@Override
	public void readTableOptions(TableIdentifier tbl, List<ColumnIdentifier> columns)
	{
		if (tbl.getSourceOptions().isInitialized()) return;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select /* SQLWorkbench */ atb.tablespace_name, \n" +
			"       atb.degree, \n" +
			"       atb.row_movement, \n" +
			"       atb.temporary, \n" +
			"       atb.duration, \n" +
			"       atb.pct_free, \n" +
			"       atb.pct_used, \n" +
			"       atb.pct_increase, \n" +
			"       atb.logging, \n" +
			"       atb.iot_type, \n" +
			"       iot.tablespace_name as iot_overflow \n" +
			"from all_tables atb \n" +
			"  left join all_tables iot on atb.owner = iot.owner and atb.table_name = iot.iot_name \n" +
			"where atb.owner = ? \n" +
			"and atb.table_name = ? ";

		StringBuilder options = new StringBuilder(100);

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getSchema());
			pstmt.setString(2, tbl.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readTableOptions()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()));
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String tablespace = rs.getString("tablespace_name");
				tbl.setTablespace(tablespace);

				String iot = rs.getString("IOT_TYPE");
				if (StringUtil.isNonBlank(iot))
				{
					tbl.getSourceOptions().addConfigSetting("organization", "index");
					options.append("ORGANIZATION INDEX");
					String overflow = rs.getString("IOT_OVERFLOW");
					if (StringUtil.isNonBlank(overflow))
					{
						options.append("\nOVERFLOW TABLESPACE ");
						options.append(overflow);
						tbl.getSourceOptions().addConfigSetting("overflow_tablespace", "overflow");
					}
					tbl.setUseInlinePK(true); // you cannot define a IOT without a PK therefor the PK has to be inline!
				}

				String degree = rs.getString("degree");
				if (degree != null) degree = degree.trim();
				if (!StringUtil.equalString("1", degree))
				{
					if (options.length() > 0) options.append('\n');
					if ("DEFAULT".equals(degree))
					{
						options.append("PARALLEL");
						tbl.getSourceOptions().addConfigSetting("parallel", "default");  // make this show in the XML schema report
					}
					else
					{
						options.append("PARALLEL " + degree);
						tbl.getSourceOptions().addConfigSetting("parallel", degree);
					}
				}
				String movement = rs.getString("row_movement");
				if (movement != null) movement = movement.trim();
				if (StringUtil.equalString("ENABLED", movement))
				{
					if (options.length() > 0) options.append('\n');
					options.append("ENABLE ROW MOVEMENT");
					tbl.getSourceOptions().addConfigSetting("row_movement", "enabled");
				}

				String tempTable = rs.getString("temporary");
				String duration = rs.getString("duration");
				if (StringUtil.equalString("Y", tempTable))
				{
					tbl.getSourceOptions().setTypeModifier("GLOBAL TEMPORARY");
					if (options.length() > 0) options.append('\n');
					if (StringUtil.equalString("SYS$TRANSACTION", duration))
					{
						options.append("ON COMMIT DELETE ROWS");
						tbl.getSourceOptions().addConfigSetting("on_commit", "delete");
					}
					else if (StringUtil.equalString("SYS$SESSION", duration))
					{
						options.append("ON COMMIT PRESERVE ROWS");
						tbl.getSourceOptions().addConfigSetting("on_commit", "preserve");
					}
				}

				int free = rs.getInt("pct_free");
				if (!rs.wasNull() && free != 10)
				{
					tbl.getSourceOptions().addConfigSetting("pct_free", Integer.toString(free));
					if (options.length() > 0) options.append('\n');
					options.append("PCTFREE");
					options.append(free);
				}

				int used = rs.getInt("pct_used");
				if (!rs.wasNull() && used != 40)
				{
					tbl.getSourceOptions().addConfigSetting("pct_used", Integer.toString(used));
					if (options.length() > 0) options.append('\n');
					options.append("PCTUSED");
					options.append(used);
				}

				String logging = rs.getString("logging");
				if (StringUtil.equalStringIgnoreCase("NO", logging))
				{
					tbl.getSourceOptions().addConfigSetting("logging", logging);
					if (options.length() > 0) options.append('\n');
					options.append("NOLOGGING");
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readTableOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}

		String tablespace = tbl.getTablespace();
		if (OracleUtils.shouldAppendTablespace(tablespace, defaultTablespace, tbl.getRawSchema(), dbConnection.getCurrentUser()))
		{
			if (options.length() > 0)
			{
				options.append('\n');
			}
			options.append("TABLESPACE ");
			options.append(tablespace);
		}

		if (includePartitions)
		{
			StringBuilder partition = getPartitionSql(tbl);
			if (partition != null && partition.length() > 0)
			{
				if (options.length() > 0 && options.charAt(options.length() - 1) != '\n')
				{
					options.append('\n');
				}
				options.append(partition);
			}
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_externaltables", true))
		{
			OracleExternalTableReader reader = new OracleExternalTableReader();
			CharSequence ext = reader.getDefinition(tbl, dbConnection);
			if (ext != null)
			{
				if (options.length() > 0)
				{
					options.append('\n');
				}
				options.append(ext);
			}
		}

		StringBuilder nested = getNestedTableSql(tbl, columns);
		if (nested != null && nested.length() > 0)
		{
			options.append(nested);
		}

		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_flashback", false))
		{
			retrieveFlashbackInfo(tbl);
		}
		tbl.getSourceOptions().setTableOption(options.toString());
		tbl.getSourceOptions().setInitialized();
	}

	private void retrieveFlashbackInfo(TableIdentifier tbl)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;


		// Using the table's tablespace for the flashback archive is not correct,
		// but there isn't a way to retrieve that information as far as I can tell
		// (not even SQL Developer displays the flashback archive information!)
		String sql =
			"select fa.flashback_archive_name,   \n" +
			"       fa.retention_in_days, \n" +
			"       tbl.tablespace_name \n" +
			"from dba_flashback_archive fa  \n" +  // this should be user_flashback_archive but that does not contain any information!
			"  join user_flashback_archive_tables fat  \n" +
			"    on fat.flashback_archive_name = fa.flashback_archive_name  \n" +
			"  join all_tables tbl  \n" +
			"    on tbl.owner = fat.owner_name  \n" +
			"   and tbl.table_name = fat.table_name \n" +
		  "where fat.owner_name = ? \n" +
			"  and fat.table_name = ? ";

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getSchema());
			pstmt.setString(2, tbl.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.retrieveFlashbackInfo()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()));
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String archiveName = rs.getString(1);
				int days = rs.getInt(2);
				String tbSpace = rs.getString(3);
				String rentention = "RETENTION ";
				if (days < 30)
				{
					rentention += Integer.toString(days) + " DAY";
				}
				else if (days < 365)
				{
					rentention += Integer.toString(days / 30) + " MONTH";
				}
				else
				{
					rentention += Integer.toString(days / 365) + " YEAR";
				}
				String create =
					"CREATE FLASHBACK ARCHIVE " + archiveName + "\n" +
					"  TABLESPACE " + tbSpace +"\n  " + rentention + ";\n" +
					"ALTER TABLE " + tbl.getTableExpression(dbConnection) + "\n" +
					"  FLASHBACK ARCHIVE " + archiveName + ";";

				tbl.getSourceOptions().setAdditionalSql(create);
			}
		}
		catch (Exception ex)
		{
			LogMgr.logWarning("OracleTableSourceBuilder.retrieveFlashbackInfo()", "Could not retrieve flashback information", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
	}

	private StringBuilder getPartitionSql(TableIdentifier table)
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
				LogMgr.logError("OracleTableSourceBuilder.getPartitionSql()", "Error retrieving partitions", sql);
			}
		}
		return result;
	}

	private StringBuilder getNestedTableSql(TableIdentifier tbl, List<ColumnIdentifier> columns)
	{
		StringBuilder options = new StringBuilder();

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

		if (!hasUserType) return null;

		String sql =
			"SELECT /* SQLWorkbench */ 'NESTED TABLE '||parent_table_column||' STORE AS '||table_name \n" +
			"FROM all_nested_tables \n" +
			"WHERE parent_table_name = ? \n" +
			"  AND owner = ?";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getTableName());
			pstmt.setString(2, tbl.getSchema());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.getNestedTableSql()", "Using sql:\n" +
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
			LogMgr.logError("OracleTableSourceBuilder.getNestedTableSql()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return options;
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
		String sql = super.getPkSource(table, def, forInlineUse).toString();
		if (StringUtil.isEmptyString(sql)) return sql;

		PkDefinition pk = def == null ? table.getPrimaryKey() : def;

		// The name used by the index is not necessarily the same as the one used by the constraint.
		String pkIndexName = pk.getPkIndexName();

		boolean pkEnabled = pk.isEnabled() != null ? pk.isEnabled().booleanValue() : true;
		IndexDefinition pkIdx = null;
		boolean isPartitioned = false;

		if (pkEnabled)
		{
			pkIdx = getIndexDefinition(table, pkIndexName);
		}

		try
		{
			if (pkIdx != null)
			{
				OracleIndexPartition partIndex =  new OracleIndexPartition(this.dbConnection);
				partIndex.retrieve(pkIdx, dbConnection);
				isPartitioned = partIndex.isPartitioned();
			}
		}
		catch (SQLException ex)
		{
			isPartitioned = false;
		}

		if (!pkEnabled || pkIdx == null )
		{
			sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, " DISABLE");
		}
		else if (pkIndexName.equals(pk.getPkName()) && !isPartitioned)
		{
			if (pkIdx != null && OracleUtils.shouldAppendTablespace(pkIdx.getTablespace(), defaultTablespace, pkIdx.getSchema(), dbConnection.getCurrentUser()))
			{
				sql = sql.replace(INDEX_USAGE_PLACEHOLDER, "\n   USING INDEX TABLESPACE " + pkIdx.getTablespace());
			}
			else
			{
				sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, "");
			}
		}
		else
		{
			String indexSql = reader.getExtendedIndexSource(table, pkIdx, "    ").toString();
			if ("NORMAL/REV".equals(pkIdx.getIndexType()))
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
