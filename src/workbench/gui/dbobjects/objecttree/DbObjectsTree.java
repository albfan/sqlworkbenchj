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

import java.awt.EventQueue;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbStatusLabel;

import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectsTree
  extends JTree
  implements TreeExpansionListener, Serializable
{
  private TreeLoader loader;
  private ObjectTreeDragSource dragSource;
  private WbStatusLabel statusBar;
  private DbObjectNodeRenderer renderer;

  public DbObjectsTree(WbStatusLabel status)
  {
    super(new DbObjectTreeModel(new ObjectTreeNode("Database", "database")));
    setShowsRootHandles(true);
    addTreeExpansionListener(this);
    renderer = new DbObjectNodeRenderer();
		setCellRenderer(renderer);
		setAutoscrolls(true);

		setEditable(false);
		setExpandsSelectedPaths(true);
    getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// setting the row height to 0 makes it dynamic
		// so it will adjust properly to the font of the renderer
		setRowHeight(0);
    ToolTipManager.sharedInstance().registerComponent(this);
    dragSource = new ObjectTreeDragSource(this);
    loader = new TreeLoader();
    statusBar = status;
    setRowHeight(0);
  }

  @Override
  public DbObjectTreeModel getModel()
  {
    return (DbObjectTreeModel)super.getModel();
  }


  public WbConnection getConnection()
  {
    if (loader == null) return null;
    return loader.getConnection();
  }

  public void setConnection(WbConnection conn)
  {
    if (conn != null)
    {
      loader.setConnection(conn, conn.getProfile().getName());
      setModel(loader.getModel());
      if (conn.getMetadata().getSynonymReader() != null)
      {
        renderer.setSynonymTypeName(conn.getMetadata().getSynonymReader().getSynonymTypeName());
      }
      renderer.setTableTypes(conn.getMetadata().getTableTypes());
      renderer.setViewTypes(conn.getMetadata().getViewTypes());
    }
    else
    {
      clear();
    }
  }

  public void setTypesToShow(List<String> types)
  {
    loader.setSelectedTypes(types);
  }

  public void clear()
  {
    if (loader != null)
    {
      loader.clear();
      setModel(loader.getModel());
    }
  }

  private ObjectTreeNode expandAndLoad(TreePath path)
  {
    if (path == null) return null;

    try
    {
      int count = path.getPathCount();
      ObjectTreeNode toExpand = (ObjectTreeNode)path.getPathComponent(0);
      ObjectTreeNode parent = getTreeModel().findNodeByType(toExpand.getName(), toExpand.getType());
      if (!parent.isLoaded())
      {
        loader.loadChildren(parent);
      }
      expandNode(parent);

      for (int i=1; i < count; i++)
      {
        ObjectTreeNode toFind = (ObjectTreeNode)path.getPathComponent(i);
        ObjectTreeNode node = getTreeModel().findNodeByType(parent, toFind.getName(), toFind.getType());
        if (node != null)
        {
          if (!node.isLoaded())
          {
            loader.loadChildren(node);
          }
          expandNode(node);
          parent = node;
        }
        else
        {
          break;
        }
      }
      return parent;
    }
    catch (Exception ex)
    {
      LogMgr.logError("DbObjectsTree.loadNodesForPath()", "Could not load nodes", ex);
    }
    return null;
  }

  public void reloadSchemas(Set<String> schemas)
  {
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;
    TreePath selection = getSelectionPath();

    try
    {
      loader.getConnection().setBusy(true);
      for (String schema : schemas)
      {
        reloadSchema(schema);
      }
    }
    finally
    {
      loader.getConnection().setBusy(false);
    }

    final ObjectTreeNode toSelect = expandAndLoad(selection);

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        if (toSelect != null)
        {
          TreeNode[] nodes = getTreeModel().getPathToRoot(toSelect);
          TreePath path = new TreePath(nodes);
          setSelectionPath(path);
          scrollPathToVisible(path);
        }
      }
    });

  }

  public void selectObject(TableIdentifier tbl)
  {
    if (tbl == null) return;

    ObjectTreeNode node = findNodeByDbObject(getModel().getRoot(), tbl);
    if (node != null)
    {
      expandNode(node);
    }
  }

  public ObjectTreeNode findNodeByDbObject(ObjectTreeNode parent, TableIdentifier table)
  {
    if (parent == null) return null;
    if (table == null) return null;

    int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)parent.getChildAt(i);
      if (child != null && child.getDbObject() != null)
      {
        if (child.getDbObject() instanceof TableIdentifier)
        {
          TableIdentifier other = (TableIdentifier)child.getDbObject();
          if (table.compareNames(other))
          {
            return child;
          }
        }
      }
      ObjectTreeNode n2 = findNodeByDbObject(child, table);
      if (n2 != null) return n2;
    }
    return null;
  }


  private void reloadSchema(String schema)
  {
    ObjectTreeNode node = getTreeModel().findNodeByType(schema, TreeLoader.TYPE_SCHEMA);
    if (node == null) return;

    node.removeAllChildren();
    try
    {
      loader.reloadSchema(node);
    }
    catch (Exception ex)
    {
      LogMgr.logError("DbObjectsTree.reloadSchema", "Could not load schema", ex);
    }
  }

  public void reload()
  {
    TreePath selection = getSelectionPath();

    load(false);
    final ObjectTreeNode toSelect = expandAndLoad(selection);

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        if (toSelect != null)
        {
          TreeNode[] nodes = getTreeModel().getPathToRoot(toSelect);
          TreePath path = new TreePath(nodes);
          setSelectionPath(path);
          scrollPathToVisible(path);
        }
      }
    });
  }

  public void load(boolean selectDefaultNamespace)
  {
    if (loader == null) return;
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;

    clear();

    try
    {
      loader.load();

      setModel(loader.getModel());

      if (selectDefaultNamespace)
      {
        final boolean useCatalog = loader.getConnection().getDbSettings().supportsCatalogs();
        EventQueue.invokeLater(new Runnable()
        {
          @Override
          public void run()
          {
            boolean selected = false;
            if (useCatalog)
            {
              selected = selectCurrentCatalog();
            }
            if (!selected)
            {
              selectCurrentSchema();
            }
          }
        });
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError("DbObjectsTree.<init>", "Could not load tree", ex);
    }
  }

  public void expandNode(ObjectTreeNode node)
  {
    TreeNode[] nodes = getTreeModel().getPathToRoot(node);
    if (nodes != null)
    {
      selectPath(new TreePath(nodes));
    }
  }

	public void selectPath(TreePath path)
	{
		if (path == null) return;
    setExpandedState(path, true);
		setSelectionPath(path);
		scrollPathToVisible(path);
	}

  public void selectCurrentSchema()
  {
    WbConnection conn = loader.getConnection();
    if (conn == null || conn.isBusy()) return;
    String schema = conn.getCurrentSchema();
    ObjectTreeNode node = getTreeModel().findNodeByType(schema, TreeLoader.TYPE_SCHEMA);
    expandNode(node);
  }

  public boolean selectCurrentCatalog()
  {
    WbConnection conn = loader.getConnection();
    if (conn == null || conn.isBusy()) return false;
    String catalog = conn.getCurrentCatalog();
    if (catalog == null) return false;
    ObjectTreeNode node = getTreeModel().findNodeByType(catalog, TreeLoader.TYPE_CATALOG);
    expandNode(node);
    return node != null;
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
    try
    {
      WbSwingUtilities.showWaitCursor(this);
      statusBar.setStatusMessage(ResourceMgr.getString("MsgRetrieving"));
      loader.loadChildren(node);
    }
    catch (SQLException ex)
    {
      LogMgr.logError("DbObjectsTree.doLoad()", "Could not load node: " + node, ex);
    }
    finally
    {
      WbSwingUtilities.showDefaultCursor(this);
      statusBar.clearStatusMessage();
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

  private DbObjectTreeModel getTreeModel()
  {
    return (DbObjectTreeModel)getModel();
  }

  public ObjectTreeNode findNodeByType(ObjectTreeNode parent, String type)
  {
    if (!parent.canHaveChildren()) return null;
    if (StringUtil.isEmptyString(type)) return null;

    int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)parent.getChildAt(i);
      if (child != null && child.getType().equals(type))
      {
        if (!child.isLoaded())
        {
          doLoad(child);
        }
        return child;
      }
    }
    return null;
  }

  public void reloadNode(ObjectTreeNode node)
    throws SQLException
  {
    if (node == null) return;

    if (TreeLoader.TYPE_SCHEMA.equals(node.getType()))
    {
      loader.reloadSchema(node);
    }
    else
    {
      node.removeAllChildren();
      loader.loadChildren(node);
    }

  }

  public void expandNodes(List<TreePath> nodes)
  {
    if (nodes == null) return;

    for (TreePath path : nodes)
    {
      expandPath(path);
    }
  }

  public List<TreePath> getExpandedNodes()
  {
    List<TreePath> result = new ArrayList<>();
    int count = getRowCount();
    for (int i=0; i < count; i++)
    {
      if (isExpanded(i))
      {
        TreePath path = getPathForRow(i);
        result.add(path);
      }
    }
    return result;
  }


}
