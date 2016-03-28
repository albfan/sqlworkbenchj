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

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ToolbarBuilder
{
  public static final String CONFIG_PROPERTY = "workbench.gui.toolbar.actions";
  public static final String SEPARATOR_KEY = "$sep$";

  private static final String DEFAULT_ACTIONS =
    "wb-ExecuteSelAction,wb-ExecuteCurrentAction," +
    SEPARATOR_KEY +
    ",wb-StopAction," +
    SEPARATOR_KEY +
    ",wb-FirstStatementAction,wb-PrevStatementAction,wb-NextStatementAction,wb-LastStatementAction," +
    SEPARATOR_KEY +
    ",wb-UpdateDatabaseAction,wb-InsertRowAction,wb-CopyRowAction,wb-DeleteRowAction,"+
    ",wb-SelectionFilterAction,wb-FilterDataAction,wb-FilterPickerAction,wb-ResetFilterAction,"+
    SEPARATOR_KEY +
    ",wb-CommitAction,wb-RollbackAction," +
    SEPARATOR_KEY +
    ",wb-IgnoreErrorsAction,wb-AppendResultsAction," +
    SEPARATOR_KEY +
    ",wb-ShowDbExplorerAction,wb-ShowDbTreeAction";

  private MainToolbar toolbar;
  private final List<WbAction> mainActions;
  private final List<WbAction> panelActions;

  public ToolbarBuilder(final List<WbAction> panelActionList, final List<WbAction> globalActions)
  {
    mainActions = new ArrayList<>(globalActions);
    panelActions = new ArrayList<>(panelActionList);
  }

  public MainToolbar createToolbar()
  {
    // make sure this happens on the EDT!
    WbSwingUtilities.invoke(this::_createToolbar);
    return toolbar;
  }

  private void _createToolbar()
  {
    toolbar = new MainToolbar();
    List<String> commands = getConfiguredToolbarCommands();

    LogMgr.logDebug("ToolbarBuilder.createToolbar()", "Using toolbar list: " + commands);
    for (String cmd : commands)
    {
      if (cmd.equals(SEPARATOR_KEY))
      {
        toolbar.addSeparator();
      }
      else
      {
        WbAction action = getPanelAction(cmd);
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

  public static List<String> getDefaultToolbarCommands()
  {
    return StringUtil.stringToList(DEFAULT_ACTIONS, ",", true, true, false, false);
  }
  
  public static List<String> getConfiguredToolbarCommands()
  {
    return Settings.getInstance().getListProperty(CONFIG_PROPERTY, false, DEFAULT_ACTIONS);
  }

  private WbAction getPanelAction(String actionCommand)
  {
    for (WbAction action : panelActions)
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
