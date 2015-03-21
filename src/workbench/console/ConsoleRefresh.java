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
package workbench.console;

import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.gui.sql.AutomaticRefreshMgr;

import workbench.sql.BatchRunner;
import workbench.sql.RefreshAnnotation;
import workbench.sql.StatementHistory;
import workbench.sql.WbAnnotation;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ConsoleRefresh
{
  private boolean doRefresh;
  private WbThread refreshThread;

  public ConsoleRefresh()
  {
  }

  public boolean handleRefresh(BatchRunner runner, String sql, StatementHistory history)
  {
    if (sql == null) return false;
    if (refreshThread != null) return false;

    String verb = SqlUtil.getSqlVerb(sql);
    String interval = null;
    boolean manualRefresh = false;
    if (RefreshAnnotation.ANNOTATION.equalsIgnoreCase(verb))
    {
      interval = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false));
      if (history.size() == 0) return false;
      sql = history.get(history.size() - 1);
      manualRefresh = true;
    }
    else
    {
      Set<String> tags = CollectionUtil.caseInsensitiveSet(WbAnnotation.getTag(RefreshAnnotation.ANNOTATION));
      List<WbAnnotation> annotations = WbAnnotation.readAllAnnotations(sql, tags);
      if (annotations.size() != 1) return false;
      interval = annotations.get(0).getValue();
    }

    int milliSeconds = AutomaticRefreshMgr.parseInterval(interval);
    if (milliSeconds <= 5)
    {
      // if this was a manual "WbRefresh", don't handle the actual statement
      // otherwise proceed normally
      return manualRefresh;
    }

    startRefresh(runner, sql, milliSeconds);
    return true;
  }

  private void startRefresh(final BatchRunner runner, final String sql, final int interval)
  {
    doRefresh = true;
    refreshThread = new WbThread(new Runnable()
    {

      @Override
      public void run()
      {
        doRefresh(runner, sql, interval);
      }
    }, "Console Refresh");

    refreshThread.start();

    WbConsoleReader console = ConsoleReaderFactory.getConsoleReader();

    char c = 0;
    while ( c != 'q')
    {
      c = console.readCharacter();
      if (Character.toLowerCase(c) == 'q' || Character.toLowerCase(c) == 'x')
      {
        doRefresh = false;
        if (refreshThread != null)
        {
          refreshThread.interrupt();
        }
      }
    }

  }

  private void doRefresh(BatchRunner runner, String sql, int interval)
  {
    while (doRefresh)
    {
      try
      {
        runner.runScript(sql);
        System.out.println("**** Refreshing statement. Press q to quit");
        Thread.sleep(interval);
      }
      catch (InterruptedException ir)
      {
        // this is expected
        break;
      }
      catch (Exception ex)
      {
        LogMgr.logError("ConsoleRefresh", "Error running statement", ex);
        break;
      }
    }
    refreshThread = null;
    doRefresh = false;
  }
}
