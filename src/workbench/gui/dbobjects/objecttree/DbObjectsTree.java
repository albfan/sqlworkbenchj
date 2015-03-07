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

import java.sql.SQLException;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import workbench.interfaces.ExpandableTree;
import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectsTree
  extends JTree
  implements TreeExpansionListener, ExpandableTree
{
  private TreeLoader loader;

  public DbObjectsTree()
  {
    super();
    setShowsRootHandles(true);
    addTreeExpansionListener(this);
		setCellRenderer(new DbObjectNodeRenderer());
		setAutoscrolls(true);

		setEditable(false);
		setExpandsSelectedPaths(true);

		// setting the row height to 0 makes it dynamic
		// so it will adjust properly to the font of the renderer
		setRowHeight(0);
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void setConnection(WbConnection conn)
  {
    clear();
    if (conn != null)
    {
      loader = new TreeLoader(conn.getProfile().getName());
      loader.setConnection(conn);
    }
  }

  public void clear()
  {
    if (loader != null)
    {
      loader.clear();
      setModel(loader.getModel());
    }
  }

  public void load()
  {
    if (loader == null) return;
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;

    clear();

    try
    {
      loader.load();
      setModel(loader.getModel());
    }
    catch (SQLException ex)
    {
      LogMgr.logError("DbObjectsTree.<init>", "Could not load tree", ex);
    }
  }

  @Override
  public void treeExpanded(TreeExpansionEvent event)
  {
    if (loader == null) return;
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;

    TreePath path = event.getPath();
    Object obj = path.getLastPathComponent();
    if (obj instanceof ObjectTreeNode)
    {
      final ObjectTreeNode node = (ObjectTreeNode)obj;

      if (!node.isLoaded())
      {
        WbThread load = new WbThread(new Runnable()
        {

          @Override
          public void run()
          {
            doLoad(node);
          }
        }, "DbTree Load Thread");
        load.start();
      }
    }
  }

  private void doLoad(final ObjectTreeNode node)
  {
    if (loader == null) return;
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;

    try
    {
      WbSwingUtilities.showWaitCursor(this);
      loader.loadChildren(node);
    }
    catch (SQLException ex)
    {
      LogMgr.logError("DbObjectsTree.doLoad()", "Could not load node: " + node, ex);
    }
    finally
    {
      WbSwingUtilities.showDefaultCursor(this);
    }
  }

  @Override
  public void treeCollapsed(TreeExpansionEvent event)
  {

  }

  public void dispose()
  {
    clear();
    loader = null;
  }

  @Override
	public void expandAll()
  {

  }

  @Override
	public void collapseAll()
  {

  }


}
