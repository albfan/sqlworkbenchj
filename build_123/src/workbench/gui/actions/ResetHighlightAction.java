/*
 * ResetHighlightAction.java
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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;

/**
 * Reset the filter defined on a WbTable
 *
 * @author Thomas Kellerer
 */
public class ResetHighlightAction
  extends WbAction
  implements TableModelListener
{
  private WbTable client;

  public ResetHighlightAction(WbTable aClient)
  {
    super();
    this.initMenuDefinition("MnuTxtResetHighlight");
    this.setClient(aClient);
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    this.setEnabled(false);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    this.client.clearHighlightExpression();
  }

  @Override
  public void tableChanged(TableModelEvent tableModelEvent)
  {
    this.setEnabled(this.client.isHighlightEnabled());
  }

  public void setClient(WbTable c)
  {
    if (this.client != null)
    {
      this.client.removeTableModelListener(this);
    }
    this.client = c;
    if (this.client != null)
    {
      this.client.addTableModelListener(this);
    }
  }

}
