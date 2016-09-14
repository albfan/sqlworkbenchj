/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.mssql;

import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class NamedDefault
  implements DbObject
{
  private String database;
  private String schema;
  private String constraintName;
  private String definition;

  public NamedDefault(String database, String schema, String constraintName)
  {
    this.database = database;
    this.schema = schema;
    this.constraintName = constraintName;
  }

  @Override
  public String getCatalog()
  {
    return database;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  @Override
  public String getObjectType()
  {
    return "DEFAULT";
  }

  @Override
  public String getObjectName()
  {
    return constraintName;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return constraintName;
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, database, schema, constraintName);
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return SqlUtil.fullyQualifiedName(conn, this);
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    synchronized (this)
    {
      if (definition == null)
      {
        readSource(con);
      }
      return definition;
    }
  }

  private void readSource(WbConnection con)
  {
    SpHelpTextRunner runner = new SpHelpTextRunner();
    CharSequence source = runner.getSource(con, database, schema, constraintName);
    if (source != null)
    {
      definition = SqlUtil.addSemicolon(source.toString());
    }
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return getFullyQualifiedName(con);
  }

  @Override
  public String getComment()
  {
    return null;
  }

  @Override
  public void setComment(String cmt)
  {
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return "DROP DEFAULT " + getFullyQualifiedName(con);
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
