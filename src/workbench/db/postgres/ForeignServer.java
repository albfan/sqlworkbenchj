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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import workbench.db.DbObject;
import workbench.db.WbConnection;


/**
 *
 * @author Thomas Kellerer
 */
public class ForeignServer
  implements DbObject
{
  public static final String TYPE_NAME = "FOREIGN SERVER";
  private String serverName;
  private String remarks;
  private Map<String, String> options = Collections.emptyMap();
  private String version;
  private String fdwName;
  private String type;

  public ForeignServer(String name)
  {
    serverName = name;
  }

  public Map<String, String> getOptions()
  {
    return Collections.unmodifiableMap(options);
  }

  public void setOptions(Map<String, String> options)
  {
    this.options = options == null ? Collections.emptyMap() : new HashMap<>(options);
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getFdwName()
  {
    return fdwName;
  }

  public void setFdwName(String fdwName)
  {
    this.fdwName = fdwName;
  }

  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }


  @Override
  public String getCatalog()
  {
    return null;
  }

  @Override
  public String getSchema()
  {
    return null;
  }

  @Override
  public String getObjectType()
  {
    return TYPE_NAME;
  }

  @Override
  public String getObjectName()
  {
    return serverName;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return serverName;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return serverName;
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return serverName;
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
    String sql =
      "CREATE FOREIGN SERVER " + serverName + "\n"+
      "  FOREIGN DATA WRAPPER " + fdwName + "\n" +
      "  OPTIONS (";

    boolean first = true;
    for (Map.Entry<String, String> entry : options.entrySet())
    {
      if (first)
      {
        first = false;
      }
      else
      {
        sql += ", ";
      }
      sql += entry.getKey() + " '" + entry.getValue() + "'";
    }
    sql += ");";
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
    sql.append("DROP FOREIGN SERVER ");
    sql.append(con.getMetadata().quoteObjectname(serverName));
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
