/*
 * PostgresForeignServerReader.java
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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.TriggerDefinition;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read information about extensions from Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresEventTriggerReader
  implements ObjectListExtender
{
  public static final String TYPE = "EVENT TRIGGER";

  @Override
  public TriggerDefinition getObjectDefinition(WbConnection connection, DbObject object)
  {
    return null;
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objectNamePattern, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded(TYPE, requestedTypes)) return false;

    PostgresTriggerReader reader = new PostgresTriggerReader(con);
    int count = reader.retrieveEventTriggers(result);
    return count > 0;
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public boolean handlesType(String type)
  {
    return StringUtil.equalStringIgnoreCase(TYPE, type);
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
    return null;
  }

  @Override
  public List<String> supportedTypes()
  {
    return CollectionUtil.arrayList(TYPE);
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    PostgresTriggerReader reader = new PostgresTriggerReader(con);
    try
    {
      String source = reader.getEventTriggerSource(object.getObjectName());
      return source;
    }
    catch (SQLException ex)
    {
      LogMgr.logError("PostgresEventTriggerReader.getObjectSource()", "Could not retrieve event trigger source", ex);
    }
    return null;

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
