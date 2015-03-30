/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects.objecttree;

import java.awt.event.ActionEvent;
import java.util.List;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ReloadNodeAction
  extends WbAction
{
  private DbTreePanel panel;

  public ReloadNodeAction(DbTreePanel tree)
  {
		super();
		initMenuDefinition("TxtReload");
    panel = tree;
    List<ObjectTreeNode> nodes = tree.getSelectedNodes();
    int containerNodes = 0;
    for (ObjectTreeNode node : nodes)
    {
      if (node.canHaveChildren())
      {
        containerNodes ++;
      }
    }
    setEnabled(containerNodes == nodes.size());
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (!WbSwingUtilities.isConnectionIdle(panel, panel.getConnection())) return;

    WbThread load = new WbThread("Node Reload Thread")
    {
      @Override
      public void run()
      {
        panel.reloadSelectedNodes();
      }
    };
    load.start();
  }

}
