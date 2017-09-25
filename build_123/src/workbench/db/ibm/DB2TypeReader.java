/*
 * DB2TypeReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListEnhancer;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ObjectListExtender to read OBJECT TYPEs.
 *
 * @author Thomas Kellerer
 */
public class DB2TypeReader
  implements ObjectListExtender, ObjectListEnhancer
{
  public DB2TypeReader()
  {
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
  {
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

    List<DB2ObjectType> types = getTypes(con, schemaPattern, objectPattern);
    if (types.isEmpty()) return false;

    for (DB2ObjectType type : types)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
      result.getRow(row).setUserObject(type);
    }
    return true;
  }

  public List<DB2ObjectType> getTypes(WbConnection con, String schemaPattern, String namePattern)
  {
    List<DB2ObjectType> result = new ArrayList<>();
    String select =
      "select typeschema,  \n" +
      "       typename,   \n" +
      "       remarks,   \n" +
      "       sourcename, \n" +
      "       metatype, \n" +
      "       length,  \n" +
      "       array_length,  \n" +
      "       scale   \n" +
      "from syscat.datatypes  \n" +
      "where ownertype = 'U' ";

    if (StringUtil.isNonBlank(schemaPattern))
    {
      if (schemaPattern.indexOf('%') > -1)
      {
        select += " AND typeschema LIKE '" + schemaPattern + "' ";
      }
      else
      {
        select += " AND typeschema = '" + schemaPattern + "' ";
      }
    }

    if (StringUtil.isNonBlank(namePattern))
    {
      if (namePattern.indexOf('%') > -1)
      {
        select += " AND typename LIKE '" + namePattern + "' ";
      }
      else
      {
        select += " AND typename = '" + namePattern + "' ";
      }
    }

    select += " ORDER BY typeschema, typename ";
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("DB2TypeReader.getTypes()", "Query to retrieve TYPE: " + select);
    }

    Statement stmt = null;
    ResultSet rs = null;
    Db2DataTypeMapper mapper = new Db2DataTypeMapper();

    try
    {
      stmt = con.createStatementForQuery();
      rs = stmt.executeQuery(select);
      while (rs.next())
      {
        String schema = rs.getString("typeschema");
        String name = rs.getString("typename");
        String remarks = rs.getString("REMARKS");
        String meta = rs.getString("METATYPE");
        String baseType = rs.getString("SOURCENAME");
        int len = rs.getInt("length");
        int scale = rs.getInt("scale");
        int arrayLength = rs.getInt("array_length");
        DB2ObjectType object = new DB2ObjectType(schema, name);
        object.setComment(remarks);
        object.setMetaType(meta);

        if (object.getMetaType() != DB2ObjectType.MetaType.structured)
        {
          int jdbcType = mapper.getJDBCTypeName(baseType);
          object.setBaseType(mapper.getDisplayType(baseType, jdbcType, len, scale));
          object.setArrayLength(arrayLength);
        }
        result.add(object);
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("DB2TypeReader.getTypes()", "Error retrieving object types using:\n" + select, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
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
    for (String type : types)
    {
      if (handlesType(type)) return true;
    }
    return false;
  }

  @Override
  public DataStore getObjectDetails(WbConnection con, DbObject object)
  {
    if (object == null) return null;
    if (!handlesType(object.getObjectType())) return null;

    DB2ObjectType type = getObjectDefinition(con, object);
    if (type == null) return null;

    String[] columns = new String[] { "ATTRIBUTE", "DATA_TYPE" };
    int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
    int[] sizes = new int[] { 30, 30 };
    DataStore result = new DataStore(columns, types, sizes);
    List<ColumnIdentifier> attr = type.getAttributes();
    if (CollectionUtil.isNonEmpty(attr))
    {
      for (ColumnIdentifier col : attr)
      {
        int row = result.addRow();
        result.setValue(row, 0, col.getColumnName());
        result.setValue(row, 1, col.getDbmsType());
      }
    }
    return result;
  }

  @Override
  public DB2ObjectType getObjectDefinition(WbConnection con, DbObject name)
  {
    List<DB2ObjectType> objects = getTypes(con, name.getSchema(), name.getObjectName());
    if (CollectionUtil.isEmpty(objects)) return null;
    DB2ObjectType type = objects.get(0);
    List<ColumnIdentifier> attr = getAttributes(con, type);
    type.setAttributes(attr);
    return type;
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    DB2ObjectType type = getObjectDefinition(con, object);
    if (type == null) return null;
    StringBuilder sql = new StringBuilder(50 + type.getNumberOfAttributes() * 50);
    sql.append("CREATE TYPE ");
    sql.append(type.getObjectName());

    DB2ObjectType.MetaType metaType = type.getMetaType();

    switch (metaType)
    {
      case cursor:
        sql.append(" AS CURSOR;");
        break;

      case distinct:
        sql.append(" AS ");
        sql.append(type.getBaseType());
        if (!type.getBaseType().endsWith("LOB"))
        {
          sql.append(" WITH COMPARISONS");
        }
        sql.append(';');
        break;

      case array:
        sql.append(" AS ");
        sql.append(type.getBaseType());
        sql.append(" ARRAY[");
        sql.append(type.getArrayLength());
        sql.append("]");
        sql.append(';');
        break;

      default:
        if (metaType == DB2ObjectType.MetaType.row)
        {
          sql.append(" AS ROW \n(\n");
        }
        else
        {
          sql.append(" AS\n(\n");
        }
        List<ColumnIdentifier> columns = type.getAttributes();
        int maxLen = ColumnIdentifier.getMaxNameLength(columns);
        for (int i = 0; i < columns.size(); i++)
        {
          sql.append("  ");
          sql.append(StringUtil.padRight(columns.get(i).getColumnName(), maxLen + 2));
          sql.append(columns.get(i).getDbmsType());
          if (i < columns.size() - 1)
          {
            sql.append(",\n");
          }
        }
        sql.append("\n);\n");
    }
    return sql.toString();
  }

  @Override
  public boolean hasColumns()
  {
    return true;
  }

  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject type)
  {
    if (type instanceof DB2ObjectType)
    {
      return getAttributes(con, (DB2ObjectType)type);
    }
    return null;
  }


  public List<ColumnIdentifier> getAttributes(WbConnection con, DB2ObjectType type)
  {
    if (type == null) return null;

    String sql = null;

    if (type.getMetaType() == DB2ObjectType.MetaType.row)
    {
      sql =
        "select fieldname,  \n" +
        "       fieldtypename, \n" +
        "       length, \n" +
        "       scale  \n" +
        "from syscat.rowfields \n";
    }
    else
    {
      sql =
        "select attr_name,  \n" +
        "       attr_typename, \n" +
        "       length, \n" +
        "       scale  \n" +
        "from syscat.attributes  \n";
    }

    sql += " WHERE typename = '" + type.getObjectName() + "' \n";
    sql += " AND typeschema = '" + type.getSchema() + "' \n";
    sql += " ORDER BY ordinal";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("DB2TypeReader.getAttributes()", "Retrieving type attributes using:\n" + sql);
    }

    Statement stmt = null;
    ResultSet rs = null;
    List<ColumnIdentifier> result = new ArrayList<>(type.getNumberOfAttributes());

    try
    {
      stmt = con.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      Db2DataTypeMapper mapper = new Db2DataTypeMapper();

      while (rs.next())
      {
        String colname = rs.getString(1);
        String dataType = rs.getString(2);
        int length = rs.getInt(3);
        int scale = rs.getInt(4);

        int jdbcType = mapper.getJDBCTypeName(dataType);
        ColumnIdentifier col = new ColumnIdentifier(colname, jdbcType);

        String dbmsType = mapper.getDisplayType(dataType, jdbcType, length, scale);
        col.setDbmsType(dbmsType);

        result.add(col);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logError("DB2TypeReader.getAttributes()", "Error retrieving attributes using:\n" + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

}
