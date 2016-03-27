/*
 * CloseResultTabAction.java
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

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

import workbench.gui.sql.DetachedResultWindow;
import workbench.gui.sql.DwPanel;
import workbench.gui.sql.SqlPanel;

/**
 * An action to detach the the currently selected result tab of a SqlPanel and open it in a separate window.
 *
 * @author Thomas Kellerer
 */
public class DetachResultTabAction
  extends WbAction
{
  private SqlPanel panel;

  public DetachResultTabAction(SqlPanel sqlPanel)
  {
    super();
    panel = sqlPanel;
    this.initMenuDefinition("MnuTxtDetachResult");
    this.setIcon(null);
    this.setEnabled(panel.getCurrentResult() != null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    final DwPanel result = panel.getCurrentResult();
    if (result == null) return;

    if (result.getTable() == null) return;
    if (result.getDataStore() == null) return;

    final int timer = panel.getRefreshMgr().getRefreshPeriod(result);
    panel.removeCurrentResult();
    final Window parent = SwingUtilities.getWindowAncestor(panel);

    EventQueue.invokeLater(()
      ->
      {
        DetachedResultWindow window = new DetachedResultWindow(result, parent, panel);
        if (timer > 0)
        {
          window.refreshAutomatically(timer);
        }
        window.showWindow();
    });

  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
