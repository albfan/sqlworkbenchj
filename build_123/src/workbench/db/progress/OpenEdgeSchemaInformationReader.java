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
package workbench.db.progress;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import workbench.db.SchemaInformationReader;
import workbench.db.WbConnection;
import workbench.sql.commands.SetCommand;

/**
 * A SchemaInformationReader that gets the default schema from the JDBC URL.
 *
 * OpenEdge does not seem to support DatabaseMetaData.getSchema() or any SQL query
 * to retrieve the default schema of the session (although it does support a SET SCHEMA statement).
 *
 * This class is a workaround in order to be able to properly use the current schema
 * in various places.
 *
 * The class registers as a listener on the connection state. When SET SCHEMA is called,
 * the current schema is updated with the value supplied to the SET command.
 *
 * @author Thomas Kellerer
 *
 * @see SetCommand#handleSchemaChange(java.lang.String, java.lang.String)
 */
public class OpenEdgeSchemaInformationReader
  implements SchemaInformationReader, PropertyChangeListener
{
  private String schema;

  public OpenEdgeSchemaInformationReader(WbConnection conn)
  {
    if (conn != null)
    {
      String url = conn.getUrl();
      schema = extractURLProperty(url);
      conn.addChangeListener(this);
    }
  }

  public String extractURLProperty(String url)
  {
    int pos = url.indexOf(';');
    if (pos < 0) return null;

    String props = url.substring(pos + 1);
    String[] args = props.split(";");
    for (String arg : args)
    {
      String[] prop = arg.split("=");
      if (prop.length == 2 && prop[0].equalsIgnoreCase("defaultSchema"))
      {
        return prop[1];
      }
    }
    return null;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (WbConnection.PROP_SCHEMA.equals(evt.getNewValue()))
    {
      schema = (String)evt.getNewValue();
    }
  }

  @Override
  public String getCurrentSchema()
  {
    return schema;
  }

  @Override
  public String getCachedSchema()
  {
    return schema;
  }

  @Override
  public void dispose()
  {

  }

  @Override
  public void clearCache()
  {
    schema = null;
  }

  @Override
  public boolean isSupported()
  {
    return true;
  }

}
