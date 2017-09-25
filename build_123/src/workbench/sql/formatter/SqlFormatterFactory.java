/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.formatter;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlFormatterFactory
{

  public static SqlFormatter createFormatter(WbConnection conn)
  {
    return createFormatter(conn == null ? null : conn.getDbId());
  }

  public static SqlFormatter createFormatter(String dbId)
  {
    SqlFormatter ext = getExternalFormatter(dbId);
    if (ext == null)
    {
      ext = getExternalFormatter(null);
    }
    if (ext != null)
    {
      return ext;
    }
    return new WbSqlFormatter(Settings.getInstance().getFormatterMaxSubselectLength(), dbId);
  }

  private static SqlFormatter getExternalFormatter(String dbId)
  {
    ExternalFormatter formatter = ExternalFormatter.getDefinition(dbId);
    if (formatter != null && formatter.isEnabled())
    {
      if (formatter.isUsable())
      {
        LogMgr.logInfo("SqlFormatterFactory.createFormatter", "Using external formatter: " + formatter.toString() + " for DBID: " + dbId);
        return formatter;
      }
      else if (!formatter.programExists())
      {
        LogMgr.logInfo("SqlFormatterFactory.createFormatter", "External formatter executetable: " + formatter.getProgram() + " not found! Formatter for " + dbId + " not used.");
      }
    }
    return null;
  }
}
