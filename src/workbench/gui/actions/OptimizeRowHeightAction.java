/*
 * OptimizeRowHeightAction.java
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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.gui.components.WbTable;

import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class OptimizeRowHeightAction
  extends WbAction
  implements TableModelListener
{
  protected WbTable client;

  public OptimizeRowHeightAction()
  {
    super();
    initMenuDefinition("LblRowHeightOpt");
    this.setEnabled(false);
  }

  public OptimizeRowHeightAction(WbTable table)
  {
    this();
    setClient(table);
  }

  public void setClient(WbTable table)
  {
    this.client = table;
    checkEnabled();
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (client == null) return;
    Thread t = new WbThread("OptimizeRows Thread")
    {
      @Override
      public void run()
      {
        client.optimizeRowHeight();
      }
    };
    t.start();
  }

  private void checkEnabled()
  {
    if (this.client != null)
    {
      this.setEnabled(client.getRowCount() > 0);
    }
    else
    {
      setEnabled(false);
    }
  }

  @Override
  public void tableChanged(TableModelEvent e)
  {
    checkEnabled();
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
