/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2iColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
    if (conn.getDbSettings().getBoolProperty("remarks.columns.use_columntext", false))
    {
      readColumnComments(table, conn);
    }
	}

	public void readColumnComments(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

    String tablename = conn.getMetadata().removeQuotes(table.getTable().getTableName());
		String schema = conn.getMetadata().removeQuotes(table.getTable().getSchema());

		String sql =
      "select column_name, \n" +
      "       column_text \n" +
      "from qsys2.syscolumns \n" +
      "where table_schema = ? \n" +
      "  and table_name  = ?";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2ColumnEnhancer.updateComputedColumns()", "Query to retrieve column comments:\n" + SqlUtil.replaceParameters(sql, schema, tablename));
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, tablename);
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String comment = rs.getString(2);
        if (StringUtil.isNonEmpty(comment))
        {
          ColumnIdentifier col = ColumnIdentifier.findColumnInList(table.getColumns(), colname);
          if (col != null)
          {
            col.setComment(comment);
          }
        }
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Db2ColumnEnhancer.updateComputedColumns()", "Error retrieving column comments using:\n" + SqlUtil.replaceParameters(sql, schema, tablename), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

}
