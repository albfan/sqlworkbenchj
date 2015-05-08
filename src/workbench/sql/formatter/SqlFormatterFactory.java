/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.File;

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
    return createFormatter(conn.getDbId());
  }

  public static SqlFormatter createFormatter(String dbId)
  {
    String prg = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".program", null);
    if (prg != null)
    {
      File prgFile = new File(prg);
      if (!prgFile.exists())
      {
        LogMgr.logWarning("SqlFormatterFactory.createFormatter", "External formatter for DBID=" + dbId + ": " + prg + " does not exist!");
      }
      else
      {
        String cmdLine  = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".cmdline", null);
        String inputEncoding  = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".inputencoding", null);
        String outputEncoding  = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".outputEncoding", null);
        boolean supportsScripts  = Settings.getInstance().getBoolProperty("workbench.formatter." + dbId + ".supports.scripts", false);

        ExternalFormatter f = new ExternalFormatter();
        f.setCommandLine(cmdLine);
        f.setProgram(prg);
        f.setInputEncoding(inputEncoding);
        f.setOutputEncoding(outputEncoding);
        f.setSupportsMultipleStatements(supportsScripts);
        return f;
      }
    }
    return new WbSqlFormatter(Settings.getInstance().getFormatterMaxSubselectLength(), dbId);
  }

}
