/*
 * ManageMacroAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;
import workbench.gui.macros.MacroManagerDialog;
import workbench.gui.sql.SqlPanel;

/**
 * @author Thomas Kellerer
 */
public class ManageMacroAction
  extends WbAction
{
  private MainWindow client;

  public ManageMacroAction(MainWindow aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtManageMacros", KeyStroke.getKeyStroke(KeyEvent.VK_M, PlatformShortcuts.getDefaultModifier()));
    this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
    this.setIcon(null);
  }

  @Override
  public void actionPerformed(final ActionEvent e)
  {
    showDialog();
  }

  private void showDialog()
  {
    try
    {
      SqlPanel sql = this.client.getCurrentSqlPanel();
      MacroManagerDialog d = new MacroManagerDialog(client, sql, client.getMacroClientId());
      d.setVisible(true);
    }
    catch (Throwable th)
    {
      LogMgr.logError("ManageMacroAction.executeAction()", "Could not open MacroManagerDialog", th);
    }
  }

}
