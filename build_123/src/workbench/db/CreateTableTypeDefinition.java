/*
 * CreateTableTypeDefinition.java
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
package workbench.db;

import java.util.Map;
import workbench.util.ArgumentValue;

/**
 * A wrapper class to retrieve defined "table types" from workbench.settings.
 *
 * These table types are used by the DataCopier in case the target table should be
 * created on the fly. The name of the type is used to retrieve the SQL definition
 * to create such a table.
 *
 * @see TableCreator
 * @see DbSettings#getCreateTableTemplate(String)
 *
 * @author Thomas Kellerer
 */
public class CreateTableTypeDefinition
  implements Comparable<CreateTableTypeDefinition>, ArgumentValue
{
  private String dbmsName;
  private String dbid;
  private String type;

  public CreateTableTypeDefinition(String settingsKey)
  {
    String key = settingsKey.substring("workbench.db.".length());
    int pos = key.indexOf('.');
    dbid = key.substring(0, pos);
    type = key.substring(pos+1).replace("create.table.", "");
    Map<String, String> dbmsNames = DbSettings.getDBMSNames();
    dbmsName = dbmsNames.get(dbid);
    if (dbmsName == null)
    {
      dbmsName = makeWords(dbid);
    }
  }

  @Override
  public String toString()
  {
    return type + " (" + dbmsName + ")";
  }

  public String getDbId()
  {
    return dbid;
  }

  public String getDatabase()
  {
    return dbmsName;
  }

  /**
   * The type of this definition.
   *
   * This type can be used to retrieve the configured template from DbSettings
   *
   * @see workbench.db.DbSettings#getCreateTableTemplate(java.lang.String)
   */
  public String getType()
  {
    return type;
  }

  @Override
  public int compareTo(CreateTableTypeDefinition o)
  {
    String me = dbmsName + "_" + type;
    String other = o.dbmsName + "_" + o.type;
    return me.compareToIgnoreCase(other);
  }

  private static String makeWords(String input)
  {
    StringBuilder result = new StringBuilder(input.length());
    boolean wordStart = true;
    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      if (c == '_')
      {
        wordStart = true;
        result.append(' ');
      }
      if ( (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') )
      {
        if (wordStart)
        {
          result.append(Character.toUpperCase(c));
          wordStart = false;
        }
        else
        {
          result.append(c);
        }
      }
    }
    return result.toString();
  }

  @Override
  public String getDisplay()
  {
    return toString();
  }

  @Override
  public String getValue()
  {
    return getType();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final CreateTableTypeDefinition other = (CreateTableTypeDefinition) obj;
    if ((this.dbid == null) ? (other.dbid != null) : !this.dbid.equals(other.dbid))
    {
      return false;
    }
    if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type))
    {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 59 * hash + (this.dbid != null ? this.dbid.hashCode() : 0);
    hash = 59 * hash + (this.type != null ? this.type.hashCode() : 0);
    return hash;
  }


}
