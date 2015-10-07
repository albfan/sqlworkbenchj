/*
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
package workbench.db.firebird;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdTableSourceBuilder
	extends TableSourceBuilder
{

	public FirebirdTableSourceBuilder(WbConnection con)
	{
		super(con);
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

		String sql =
			"select trim(t.rdb$type_name), rdb$external_file \n" +
			"from rdb$relations r \n" +
			"  join rdb$types t on r.rdb$relation_type = t.rdb$type and t.rdb$field_name = 'RDB$RELATION_TYPE' \n" +
			"where coalesce (r.rdb$system_flag, 0) = 0 \n" +
			"  and rdb$relation_name = ? ";

		PreparedStatement stmt = null;
		ResultSet rs = null;

		StringBuilder options = new StringBuilder(50);
		try
		{
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, tbl.getTableName());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				String type = rs.getString(1);
				if (StringUtil.equalStringIgnoreCase(type, "GLOBAL_TEMPORARY_PRESERVE"))
				{
					tbl.getSourceOptions().setTypeModifier("GLOBAL TEMPORARY");
					options.append("ON COMMIT PRESERVE ROWS");
					tbl.getSourceOptions().addConfigSetting("on_commit", "preserve");
				}
				else if (StringUtil.equalStringIgnoreCase(type, "GLOBAL_TEMPORARY_DELETE"))
				{
					tbl.getSourceOptions().setTypeModifier("GLOBAL TEMPORARY");
					options.append("ON COMMIT DELETE ROWS");
					tbl.getSourceOptions().addConfigSetting("on_commit", "delete");
				}
        if ("EXTERNAL".equals(type))
        {
          String fileName = rs.getString(2);
          tbl.getSourceOptions().addConfigSetting("external_file", fileName);
        }
			}
			tbl.getSourceOptions().setTableOption(options.toString());
			tbl.getSourceOptions().setInitialized();
		}
		catch (Exception ex)
		{
			LogMgr.logError("FirebirdTableSourceBuilder.readTableOptions()", "Could not read table options using query:\n" + sql, ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

  @Override
  protected String getCreateDDL(String objectType, TableIdentifier tbl)
  {
    String ddl = super.getCreateDDL(objectType, tbl);
    String file = tbl.getSourceOptions().getConfigSettings().get("external_file");
    if (StringUtil.isNonBlank(file))
    {
      ddl += "\nEXTERNAL FILE '" + file + "'";
    }
    return ddl;
  }


}
