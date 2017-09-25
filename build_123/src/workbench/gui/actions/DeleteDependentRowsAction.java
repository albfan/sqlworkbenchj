/*
 * DeleteDependentRowsAction.java
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

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 *
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author Thomas Kellerer
 */
public class DeleteDependentRowsAction
  extends WbAction
{
  private DbData client;

  public DeleteDependentRowsAction(DbData aClient)
  {
    super();
    this.client = aClient;
    this.setEnabled(false);
    this.initMenuDefinition("MnuTxtDelDependentRows");
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    this.client.deleteRowWithDependencies();
  }

  public void setClient(DbData db)
  {
    this.client = db;
  }
}
