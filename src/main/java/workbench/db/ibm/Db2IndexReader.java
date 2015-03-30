/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
  public void processIndexList(Collection<IndexDefinition> indexList)
  {
    if (JdbcUtils.hasMinimumServerVersion(metaData.getSqlConnection(), "10.5"))
    {
      readExpressionIndex(indexList);
    }
  }

  private void readExpressionIndex(Collection<IndexDefinition> indexList)
  {
    String sql =
    "select indschema, indname, colname, text \n" +
    "from syscat.indexcoluse \n" +
    "where text is not null \n " +
    "  and (";

    int counter = 0;
    for (IndexDefinition idx : indexList)
    {
      if (mightHaveExpression(idx))
      {
        if (counter > 0) sql += " or\n        ";
        sql += " (indschema, indname) = (";
        sql += "'" + idx.getSchema() + "', '" + idx.getName() + "')";
        counter++;
      }
    }

    sql += ")\n" +
      "order by indschema, indname, colseq \n" +
      "for read only ";

    if (counter == 0) return;

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("Db2IndexReader.readExpressionIndex()", "Retrieving index expressions using:\n" + sql);
    }

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = metaData.getWbConnection().createStatementForQuery();
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        String schema = rs.getString(1);
        String idxName = rs.getString(2);
        String colname = rs.getString(3);
        String expression = rs.getString(4);

        IndexDefinition idx = IndexDefinition.findIndex(indexList, idxName, schema);
        updateIndexColumn(idx, colname, expression);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("Db2IndexReader.readExpressionIndex()", "Could not read expression index definition using:\n" + sql, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  private boolean mightHaveExpression(IndexDefinition index)
  {
    String regex = metaData.getDbSettings().getProperty("expression_index.colname.pattern", null);
    if (regex == null) return true;
    try
    {
      Pattern p = Pattern.compile(regex);
      for (IndexColumn col : index.getColumns())
      {
        Matcher m = p.matcher(col.getColumn());
        if (m.matches()) return true;
      }
      return false;
    }
    catch (Exception ex)
    {
      return true;
    }
  }

  private void updateIndexColumn(IndexDefinition idx, String colName, String expression)
  {
    if (idx == null) return;
    if (StringUtil.isEmptyString(colName)) return;
    if (StringUtil.isBlank(expression)) return;
    for (IndexColumn col : idx.getColumns())
    {
      if (col.getColumn().equalsIgnoreCase(colName))
      {
        col.setColumn(SqlUtil.removeObjectQuotes(expression));
      }
    }
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
			"select colcount, unique_colcount, remarks \n" +
			"from syscat.indexes \n" +
			"where indschema = ? \n" +
			"  and indname = ? \n" +
			"  and tabname = ? \n"  +
			"  and tabschema = ? \n" +
      "for read only";

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
				if (uniqueCols > -1 && uniqueCols < colCount)
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
        index.setComment(rs.getString(3));
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("Db2IndexReader.readIndexOptions()", "Could not read index options using:\n" +
        SqlUtil.replaceParameters(sql, index.getSchema(), index.getName(), table.getRawTableName(), table.getRawSchema()), ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}


}
