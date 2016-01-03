/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
import workbench.resource.ResourceMgr;

import workbench.gui.sql.AutomaticRefreshMgr;

import workbench.sql.BatchRunner;
import workbench.sql.ExecutionStatus;
import workbench.sql.RefreshAnnotation;
import workbench.sql.StatementHistory;
import workbench.sql.WbAnnotation;

import workbench.util.CollectionUtil;
import workbench.util.DurationFormatter;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ConsoleRefresh
{
  private boolean doRefresh;
  private WbThread refreshThread;
  private WbThread inputThread;

  public ConsoleRefresh()
  {
  }

  public HandlerState handleRefresh(BatchRunner runner, String sql, StatementHistory history)
  {
    if (sql == null) return HandlerState.notHandled;
    if (refreshThread != null) return HandlerState.notHandled;

    String verb = SqlUtil.getSqlVerb(sql);
    String interval = null;
    boolean manualRefresh = false;
    if (RefreshAnnotation.ANNOTATION.equalsIgnoreCase(verb))
    {
      if (history.size() == 0) return HandlerState.notHandled;

      interval = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql, false, false));
      if (StringUtil.isBlank(interval))
      {
        interval = ConsoleSettings.getDefaultRefreshInterval();
      }
      sql = history.get(history.size() - 1);
      manualRefresh = true;
    }
    else
    {
      Set<String> tags = CollectionUtil.caseInsensitiveSet(WbAnnotation.getTag(RefreshAnnotation.ANNOTATION));
      List<WbAnnotation> annotations = WbAnnotation.readAllAnnotations(sql, tags);
      if (annotations.size() != 1) return HandlerState.notHandled;
      interval = annotations.get(0).getValue();
    }

    int milliSeconds = AutomaticRefreshMgr.parseInterval(interval);
    if (milliSeconds <= 5)
    {
      // if this was a manual "WbRefresh", don't handle the actual statement
      // returning true signals to the caller that the statement does
      // not need to be processed further (which is the case for a "WbRefresh" command)
      // for a @WbRefresh annotation this means the interval was invalid
      // and then the caller should run the statement (manualRefresh will be false then)
      return manualRefresh ? HandlerState.handled : HandlerState.notHandled;
    }

    startRefresh(runner, sql, milliSeconds);
    return HandlerState.handled;
  }

  private void startRefresh(final BatchRunner runner, final String sql, final int interval)
  {
    doRefresh = true;

    inputThread = new WbThread("Console Refresh Input")
    {
      @Override
      public void run()
      {
        WbConsole console = WbConsoleFactory.getConsole();
        while (doRefresh)
        {
          char c = console.readCharacter();
          if (Character.toLowerCase(c) == 'q' || Character.toLowerCase(c) == 'x')
          {
            WbConsoleFactory.getConsole().reset();
            doRefresh = false;
            break;
          }
        }
        inputThread = null;
      }
    };
    inputThread.start();

    refreshThread = new WbThread("Console Refresh")
    {
      @Override
      public void run()
      {
        doRefresh(runner, sql, interval);
      }
    };

    try
    {
      refreshThread.start();
      refreshThread.join();
    }
    catch (InterruptedException ex)
    {
      // this is excpected
    }
    finally
    {
      if (inputThread != null)
      {
        inputThread.interrupt();
        inputThread = null;
      }
      refreshThread = null;
      doRefresh = false;
    }
  }

  private void doRefresh(BatchRunner runner, String sql, int interval)
  {
    DurationFormatter formatter = new DurationFormatter();
    String intDisplay = formatter.formatDuration(interval, false, false).trim();

    boolean clearScreen = ConsoleSettings.getClearScreenForRefresh();
    String quitMsg = ResourceMgr.getString("MsgRefreshQuit");

    while (doRefresh)
    {
      try
      {
        if (clearScreen)
        {
          WbConsoleFactory.getConsole().clearScreen();
        }

        ExecutionStatus status = runner.runScript(sql);

        if (status == ExecutionStatus.Error)
        {
          break;
        }

        String msg = "*** " + ResourceMgr.getFormattedString("MsgRefreshing", intDisplay, StringUtil.getCurrentTimestamp()) + " - " + quitMsg;
        System.out.println(msg);
        inputThread.join(interval);
      }
      catch (Exception ex)
      {
        LogMgr.logError("ConsoleRefresh", "Error refreshing last statement", ex);
        break;
      }
    }
    refreshThread = null;
    doRefresh = false;
  }

}
