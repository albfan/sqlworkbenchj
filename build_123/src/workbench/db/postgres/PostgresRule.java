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
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRule
  implements DbObject
{
  private String catalog;
  private String schema;
  private String ruleName;
  private final String objectType = "RULE";
  private String remarks;
  private String source;
  private TableIdentifier ruleTable;
  private String event;

  public PostgresRule(String ruleCatalog, String ruleSchema, String name)
  {
    catalog = ruleCatalog;
    schema = ruleSchema;
    ruleName = name;
  }

  public void setEvent(String evt)
  {
    event = evt;
  }

  public String getEvent()
  {
    return event;
  }

  /**
   * Set the table for which this rule is defined
   * @param tbl
   */
  public void setTable(TableIdentifier tbl)
  {
    ruleTable = tbl;
  }

  public TableIdentifier getTable()
  {
    return ruleTable;
  }

  @Override
  public String getCatalog()
  {
    return catalog;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  @Override
  public String getObjectType()
  {
    return objectType;
  }

  @Override
  public String getObjectName()
  {
    return ruleName;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return ruleName;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return SqlUtil.fullyQualifiedName(conn, this);
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return ruleName;
  }

  @Override
  public String toString()
  {
    return getObjectName();
  }

  public void setSource(String sql)
  {
    this.source = sql;
  }

  public String getSource()
  {
    return source;
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    if (this.source == null)
    {
      return con.getMetadata().getObjectSource(this);
    }
    return source;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    StringBuilder sql = new StringBuilder(50);
    sql.append("DROP RULE ");
    sql.append(con.getMetadata().quoteObjectname(ruleName));
    sql.append(" ON ");
    sql.append(ruleTable.getObjectName(con));
    if (cascade)
    {
      sql.append(" CASCADE");
    }
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
