/*
 * OracleTableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import workbench.db.DependencyNode;
import workbench.db.IndexDefinition;
import workbench.db.JdbcUtils;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.db.sqltemplates.TemplateHandler;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableSourceBuilder
	extends TableSourceBuilder
{
	private static final String REV_IDX_TYPE = "NORMAL/REV";
	private static final String INDEX_USAGE_PLACEHOLDER = "%pk_index_usage%";
	private String defaultTablespace;
	private String currentUser;

	public OracleTableSourceBuilder(WbConnection con)
	{
		super(con);
		if (OracleUtils.checkDefaultTablespace())
		{
			defaultTablespace = OracleUtils.getDefaultTablespace(con);
		}
		currentUser = con.getCurrentUser();
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

		StringBuilder options = new StringBuilder(100);

		CharSequence externalDef;
		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_externaltables", true))
		{
			OracleExternalTableReader reader = new OracleExternalTableReader();
			externalDef = reader.getDefinition(tbl, dbConnection);
			if (externalDef != null)
			{
				options.append(externalDef);
				tbl.getSourceOptions().setTableOption(options.toString());
				tbl.getSourceOptions().setInitialized();
				return;
			}
		}

		boolean supportsArchives = JdbcUtils.hasMinimumServerVersion(dbConnection, "11.2");
		boolean supportsCompression = JdbcUtils.hasMinimumServerVersion(dbConnection, "11.1");

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		boolean useUserTables = false;
		if (OracleUtils.optimizeCatalogQueries())
		{
			String schema = tbl.getRawSchema();
			if (StringUtil.isEmptyString(schema) || schema.equalsIgnoreCase(currentUser))
			{
				useUserTables = true;
			}
		}

		String archiveJoin = "";
		if (supportsArchives)
		{
			if (useUserTables)
			{
				archiveJoin = "  left join user_flashback_archive_tables fat on fat.table_name = atb.table_name \n";
			}
			else
			{
				archiveJoin = "  left join dba_flashback_archive_tables fat on fat.table_name = atb.table_name and fat.owner_name = atb.owner \n";
			}
		}

		String sql =
			"select /* SQLWorkbench */ coalesce(atb.tablespace_name, pt.def_tablespace_name) as tablespace_name, \n" +
			"       atb.degree, \n" +
			"       atb.row_movement, \n" +
			"       atb.temporary, \n" +
			"       atb.duration, \n" +
			"       atb.pct_free, \n" +
			"       atb.pct_used, \n" +
			"       atb.pct_increase, \n" +
			"       atb.logging, \n" +
			"       atb.iot_type, \n" +
			"       iot.tablespace_name as iot_overflow, \n" +
			"       iot.table_name as overflow_table, \n" +
			"       ac.index_name as pk_index_name, \n" +
			"       ai.compression as index_compression, \n" +
			"       ai.prefix_length, \n" +
			"       ai.tablespace_name as index_tablespace, \n" +
			(supportsArchives ?
			"       fat.flashback_archive_name, \n" :
			"       null as flashback_archive_name, \n") +
			(supportsCompression ?
			"       atb.compression, \n " +
			"       atb.compress_for \n " :
			"       null as compression, \n       null as compress_for \n ") +
			"from all_tables atb \n" +
			"  left join all_tables iot on atb.table_name = iot.iot_name " + (useUserTables ? "\n" : " and atb.owner = iot.owner \n")  +
			"  left join all_constraints ac on ac.table_name = atb.table_name and ac.constraint_type = 'P' " + (useUserTables ? "\n" : " and ac.owner = atb.owner \n") +
			"  left join all_indexes ai on ai.table_name = ac.table_name and ai.index_name = ac.index_name " + (useUserTables ? "\n" : " and ai.owner = coalesce(ac.index_owner, ac.owner) \n") +
			archiveJoin +
			"  left join all_part_tables pt on pt.table_name = iot.table_name " + (useUserTables ? "\n" : " and pt.owner = iot.owner \n") +
			"where atb.table_name = ? ";

		if (useUserTables)
		{
			sql = sql.replace(" all_", " user_");
		}
		else
		{
			sql += "\n and atb.owner = ?";
		}

		String archive = null;
		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getRawTableName());
			if (!useUserTables)
			{
				pstmt.setString(2, tbl.getRawSchema());
			}
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.readTableOptions()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, tbl.getTableName(), tbl.getSchema()));
			}

			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String tablespace = rs.getString("tablespace_name");
				tbl.setTablespace(tablespace);

				String iot = rs.getString("IOT_TYPE");
				if (StringUtil.isNonBlank(iot))
				{
					String pkIndex = rs.getString("pk_index_name");
					String overflow = rs.getString("IOT_OVERFLOW");
					tbl.getSourceOptions().addConfigSetting("organization", "index");
					options.append("ORGANIZATION INDEX");

					String compression = rs.getString("index_compression");
					if ("ENABLED".equalsIgnoreCase(compression))
					{
						String cols = rs.getString("prefix_length");
						if (StringUtil.isNonBlank(cols))
						{
							options.append("\n  COMPRESS ");
							options.append(cols);
						}
					}
					String included = getIOTIncludedColumn(tbl.getSchema(), tbl.getTableName(), pkIndex);
					if (included != null)
					{
						options.append("\n  INCLUDING ");
						options.append(included);
						if (StringUtil.isEmptyString(overflow))
						{
							options.append(" OVERFLOW");
						}
						tbl.getSourceOptions().addConfigSetting("iot_included_cols", included);
					}

					String idxTbs = rs.getString("INDEX_TABLESPACE");
					if (StringUtil.isNonEmpty(idxTbs))
					{
						options.append("\n  TABLESPACE ").append(idxTbs);
						tbl.getSourceOptions().addConfigSetting("index_tablespace", idxTbs);
						tbl.setTablespace(null);
					}

					if (StringUtil.isNonBlank(overflow))
					{
						options.append("\n  OVERFLOW TABLESPACE ");
						options.append(overflow);
						tbl.getSourceOptions().addConfigSetting("overflow_tablespace", overflow);
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
				boolean isTempTable = StringUtil.equalString("Y", tempTable);
				String duration = rs.getString("duration");
				if (isTempTable)
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
					tbl.setTablespace(null); // temporary tables can't have a tablespace
				}

				int free = rs.getInt("pct_free");
				if (!rs.wasNull() && free != 10 && StringUtil.isEmptyString(iot))
				{
					tbl.getSourceOptions().addConfigSetting("pct_free", Integer.toString(free));
					if (options.length() > 0) options.append('\n');
					options.append("PCTFREE ");
					options.append(free);
				}

				int used = rs.getInt("pct_used");
				if (!rs.wasNull() && used != 40 && StringUtil.isEmptyString(iot) && !isTempTable) // PCTUSED is not valid for IOTs
				{
					tbl.getSourceOptions().addConfigSetting("pct_used", Integer.toString(used));
					if (options.length() > 0) options.append('\n');
					options.append("PCTUSED ");
					options.append(used);
				}

				String logging = rs.getString("logging");
				if (StringUtil.equalStringIgnoreCase("NO", logging) && !isTempTable)
				{
					tbl.getSourceOptions().addConfigSetting("logging", "nologging");
					if (options.length() > 0) options.append('\n');
					options.append("NOLOGGING");
				}

				String compression = rs.getString("compression");
				if (StringUtil.equalStringIgnoreCase("enabled", compression))
				{
					String compressType = rs.getString("compress_for");
					tbl.getSourceOptions().addConfigSetting("compression", compressType);
					if (options.length() > 0) options.append('\n');
					options.append("COMPRESS FOR ");
					options.append(compressType);
				}

				archive = rs.getString("flashback_archive_name");
				if (StringUtil.isNonEmpty(archive))
				{
					tbl.getSourceOptions().addConfigSetting("flashback_archive", archive);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("OracleTableSourceBuilder.readTableOptions()", "Error retrieving table options using SQL: \n" + sql, e);
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

		if (StringUtil.isNonEmpty(archive))
		{
			if (options.length() > 0) options.append('\n');
			options.append("FLASHBACK ARCHIVE ");
			options.append(dbConnection.getMetadata().quoteObjectname(archive));
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

		StringBuilder nested = getNestedTableSql(tbl, columns);
		if (nested != null && nested.length() > 0)
		{
			if (options.length() > 0) options.append('\n');
			options.append(nested);
		}

		if (supportsArchives && Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_flashback", false))
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
					"\n-- definition of flasback archive \n" +
					"CREATE FLASHBACK ARCHIVE " + archiveName + "\n" +
					"  TABLESPACE " + tbSpace +"\n  " + rentention + ";\n";

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
			while (rs.next())
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
	 * @param table        the table for which the PK source should be created
	 * @param def          the definition of the primary key of the table
	 * @param forInlineUse if true, the SQL should be generated so it can be used inside a CREATE TABLE
	 *                     otherwise an ALTER TABLE will be created
	 * <p>
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

		boolean pkIdxReverse = pkIdx != null && REV_IDX_TYPE.equals(pkIdx.getIndexType());

		if (!pkEnabled || pkIdx == null )
		{
			sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, " DISABLE");
		}
		else if (pkIndexName.equals(pk.getPkName()) && !isPartitioned)
		{
			if (OracleUtils.shouldAppendTablespace(pkIdx.getTablespace(), defaultTablespace, pkIdx.getSchema(), dbConnection.getCurrentUser()))
			{
				String idx = "USING INDEX";
				if (pkIdxReverse)
				{
					idx += " REVERSE";
				}
				sql = sql.replace(INDEX_USAGE_PLACEHOLDER, "\n   " + idx + " TABLESPACE " + pkIdx.getTablespace());
			}
			else
			{
				sql = sql.replace(" " + INDEX_USAGE_PLACEHOLDER, "");
			}
		}
		else
		{
			String indexSql = reader.getIndexSource(table, pkIdx).toString();
			if (pkIdxReverse)
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
		IndexDefinition index;
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

	private String getIOTIncludedColumn(String owner, String tableName, String pkIndexName)
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select column_name \n" +
			"from all_tab_columns \n" +
			"where column_name not in (select column_name \n" +
			"                          from all_ind_columns  \n" +
			"                          where index_name = ? " +
			"                            and index_owner = ?) \n" +
			"  and table_name = ? \n" +
			"  and owner = ? \n" +
			"order by column_id \n";

		String column = null;
		try
		{
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, pkIndexName);
			pstmt.setString(2, owner);
			pstmt.setString(3, tableName);
			pstmt.setString(4, owner);
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("OracleTableSourceBuilder.getIOTIncludedColumn()", "Using sql:\n" +
					SqlUtil.replaceParameters(sql, pkIndexName, owner, tableName, owner));
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				column = rs.getString(1);
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("OracleTableSourceBuilder.getIOTIncludedColumn()", "Could not retrieve included columns", ex);
			column = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return column;
	}

	@Override
	protected String getAdditionalFkSql(TableIdentifier table, DependencyNode fk, String template)
	{
		if (fk.isValidated())
		{
			template = TemplateHandler.removePlaceholder(template, "%validate%", false);
		}
		else
		{
			template = TemplateHandler.replacePlaceholder(template, "%validate%", "NOVALIDATE", true);
		}

		if (fk.isEnabled())
		{
			template = TemplateHandler.removePlaceholder(template, "%enabled%", false);
		}
		else
		{
			template = TemplateHandler.replacePlaceholder(template, "%enabled%", "DISABLE", true);
		}
		return template;
	}

}
