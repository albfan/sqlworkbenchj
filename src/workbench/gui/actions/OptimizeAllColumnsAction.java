/*
 * OptimizeAllColumnsAction.java
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

import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;

import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.WbTable;

import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class OptimizeAllColumnsAction
  extends WbAction
{
  protected ColumnWidthOptimizer optimizer;

  public OptimizeAllColumnsAction(WbTable client)
  {
    super();
    this.setClient(client);
    this.initMenuDefinition("MnuTxtOptimizeAllCol", KeyStroke.getKeyStroke(KeyEvent.VK_W, PlatformShortcuts.getDefaultModifier()));
    this.setEnabled(false);
  }

  public void disableShortcut()
  {
    this.setAccelerator(null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (optimizer == null) return;
    final boolean shiftPressed = isShiftPressed(e);
    Thread t = new WbThread("OptimizeAllCols Thread")
    {
      @Override
      public void run()
      {
        optimizer.optimizeAllColWidth(shiftPressed || GuiSettings.getIncludeHeaderInOptimalWidth());
      }
    };
    t.start();
  }

  @Override
  public boolean hasShiftModifier()
  {
    return true;
  }

  public void setClient(WbTable client)
  {
    this.optimizer = (client != null ? new ColumnWidthOptimizer(client) : null);
    this.setEnabled(client != null);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }

}
