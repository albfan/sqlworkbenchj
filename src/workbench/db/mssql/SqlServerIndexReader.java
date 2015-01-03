/*
 * SqlServerIndexReader.java
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
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerIndexReader
	extends JdbcIndexReader
{
	private boolean checkOptions;
	private boolean checkFilteredIndex;

	public SqlServerIndexReader(DbMetadata meta)
	{
		super(meta);
		checkOptions = SqlServerUtil.isSqlServer2005(meta.getWbConnection());
		checkFilteredIndex = SqlServerUtil.isSqlServer2008(meta.getWbConnection());
	}

	@Override
	public String getSQLKeywordForType(String type)
	{
		if (type == null) return "";
		if (type.equals("NORMAL")) return "NONCLUSTERED";
		return type;
	}

	@Override
	public String getIndexOptions(TableIdentifier table, IndexDefinition index)
	{
		if (!checkOptions) return null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String select = "select ix.ignore_dup_key, ix.is_padded, ic.is_included_column, ix.fill_factor";
		if (checkFilteredIndex)
		{
			select += ", ix.filter_definition";
		}
		else
		{
			select += ", null as filter_definition";
		}

		String sql =
			select + ", col.name as column_name \n" +
			"from sys.index_columns ic with (nolock) \n" +
			"  join sys.columns col with (nolock) on col.object_id = ic.object_id and col.column_id = ic.column_id \n" +
			"  join sys.indexes ix with (nolock) on ix.index_id = ic.index_id and ix.object_id = col.object_id \n" +
			"  join sys.all_objects ao with (nolock) on ao.object_id = ix.object_id \n" +
			"  join sys.schemas sh with (nolock) on sh.schema_id = ao.schema_id \n" +
			"where ix.name = ? \n" +
			"  and ao.name = ? \n" +
			"  and sh.name = ? \n" +
			"order by ic.index_column_id";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("SqlServerIndexReader.getIncludedColumns()", "Retrieving index information using:\n" +
				SqlUtil.replaceParameters(sql, index.getName(), table.getRawTableName(), table.getRawSchema()));
		}

		List<String> cols = new ArrayList<>();
		boolean ignoreDups = false;
		boolean isPadded = false;
		int fillFactor = -1;
		String filter = null;
		try
		{
			pstmt = this.metaData.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, index.getName());
			pstmt.setString(2, table.getRawTableName());
			pstmt.setString(3, table.getRawSchema());
			rs = pstmt.executeQuery();
			boolean first = true;
			while (rs.next())
			{
				if (first)
				{
					ignoreDups = rs.getBoolean("ignore_dup_key");
					isPadded = rs.getBoolean("is_padded");
					fillFactor = rs.getInt("fill_factor");
					filter = rs.getString("filter_definition");
					first = false;
				}
				boolean isIncluded = rs.getBoolean("is_included_column");
				if (isIncluded)
				{
					cols.add(rs.getString("column_name"));
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("SqlServerIndexReader.getIncludedColumns()", "Could not read included columns", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}

		StringBuilder options = new StringBuilder(cols.size() * 20);
		if (cols.size() > 0)
		{
			options.append("\n   INCLUDE (");
			for (int i=0; i < cols.size(); i++)
			{
				if (i > 0) options.append(", ");
				options.append(cols.get(i));
			}
			options.append(')');
			index.getSourceOptions().addConfigSetting("include_columns", StringUtil.listToString(cols, ','));
		}

		if (StringUtil.isNonBlank(filter))
		{
			options.append("\n   WHERE ");
			options.append(filter.trim());
			index.getSourceOptions().addConfigSetting("filter", filter.trim());
		}

		if (isPadded || ignoreDups || fillFactor > 0)
		{
			options.append("\n   WITH (");
			int optCount = 0;
			if (ignoreDups)
			{
				options.append("IGNORE_DUP_KEY = ON");
				optCount ++;
				index.getSourceOptions().addConfigSetting("ignore_dup_key", "true");
			}

			if (fillFactor > 0)
			{
				if (optCount > 0) options.append(", ");
				options.append("FILLFACTOR = ");
				options.append(fillFactor);
				optCount ++;
				index.getSourceOptions().addConfigSetting("fillfactor", Integer.toString(fillFactor));
			}
			if (isPadded)
			{
				if (optCount > 0) options.append(", ");
				options.append("PAD_INDEX = ON");
				index.getSourceOptions().addConfigSetting("pad_index", "true");
				optCount ++;
			}
			options.append(')');
		}
		return options.toString();
	}


	@Override
	public boolean supportsIndexList()
	{
		return SqlServerUtil.isSqlServer2005(metaData.getWbConnection());
	}

	@Override
	public List<IndexDefinition> getIndexes(String catalogPattern, String schemaPattern, String tablePattern, String indexNamePattern)
	{
		if (SqlServerUtil.isSqlServer2005(metaData.getWbConnection()))
		{
			return super.getIndexes(catalogPattern, schemaPattern, tablePattern, indexNamePattern);
		}
		return Collections.emptyList();
	}

}
