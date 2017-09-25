/*
 * PostgresRule.java
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
package workbench.db.postgres;

import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class PgExtension
  implements DbObject
{
  public static final String TYPE_NAME = "EXTENSION";
  private String name;
  private String remarks;
  private String version;
  private String schema;

  public PgExtension(String schema, String name)
  {
    this.name = name;
    this.schema = schema;
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  @Override
  public String getCatalog()
  {
    return null;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  @Override
  public String getObjectType()
  {
    return TYPE_NAME;
  }

  @Override
  public String getObjectName()
  {
    return name;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return name;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return name;
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return name;
  }

  @Override
  public String toString()
  {
    return getObjectName();
  }

  public void setSource(String sql)
  {
  }

  public String getSource()
  {
    String sql = "CREATE EXTENSION IF NOT EXISTS " + SqlUtil.quoteObjectname(name);

    if (!"pg_catalog".equals(schema))
    {
      sql += "\n  WITH SCHEMA " + SqlUtil.quoteObjectname(schema);
    }

    if (!"1.0".equals(version))
    {
      sql += "\n  VERSION " + version;
    }

    sql += ";";
    return sql;
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    return getSource();
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    StringBuilder sql = new StringBuilder(50);
    sql.append("DROP EXTENSION ");
    sql.append(SqlUtil.quoteObjectname(name));
    if (cascade)
    {
      sql.append(" CASCADE");
    }
    sql.append(';');
    return sql.toString();
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return getFullyQualifiedName(con);
  }

  @Override
  public String getComment()
  {
    return remarks;
  }

  @Override
  public void setComment(String cmt)
  {
    remarks = cmt;
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
