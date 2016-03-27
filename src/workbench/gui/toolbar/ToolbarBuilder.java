/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.toolbar;

import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class ToolbarBuilder
{
  private final String separator = "wb-sep";
  private final String defaultActions =
    "wb-ExecuteSelAction,wb-ExecuteCurrentAction," +
    separator +
    ",wb-StopAction," +
    separator +
    ",wb-FirstStatementAction,wb-PrevStatementAction,wb-NextStatementAction,wb-LastStatementAction," +
    separator +
    ",wb-UpdateDatabaseAction,wb-InsertRowAction,wb-CopyRowAction,wb-DeleteRowAction,"+
    ",wb-SelectionFilterAction,wb-FilterDataAction,wb-FilterPickerAction,wb-ResetFilterAction,"+
    separator +
    ",wb-CommitAction,wb-RollbackAction," +
    separator +
    ",wb-IgnoreErrorsAction,wb-AppendResultsAction," +
    separator +
    ",wb-ShowDbExplorerAction,wb-ShowDbTreeAction";
  private MainToolbar toolbar;
  private final List<WbAction> mainActions;

  public ToolbarBuilder(List<WbAction> globalActions)
  {
    mainActions = new ArrayList<>(globalActions);
  }

  public MainToolbar createToolbar(List<WbAction> panelActions)
  {
    WbSwingUtilities.invoke(() -> _createToolbar(panelActions));
    return toolbar;
  }

  private void _createToolbar(final List<WbAction> panelActions)
  {
    toolbar = new MainToolbar();
    List<String> commands = Settings.getInstance().getListProperty("workbench.gui.toolbar.actions", false, defaultActions);

    LogMgr.logDebug("ToolbarBuilder.createToolbar()", "Using toolbar list: " + commands);
    for (String cmd : commands)
    {
      if (cmd.equals(separator))
      {
        toolbar.addSeparator();
      }
      else
      {
        WbAction action = getPanelAction(panelActions, cmd);
        if (action != null)
        {
          action.addToToolbar(toolbar);
        }
        else
        {
          action = getGlobalAction(cmd);
          if (action != null)
          {
            toolbar.add(action.getToolbarButton(true));
          }
          else
          {
            LogMgr.logWarning("ToolbarBuilder.createToolbar()", "Action: " + cmd + " not found!");
          }
        }
      }
    }
    toolbar.addSeparator();
    toolbar.addConnectionInfo();
  }

  private WbAction getPanelAction(List<WbAction> actions, String actionCommand)
  {
    for (WbAction action : actions)
    {
      if (action.getActionCommand().equals(actionCommand)) return action;
    }
    return null;
  }

  private WbAction getGlobalAction(String actionCommand)
  {
    for (WbAction action : mainActions)
    {
      if (action.getActionCommand().equals(actionCommand)) return action;
    }
    return null;
  }

}
