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
package workbench.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class WorkspaceConnectionVariables
{
  public static final String VAR_DB_USER = "${db.user}";
  public static final String VAR_DB_URL = "${db.url}";
  public static final String VAR_DB_SCHEMA = "${db.schema}";
  public static final String VAR_DB_CATALOG = "${db.catalog}";

  private final Map<String, String> sysVars = new HashMap<>();

  public WorkspaceConnectionVariables(WbConnection conn)
  {
    if (conn != null)
    {
      sysVars.put(VAR_DB_URL, conn.getUrl());
      sysVars.put(VAR_DB_USER, conn.getCurrentUser());
      sysVars.put(VAR_DB_SCHEMA, conn.getCurrentSchema());
      sysVars.put(VAR_DB_CATALOG, conn.getCurrentCatalog());
    }
  }

  public Map<String, String> getVariables()
  {
    return Collections.unmodifiableMap(sysVars);
  }

  public void replaceVariableValues(WbProperties props)
  {
    if (props == null) return;

    for (String key : props.getKeys())
    {
      String value = props.getProperty(key);
      String newValue = StringUtil.replaceProperties(sysVars, value);
      props.setProperty(key, newValue);
    }
  }

}
