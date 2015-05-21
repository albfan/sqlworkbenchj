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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbSettings;
import workbench.db.SchemaIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbStatusLabel;

import workbench.util.CollectionUtil;
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
  private Map<ObjectTreeNode, Runnable> afterLoadProcess = new ConcurrentHashMap<>();

  public DbObjectsTree(WbStatusLabel status)
  {
    super(new DbObjectTreeModel());
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

  public void selectObject(final TableIdentifier tbl)
  {
    if (tbl == null) return;

    ObjectTreeNode toSearch = findSchemaNode(tbl.getCatalog(), tbl.getSchema());

    if (shouldLoadSearchNode(toSearch))
    {
      final ObjectTreeNode toLoad = toSearch;
      // if we need to load the schema first, this should be done in a background thread
      // to make sure the UI is not blocked - especially because this method is called
      // from an ActionListener even which means it's called on the EDT
      WbThread th = new WbThread("SchemaLoader")
      {
        @Override
        public void run()
        {
          doLoad(toLoad, true);

          final ObjectTreeNode node = findNodeForTable(toLoad, tbl);

          if (node != null)
          {
            EventQueue.invokeLater(new Runnable()
            {
              @Override
              public void run()
              {
                expandNode(node);
              }
            });
          }
        }
      };
      th.start();
      return;
    }

    if (toSearch == null)
    {
      // no schema or catalog node found --> start with the root node
      // findNodeForTable() will not automatically load the content - which is good
      // because we do not want to load everything just to find this table.
      // when getting here, the table will only be searched in the already loaded nodes
      toSearch = getModel().getRoot();
    }

    ObjectTreeNode node = findNodeForTable(toSearch, tbl);

    if (node != null)
    {
      // this method is already called on the EDT, so there is no need to do it here
      expandNode(node);
    }
  }

  private boolean shouldLoadSearchNode(ObjectTreeNode node)
  {
    if (node == null) return false;
    if (node.childrenAreLoaded()) return false;
    return DbTreeSettings.autoLoadSchemasOnFind(loader.getConnection().getDbId()) && !loader.getConnection().isBusy();
  }

  private ObjectTreeNode findCatalogNode(String catalog)
  {
    if (catalog == null) return null;

    ObjectTreeNode root = getModel().getRoot();

    // catalogs must be first level children, no need for recursion here
    int childCount = root.getChildCount();
    for (int i=0; i < childCount; i++)
    {
      ObjectTreeNode child = root.getChildAt(i);
      if (child.isCatalogNode() && child.getName().equalsIgnoreCase(catalog))
      {
        return child;
      }
    }
    return null;
  }

  private ObjectTreeNode findSchemaNode(String catalog, String schema)
  {
    DbSettings settings = loader.getConnection().getDbSettings();
    if (!settings.supportsCatalogs() && !settings.supportsSchemas())
    {
      return getModel().getRoot();
    }

    ObjectTreeNode catNode = null;
    if (settings.supportsCatalogs())
    {
      catNode = findCatalogNode(catalog);
    }

    if (schema == null)
    {
      // no schema supplied so the catNode needs to be searched
      // if catNode is null, nothing will be searched
      if (shouldLoadSearchNode(catNode))
      {
        doLoad(catNode, true);
      }
      return catNode;
    }

    SchemaIdentifier id = new SchemaIdentifier(schema);
    id.setCatalog(catalog);

    ObjectTreeNode searchNode = getModel().getRoot();
    if (catNode != null)
    {
      if (shouldLoadSearchNode(catNode))
      {
        doLoad(catNode, true);
      }
      searchNode = catNode;
    }
    return findSchemaNode(searchNode, id);
  }

  private ObjectTreeNode findSchemaNode(ObjectTreeNode parent, SchemaIdentifier schema)
  {
    if (parent == null) return null;
    if (schema == null) return null;

    if (parent.isSchemaNode() && schema.equals(parent.getDbObject())) return parent;

    int childCount = parent.getChildCount();
    for (int i=0; i < childCount; i++)
    {
      ObjectTreeNode child = parent.getChildAt(i);
      if (schema.equals(child.getDbObject())) return child;
      if (child.isCatalogNode())
      {
        ObjectTreeNode n2 = findSchemaNode(child, schema);
        if (n2 != null) return n2;
      }
    }
    return null;
  }

  private ObjectTreeNode findNodeForTable(ObjectTreeNode parent, TableIdentifier table)
  {
    if (parent == null) return null;
    if (table == null) return null;

    int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++)
    {
      ObjectTreeNode child = parent.getChildAt(i);
      if (child != null && child.getDbObject() != null && !child.isFKTable())
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
      ObjectTreeNode n2 = findNodeForTable(child, table);
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
    if (node == null) return;
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
    final ObjectTreeNode node = getTreeModel().findNodeByType(schema, TreeLoader.TYPE_SCHEMA);
    expandNode(node);
  }

  private void loadNodeObjects(ObjectTreeNode schemaNode)
  {
    try
    {
      loader.loadNodeObjects(schemaNode);
    }
    catch (SQLException ex)
    {
      LogMgr.logError("DbObjectsTree.selectCurrentSchema()", "Could not expand current schema", ex);
    }
  }

  public boolean selectCurrentCatalog()
  {
    WbConnection conn = loader.getConnection();
    if (conn == null || conn.isBusy()) return false;
    final String catalog = conn.getCurrentCatalog();
    if (catalog == null) return false;
    final ObjectTreeNode catNode = getTreeModel().findNodeByType(catalog, TreeLoader.TYPE_CATALOG);
    expandNode(catNode);

    if (catNode != null && conn.getDbSettings().supportsSchemas())
    {
      // we can't call findNodeByType() right after calling expandNode()
      // because loading of the schemas is done in a background thread.
      // therefor we need to register a "post load" event
      Runnable r = new Runnable()
      {
        @Override
        public void run()
        {
          String schema = loader.getConnection().getCurrentSchema();
          ObjectTreeNode schemaNode = getTreeModel().findNodeByType(catNode, schema, TreeLoader.TYPE_SCHEMA);
          if (schemaNode != null)
          {
            expandNode(schemaNode);
          }
        }
      };
      afterLoadProcess.put(catNode, r);
    }

    return catNode != null;
  }

  private boolean shouldLoadNode(ObjectTreeNode node)
  {
    if (!node.isLoaded()) return true;

    if (node.isSchemaNode())
    {
      return DbTreeSettings.autoloadSchemaObjects() && !node.childrenAreLoaded();
    }
    return false;
  }

  @Override
  public void treeExpanded(TreeExpansionEvent event)
  {
    if (loader == null) return;

    TreePath path = event.getPath();
    if (path == null) return;

    final ObjectTreeNode node = (ObjectTreeNode)path.getLastPathComponent();

    if (!shouldLoadNode(node)) return;
    if (!WbSwingUtilities.isConnectionIdle(this, loader.getConnection())) return;

    WbThread load = new WbThread("DbTree Load Thread")
    {
      @Override
      public void run()
      {
        doLoad(node, DbTreeSettings.autoloadSchemaObjects());
      }
    };
    load.start();
  }

  private void doLoad(final ObjectTreeNode node, boolean loadSchemaObjects)
  {
    try
    {
      WbSwingUtilities.showWaitCursor(this);
      statusBar.setStatusMessage(ResourceMgr.getString("MsgRetrieving"));
      if (!node.isLoaded())
      {
        loader.loadChildren(node);
      }
      if (loadSchemaObjects && (node.isSchemaNode() || node.isCatalogNode()))
      {
        loadNodeObjects(node);
      }
      Runnable postLoad = afterLoadProcess.get(node);
      if (postLoad != null)
      {
        afterLoadProcess.remove(node);
        postLoad.run();
      }
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
          doLoad(child, DbTreeSettings.autoloadSchemaObjects());
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
      Set<String> expandedTypes = CollectionUtil.caseInsensitiveSet();
      for (int i=0; i < node.getChildCount(); i++)
      {
        ObjectTreeNode child = node.getChildAt(i);
        TreePath path = new TreePath(getTreeModel().getPathToRoot(child));
        if (isExpanded(path))
        {
          expandedTypes.add(child.getType());
        }
      }
      node.removeAllChildren();
      loader.loadChildren(node);
      if (node.getDbObject() instanceof TableIdentifier)
      {
        for (int i=0; i < node.getChildCount(); i++)
        {
          ObjectTreeNode child = node.getChildAt(i);
          String type = child.getType();
          if (expandedTypes.contains(type))
          {
            setExpandedState(new TreePath(getTreeModel().getPathToRoot(child)), true);
          }
        }
      }
    }
  }

  public void expandNodes(List<TreePath> nodes)
  {
    if (nodes == null) return;

    for (TreePath path : nodes)
    {
      setExpandedState(path, true);
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

  public List<TreePath> getFilteredNodes()
  {
    List<ObjectTreeNode> filtered = getModel().getFilteredNodes();
    List<TreePath> result = new ArrayList<>(filtered.size());

    for (ObjectTreeNode node : filtered)
    {
      result.add(new TreePath(getTreeModel().getPathToRoot(node)));
    }
    return result;
  }

  public String getSelectedNamespace()
  {
    TreePath path = getSelectionPath();
    if (path == null || path.getPathCount() == 0) return "";
    ObjectTreeNode node = (ObjectTreeNode)path.getLastPathComponent();
    if (node == null) return "";
    return node.getLocationInfo();
  }
}
