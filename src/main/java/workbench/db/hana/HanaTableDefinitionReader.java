/*
 * HanaTableDefinitionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.hana;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DataTypeResolver;
import workbench.db.JdbcTableDefinitionReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;

/**
 * A class to retrieve the column information for a table in the HANA database.
 *
 * The current JDBC drivers take up to 5 minutes(!) to retrieve the columns for a single table,
 * by using our own statement, we are avoiding this bug.
 *
 * It seems this bug has been fixed with JDBC driver version 1.110
 *
 * @author Thomas Kellerer
 */
public class HanaTableDefinitionReader
	extends JdbcTableDefinitionReader
{
  public static final String PROP_USE_JDBC_GETCOLUMNS = "tabledefinition.usejdbc";

  private PreparedStatement columnsStatement;

  public HanaTableDefinitionReader(WbConnection conn)
  {
    super(conn);
  }

	@Override
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table, DataTypeResolver typeResolver)
		throws SQLException
	{
    try
    {
      List<ColumnIdentifier> columns = super.getTableColumns(table, typeResolver);
      return columns;
    }
    finally
    {
      SqlUtil.closeStatement(columnsStatement);
      columnsStatement = null;
    }
  }

  @Override
  protected void processColumnsResultRow(ResultSet rs, ColumnIdentifier col)
    throws SQLException
  {
    if (!useJDBC())
    {
      String generate = rs.getString("GENERATION_TYPE");
      if (generate != null)
      {
        col.setDefaultValue(null);
        col.setComputedColumnExpression("GENERATED " + generate);
        col.setIsAutoincrement(true);
      }
    }
  }

  private boolean useJDBC()
  {
    if (dbConnection == null) return false;
    return dbConnection.getDbSettings().getBoolProperty(PROP_USE_JDBC_GETCOLUMNS, true);
  }

  @Override
  protected ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern, String type)
    throws SQLException
  {
		if (useJDBC())
		{
			return super.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern, type);
		}

    String baseTable = "sys.table_columns";
    String nameColumn = "table_name";

    if (dbConnection.getMetadata().isViewType(type))
    {
      baseTable = "sys.view_columns";
      nameColumn = "view_name";
    }

    String sql =
      "select null as table_cat,  \n" +
      "       schema_name as table_schem,  \n" +
      "       " + nameColumn + " as table_name,  \n" +
      "       column_name,  \n" +
      "       case data_type_id \n" +
      "         when -10 then 2011 \n" +
      "         else data_type_id \n" +
      "       end as data_type, \n" +
      "       data_type_name as type_name,  \n" +
      "       length as column_size, \n" +
      "       null as buffer_length, \n" +
      "       case data_type_name \n" +
      "         when 'DECIMAL' then scale \n" +
      "         else null \n" +
      "       end as decimal_digits, \n" +
      "       case is_nullable \n" +
      "         when 'TRUE' then 1 \n" +
      "         else 0 \n" +
      "       end as nullable, \n" +
      "       comments as remarks, \n" +
      "       default_value as column_def, \n" +
      "       null as sql_data_type, \n" +
      "       null as sql_datetime_sub, \n" +
      "       case data_type_name \n" +
      "         when 'CHAR' then length \n" +
      "         when 'NCHAR' then length \n" +
      "         when 'VARCHAR' then length \n" +
      "         when 'NVARCHAR' then length \n" +
      "         else null \n" +
      "       end as char_octet_length, \n" +
      "       position as ordinal_position, \n" +
      "       case is_nullable \n" +
      "         when 'TRUE' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_nullable, \n" +
      "       case  \n" +
      "         when generation_type is not null then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_generatedcolumn, \n" +
      "       case  \n" +
      "         when generation_type LIKE '%AS IDENTITY' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_autoincrement, \n" +
      "       generation_type \n" +
      "from " + baseTable + " tc \n" +
      "where " + nameColumn + " LIKE ? ESCAPE '\\' \n" +
      "  and schema_name LIKE ? ESCAPE '\\' \n" +
      "order by position";

    columnsStatement = dbConnection.getSqlConnection().prepareStatement(sql);
    columnsStatement.setString(1, tableNamePattern);
    columnsStatement.setString(2, schemaPattern);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("HanaTableDefinitionReader.prepareColumnsStatement()", "Query to retrieve column information: " + SqlUtil.replaceParameters(sql, tableNamePattern, schemaPattern));
		}

    return columnsStatement.executeQuery();
  }
}
