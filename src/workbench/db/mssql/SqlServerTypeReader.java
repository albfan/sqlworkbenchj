/*
 * SqlServerTypeReader.java
 *
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
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.ObjectListExtender;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTypeReader
  implements ObjectListExtender
{
  private final String baseSql =
    "select db_name() as type_catalog, \n" +
    "       s.name as type_schema, \n" +
    "       t.name as type_name,  \n" +
    "       type_name(system_type_id) as data_type, \n" +
    "       t.is_nullable,  \n" +
    "       t.max_length,  \n" +
    "       t.scale,  \n" +
    "       t.precision,  \n" +
    "       t.collation_name \n" +
    "from sys.types t with (nolock) \n" +
    "  join sys.schemas s with (nolock) on t.schema_id = s.schema_id \n" +
    "where t.is_user_defined = 1";

  private final String sqlServer2000Query =
    "select db_name() as type_catalog, \n" +
    "       o.name as type_schema, \n" +
    "       t.name as type_name,  \n" +
    "       type_name(t.xtype) as data_type, \n" +
    "       t.allownulls as is_nullable,  \n" +
    "       t.length as max_length,  \n" +
    "       t.scale,  \n" +
    "       t.prec as [precision],  \n" +
    "       t.collation as collation_name \n" +
    "from systypes t with (nolock) \n" +
    "  join sysusers o on t.uid = o.uid \n" +
    "where xusertype > 256";

  // the data types for which the max_length information are valid
  private Set<String> maxLengthTypes = CollectionUtil.treeSet("varchar", "nvarchar", "char", "text", "ntext", "varbinary");

  // the data types for which the scale and precision columns are valid
  private Set<String> numericTypes = CollectionUtil.treeSet("decimal", "numeric");

  public static boolean versionSupportsTypes(WbConnection con)
  {
    return SqlServerUtil.isSqlServer2000(con);
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

    List<DomainIdentifier> types = getTypeList(con, schema, objects);
    if (types.isEmpty()) return false;

    for (DomainIdentifier type : types)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, type.getCatalog());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
      result.getRow(row).setUserObject(type);
    }
    return true;
  }

  @Override
  public List<String> supportedTypes()
  {
    return CollectionUtil.arrayList("TYPE");
  }

  @Override
  public boolean handlesType(String type)
  {
    return "TYPE".equalsIgnoreCase(type);
  }

  @Override
  public boolean handlesType(String[] types)
  {
    if (types == null) return true;
    for (String typ : types)
    {
      if (handlesType(typ)) return true;
    }
    return false;
  }


  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  public List<DomainIdentifier> getTypeList(WbConnection connection, String owner, String typeName)
  {
    Statement stmt = null;
    ResultSet rs = null;
    List<DomainIdentifier> result = new ArrayList<>();
    try
    {
      stmt = connection.createStatementForQuery();
      String sql = getSql(connection, owner, typeName);
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        DomainIdentifier domain = createTypeFromResultSet(rs);
        result.add(domain);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("SqlServerTypeReader.getTypeList()", "Could not read domains", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  private DomainIdentifier createTypeFromResultSet(ResultSet rs)
    throws SQLException
  {
    String cat = rs.getString("type_catalog");
    String schema = rs.getString("type_schema");
    String name = rs.getString("type_name");
    DomainIdentifier domain = new DomainIdentifier(cat, schema, name);
    domain.setObjectType("TYPE");

    String typeName = rs.getString("data_type");
    int size = rs.getInt("max_length");
    int scale = rs.getInt("scale");
    int precision = rs.getInt("precision");
    boolean nullable = rs.getBoolean("is_nullable");

    domain.setDataType(getDataTypeDefinition(typeName, size, scale, precision));
    domain.setNullable(nullable);
    return domain;
  }

  private String getDataTypeDefinition(String typeName, int size, int scale, int precision)
  {
    StringBuilder displayType = new StringBuilder(typeName.length() + 5);
    displayType.append(typeName);
    if (maxLengthTypes.contains(typeName))
    {
      if (size == -1)
      {
        displayType.append("(max)");
      }
      else
      {
        displayType.append("(").append(size).append(")");
      }
    }
    else if (numericTypes.contains(typeName))
    {
      displayType.append("(").append(precision).append(",").append(scale).append(")");
    }
    return displayType.toString();
  }

  @Override
  public DataStore getObjectDetails(WbConnection con, DbObject object)
  {
    if (object == null) return null;
    if (!handlesType(object.getObjectType())) return null;

    DomainIdentifier type = getObjectDefinition(con, object);
    if (type == null) return null;

    String[] columns = new String[] { "TYPE", "DATA_TYPE", "NULLABLE" };
    int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN };
    int[] sizes = new int[] { 20, 10, 5};
    DataStore result = new DataStore(columns, types, sizes);
    result.addRow();
    result.setValue(0, 0, type.getObjectName());
    result.setValue(0, 1, type.getDataType());
    result.setValue(0, 2, type.isNullable());

    return result;
  }

  private String getSql(WbConnection con, String owner, String typeName)
  {
    StringBuilder sql = new StringBuilder(baseSql.length() + 20);
    if (SqlServerUtil.isSqlServer2000(con))
    {
      sql.append(sqlServer2000Query);
      if (StringUtil.isNonBlank(typeName))
      {
        SqlUtil.appendAndCondition(sql, "t.name", typeName, con);
      }
    }
    else
    {
      sql.append(baseSql);
      if (StringUtil.isNonBlank(typeName))
      {
        SqlUtil.appendAndCondition(sql, "t.name", typeName, con);
      }
      if (StringUtil.isNonBlank(owner))
      {
        SqlUtil.appendAndCondition(sql, "s.name", owner, con);
      }
    }

    sql.append("\n ORDER BY 1, 2");
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("SqlServerTypeReader.getSql()", "Using SQL=\n" + sql);
    }
    return sql.toString();
  }

  @Override
  public DomainIdentifier getObjectDefinition(WbConnection con, DbObject name)
  {
    Statement stmt = null;
    ResultSet rs = null;
    DomainIdentifier result = null;
    try
    {
      stmt = con.createStatementForQuery();
      String typename = con.getMetadata().adjustObjectnameCase(name.getObjectName());
      String schema = con.getMetadata().adjustSchemaNameCase(name.getSchema());

      String sql = getSql(con, schema, typename);

      rs = stmt.executeQuery(sql);
      if (rs.next())
      {
        result = createTypeFromResultSet(rs);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("SqlServerTypeReader.getTypeList()", "Could not read domains", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  private String getTableTypeSource(WbConnection con, DomainIdentifier type)
  {
    String sql =
      "select col.name,  \n" +
      "       type_name(col.system_type_id) as data_type, \n" +
      "       col.is_nullable, \n" +
      "       col.max_length, \n" +
      "       col.precision,  \n" +
      "       col.scale, \n" +
      "       def.definition as default_value \n" +
      "from sys.all_columns col \n" +
      "  join sys.table_types tt on tt.type_table_object_id = col.object_id \n" +
      "  join sys.schemas s on tt.schema_id = s.schema_id \n" +
      "  left join sys.default_constraints def on def.object_id = col.default_object_id and def.parent_column_id = col.column_id \n";

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    sql += "where tt.name = ? \n";

    if (StringUtil.isNonBlank(type.getSchema()))
    {
      sql += " AND s.name = ? \n";
    }

    sql += "order by col.column_id";
    List<ColumnIdentifier> columns = new ArrayList<>();

    try
    {
      pstmt = con.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, con.getMetadata().removeQuotes(type.getObjectName()));

      if (StringUtil.isNonBlank(type.getSchema()))
      {
        pstmt.setString(2, con.getMetadata().removeQuotes(type.getSchema()));
      }

      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String colname = rs.getString("name");
        String typeName = rs.getString("data_type");
        int size = rs.getInt("max_length");
        int scale = rs.getInt("scale");
        int precision = rs.getInt("precision");
        boolean nullable = rs.getBoolean("is_nullable");
        String defaultValue = rs.getString("default_value");
        ColumnIdentifier col = new ColumnIdentifier(colname);
        col.setDbmsType(getDataTypeDefinition(typeName, size, scale, precision));
        col.setIsNullable(nullable);
        col.setDefaultValue(defaultValue);
        columns.add(col);
      }

      StringBuilder source = new StringBuilder(columns.size() * 20 + 50);
      source.append("CREATE TYPE ");
      source.append(type.getObjectExpression(con));
      source.append("\nAS\nTABLE\n(\n");
      TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
      builder.appendColumnDefinitions(source, columns, con.getMetadata());
      source.append("\n);\n");
      return source.toString();
    }
    catch (SQLException ex)
    {
      LogMgr.logError("SqlServerTypeReader.getTableTypeSource()", "Could not read columns for type " + type.getObjectName(), ex);
      return "";
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    DomainIdentifier type = getObjectDefinition(con, object);
    if (type.getDataType().equalsIgnoreCase("table type"))
    {
      return getTableTypeSource(con, type);
    }
    StringBuilder result = new StringBuilder(50);
    result.append("CREATE TYPE ");
    result.append(type.getObjectExpression(con));
    result.append("\n  FROM ");
    result.append(type.getDataType());
    if (!type.isNullable())
    {
      result.append(" NOT NULL");
    }
    result.append(";\n");
    return result.toString();
  }

  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject object)
  {
    return null;
  }

  @Override
  public boolean hasColumns()
  {
    return false;
  }
}
