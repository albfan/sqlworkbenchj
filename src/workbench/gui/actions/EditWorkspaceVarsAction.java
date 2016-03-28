/*
 * CloseWorkspaceAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Properties;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.components.PropertiesEditor;
import workbench.gui.components.ValidatingDialog;

/**
 * Action to close the current workspace.
 *
 * @see workbench.gui.MainWindow#closeWorkspace(boolean)
 * @author Thomas Kellerer
 */
public class EditWorkspaceVarsAction
  extends WbAction
{
  private final String CONFIG_PROP = "workbench.gui.edit.workspace.variables.dialog";
  private MainWindow client;

  public EditWorkspaceVarsAction(MainWindow aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtEditWkspVars", null);
    this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
    this.setIcon(null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (client == null) return;

    Properties variables = client.getCurrentWorkspaceVariables();
    if (variables == null) return;

    PropertiesEditor editor = new PropertiesEditor(variables);
    ValidatingDialog dialog = ValidatingDialog.createDialog(client, editor, ResourceMgr.getPlainString("MnuTxtEditWkspVars"), null, 0, false);

    if (!Settings.getInstance().restoreWindowSize(dialog, CONFIG_PROP))
    {
      dialog.setSize(400, 300);
    }
    editor.optimizeColumnWidths();
    dialog.setVisible(true);
    
    Settings.getInstance().storeWindowSize(dialog, CONFIG_PROP);

    if (!dialog.isCancelled())
    {
      client.replaceWorkspaceVariables(editor.getProperties());
    }
  }
}
