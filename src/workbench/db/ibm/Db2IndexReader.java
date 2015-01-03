/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2IndexReader
	extends JdbcIndexReader
{
	private static final String KEY_INCLUDED_COLS = "include_columns";

	public Db2IndexReader(DbMetadata meta)
	{
		super(meta);
	}

	@Override
	public String getIndexOptions(TableIdentifier table, IndexDefinition index)
	{
		String type = index.getIndexType();
		if ("CLUSTERED".equals(type))
		{
			return " CLUSTER";
		}

		if (!index.getSourceOptions().isInitialized())
		{
			readIndexOptions(table, index);
		}

		String include = index.getSourceOptions().getConfigSettings().get(KEY_INCLUDED_COLS);
		if (include != null)
		{
			return " INCLUDE (" + include + ")";
		}

		return null;
	}

	private void readIndexOptions(TableIdentifier table, IndexDefinition index)
	{
		String sql =
			"select colcount, unique_colcount \n" +
			"from syscat.indexes \n" +
			"where indschema = ? \n" +
			"  and indname = ? \n" +
			"  and tabname = ? \n"  +
			"  and tabschema = ? ";

		PreparedStatement stmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("Db2IndexReader.readIndexOptions()", "Retrieving index information using:\n" +
				SqlUtil.replaceParameters(sql, index.getSchema(), index.getName(), table.getRawTableName(), table.getRawSchema()));
		}

		try
		{
			stmt = metaData.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, index.getSchema());
			stmt.setString(2, index.getName());
			stmt.setString(3, table.getRawTableName());
			stmt.setString(4, table.getRawSchema());

			rs = stmt.executeQuery();
			if (rs.next())
			{
				int colCount = rs.getInt(1);
				int uniqueCols = rs.getInt(2);
				if (uniqueCols < colCount)
				{
					List<IndexColumn> cols = index.getColumns();
					String includedCols = "";
					for (int c = uniqueCols; c < cols.size(); c++)
					{
						if (c > uniqueCols) includedCols += ",";
						includedCols += cols.get(c).getColumn();
					}
					for (int c = cols.size() - 1; c > uniqueCols - 1; c-- )
					{
						cols.remove(c);
					}
					index.getSourceOptions().addConfigSetting(KEY_INCLUDED_COLS, includedCols);
					index.getSourceOptions().setInitialized();
				}
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("Db2IndexReader.readIndexOptions()", "Could not read index options using:\n" + sql, ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}


}
