/*
 * H2DomainReader.java
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
package workbench.db.h2database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read information about defined DOMAINs in H2.
 *
 * @author Thomas Kellerer
 */
public class H2DomainReader
  implements ObjectListExtender
{
  final String baseSql = "SELECT domain_catalog,  \n" +
                         "       domain_schema, \n" +
                         "       domain_name, \n" +
                         "       type_name, \n" +
                         "       data_type, \n" +
                         "       precision, \n" +
                         "       scale, \n" +
                         "       is_nullable as nullable, \n" +
                         "       column_default as default_value, \n" +
                         "       check_constraint as constraint_definition, \n" +
                         "       remarks \n" +
                         " FROM information_schema.domains ";

  private String getSql(WbConnection connection, String schema, String name)
  {
    StringBuilder sql = new StringBuilder(baseSql.length() + 40);

    sql.append(baseSql);

    boolean whereAdded = false;
    if (StringUtil.isNonBlank(name))
    {
      sql.append(" WHERE domain_name like '");
      sql.append(connection.getMetadata().quoteObjectname(name));
      sql.append("%' ");
      whereAdded = true;
    }

    if (StringUtil.isNonBlank(schema))
    {
      sql.append(whereAdded ? " AND " : " WHERE ");

      sql.append(" domain_schema = '");
      sql.append(connection.getMetadata().quoteObjectname(schema));
      sql.append("'");
    }
    sql.append(" ORDER BY 1, 2 ");

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("H2DomainReader.getSql()", "Using SQL=\n" + sql);
    }

    return sql.toString();
  }

  public List<DomainIdentifier> getDomainList(WbConnection connection, String schemaPattern, String namePattern)
  {
    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;
    List<DomainIdentifier> result = new ArrayList<>();
    try
    {
      sp = connection.setSavepoint();
      stmt = connection.createStatementForQuery();
      String sql = getSql(connection, schemaPattern, namePattern);
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        String cat = rs.getString("domain_catalog");
        String schema = rs.getString("domain_schema");
        String name = rs.getString("domain_name");
        DomainIdentifier domain = new DomainIdentifier(cat, schema, name);
        domain.setCheckConstraint(rs.getString("constraint_definition"));
        String typeName = rs.getString("type_name");
        int type = rs.getInt("data_type");
        int precision = rs.getInt("precision");
        int scale = rs.getInt("scale");
        String dataType = SqlUtil.getSqlTypeDisplay(typeName, type, scale, precision);
        domain.setDataType(dataType);
        domain.setNullable(rs.getBoolean("nullable"));
        domain.setDefaultValue(rs.getString("default_value"));
        domain.setComment(rs.getString("remarks"));
        result.add(domain);
      }
      connection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      connection.rollback(sp);
      LogMgr.logError("H2DomainReader.getDomainList()", "Could not read domains", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public DomainIdentifier getObjectDefinition(WbConnection connection, DbObject object)
  {
    List<DomainIdentifier> domains = getDomainList(connection, object.getSchema(), object.getObjectName());
    if (CollectionUtil.isEmpty(domains)) return null;
    return domains.get(0);
  }

  public String getDomainSource(DomainIdentifier domain)
  {
    if (domain == null) return null;

    StringBuilder result = new StringBuilder(50);
    result.append("CREATE DOMAIN ");
    result.append(domain.getObjectName());
    result.append(" AS ");
    result.append(domain.getDataType());
    if (domain.getDefaultValue() != null)
    {
      result.append("\n   DEFAULT ");
      result.append(domain.getDefaultValue());
    }
    if (StringUtil.isNonBlank(domain.getCheckConstraint()) || !domain.isNullable())
    {
      result.append("\n   CHECK ");
      if (StringUtil.isNonBlank(domain.getConstraintName()))
      {
        result.append(domain.getConstraintName() + " ");
      }
      if (!domain.isNullable()) result.append("NOT NULL ");
      if (StringUtil.isNonBlank(domain.getCheckConstraint()))
      {
        result.append(domain.getCheckConstraint());
      }
    }
    result.append(";\n");
    if (StringUtil.isNonBlank(domain.getComment()))
    {
      result.append("\nCOMMENT ON DOMAIN " + domain.getObjectName() + " IS '");
      result.append(SqlUtil.escapeQuotes(domain.getComment()));
      result.append("';\n");
    }
    return result.toString();
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded("DOMAIN", requestedTypes)) return false;

    List<DomainIdentifier> domains = getDomainList(con, schema, objects);
    if (domains.isEmpty()) return false;

    for (DomainIdentifier domain : domains)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, domain.getSchema());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, domain.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, domain.getComment());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, domain.getObjectType());
      result.getRow(row).setUserObject(domain);
    }
    return true;
  }

  @Override
  public boolean handlesType(String type)
  {
    return StringUtil.equalStringIgnoreCase("DOMAIN", type);
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

    DomainIdentifier domain = getObjectDefinition(con, object);
    if (domain == null) return null;

    String[] columns = new String[] { "DOMAIN", "DATA_TYPE", "NULLABLE", "CONSTRAINT", "REMARKS" };
    int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR, Types.VARCHAR };
    int[] sizes = new int[] { 20, 10, 5, 30, 30 };
    DataStore result = new DataStore(columns, types, sizes);
    result.addRow();
    result.setValue(0, 0, domain.getObjectName());
    result.setValue(0, 1, domain.getDataType());
    result.setValue(0, 2, domain.isNullable());
    result.setValue(0, 3, domain.getCheckConstraint());
    result.setValue(0, 4, domain.getComment());

    return result;
  }

  @Override
  public List<String> supportedTypes()
  {
    return Collections.singletonList("DOMAIN");
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    return getDomainSource(getObjectDefinition(con, object));
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
