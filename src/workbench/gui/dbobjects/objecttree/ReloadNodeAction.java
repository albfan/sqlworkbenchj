/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import workbench.db.TableIdentifier;

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
      if (node.getDbObject() == null || node.getDbObject() instanceof TableIdentifier)
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

    WbThread load = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        panel.reloadSelectedNodes();
      }
    }, "Node Reload Thread");
    load.start();
  }

}
