/*
 * ManageDriversAction.java
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

import workbench.WbManager;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.toolbar.ConfigureToolbarPanel;

/**
 * @author Thomas Kellerer
 */
public class ConfigureToolbarAction
  extends WbAction
{
  public ConfigureToolbarAction()
  {
    super();
    this.initMenuDefinition("MnuTxtEditToolbar");
    this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    MainWindow window = (MainWindow)WbManager.getInstance().getCurrentWindow();

    ConfigureToolbarPanel panel = new ConfigureToolbarPanel(window.getAllActions());

    String title = ResourceMgr.getString("MnuTxtEditToolbar").replace("...", "");

    ValidatingDialog dialog = new ValidatingDialog(window, title, panel, true);

    if (!Settings.getInstance().restoreWindowSize(dialog, "workbench.gui.edit.toolbar"))
    {
      dialog.setSize(800, 600);
    }
    dialog.setLocationRelativeTo(window);
    dialog.setVisible(true);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }


}
