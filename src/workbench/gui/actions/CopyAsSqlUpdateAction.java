/*
 * CopyAsSqlUpdateAction.java
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

import workbench.db.exporter.ExportType;

import workbench.gui.components.ClipBoardCopier;
import workbench.gui.components.WbTable;

/**
 * Action to copy the contents of the data as SQL update statements into the clipboard.
 *
 * @see workbench.gui.components.ClipBoardCopier
 * @author Thomas Kellerer
 */
public class CopyAsSqlUpdateAction
  extends WbAction
{
  private WbTable client;

  public CopyAsSqlUpdateAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtCopyAsSqlUpdate", null);
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    this.setEnabled(false);
  }

  @Override
  public boolean hasCtrlModifier()
  {
    return true;
  }

  @Override
  public boolean hasShiftModifier()
  {
    return false;
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    ClipBoardCopier copier = new ClipBoardCopier(this.client);
    boolean selectColumns = false;
    if (invokedByMouse(e))
    {
      selectColumns = isCtrlPressed(e);
    }
    copier.copyAsSql(ExportType.SQL_UPDATE, false, selectColumns);
  }

}
