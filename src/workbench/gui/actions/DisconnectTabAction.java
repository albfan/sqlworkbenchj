/*
 * DisconnectTabAction.java
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

import workbench.resource.ResourceMgr;

import workbench.db.ConnectionProfile;

import workbench.gui.MainWindow;

/**
 *
 * @author Thomas Kellerer
 */
public class DisconnectTabAction
  extends WbAction
{
  private MainWindow window;

  public DisconnectTabAction(MainWindow client)
  {
    super();
    this.initMenuDefinition("MnuTxtDisconnectTab");
    this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
    this.setEnabled(false);
    this.window = client;
    checkState();
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (this.window == null) return;
    ConnectionProfile prof = window.getCurrentProfile();
    if (prof.getUseSeparateConnectionPerTab()) return;
    this.window.disconnectCurrentPanel();
  }

  public void checkState()
  {
    if (this.window == null)
    {
      this.setEnabled(false);
    }
    else
    {
      this.setEnabled(window.canUseSeparateConnection() && window.usesSeparateConnection());
    }
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
