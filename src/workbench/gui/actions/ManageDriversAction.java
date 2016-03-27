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
import workbench.interfaces.MainPanel;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.DbDriver;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.profiles.DriverEditorDialog;

/**
 * @author Thomas Kellerer
 */
public class ManageDriversAction
  extends WbAction
{
  public ManageDriversAction()
  {
    super();
    this.initMenuDefinition("MnuTxtEditDrivers");
    this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    WbConnection currentConnection = null;
    DbDriver currentDrv = null;

    MainWindow mainWin = (MainWindow)WbManager.getInstance().getCurrentWindow();
    if (mainWin != null)
    {
      MainPanel panel = mainWin.getCurrentPanel();
      if (panel != null)
      {
        currentConnection = panel.getConnection();
      }
      if (currentConnection != null)
      {
        String drvName = currentConnection.getProfile().getDriverName();
        currentDrv = ConnectionMgr.getInstance().findDriverByName(currentConnection.getProfile().getDriverclass(), drvName);
      }
    }

    DriverEditorDialog.showDriverDialog(mainWin, currentDrv);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
