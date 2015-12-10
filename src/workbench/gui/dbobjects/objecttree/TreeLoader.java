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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.ResourceMgr;

import workbench.db.CatalogChanger;
import workbench.db.CatalogIdentifier;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.DependencyNode;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.SchemaIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.TableNameSorter;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;
import workbench.db.dependency.DependencyReaderFactory;

import workbench.gui.dbobjects.IsolationLevelChanger;

import workbench.util.CollectionUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class TreeLoader
{
  /**
   * The node type for schema elements.
   */
  public static final String TYPE_ROOT = "database";

  /**
   * The node type for schema elements.
   */
  public static final String TYPE_SCHEMA = "schema";

  /**
   * The node type for catalog elements.
   */
  public static final String TYPE_CATALOG = "catalog";

  /**
   * The node type for table like elements.
   */
  public static final String TYPE_TABLE = "table";

  /**
   * The node type for view elements.
   */
  public static final String TYPE_VIEW = "view";

  /**
   * The node type for the "columns" node in a table or a view.
   */
  public static final String TYPE_COLUMN_LIST = "column-list";

  /**
   * The node type for the "indexes" node in a table or a view.
   */
  public static final String TYPE_INDEX_LIST = "index-list";

  /**
   * The node type for the "dependencies" node in a table or a view.
   */
  public static final String TYPE_DEPENDENCY_USED = "dep-used";

  /**
   * The node type for the "dependencies" node in a table or a view.
   */
  public static final String TYPE_DEPENDENCY_USING = "dep-using";

  /**
   * The node type for the foreign key nodes in a table.
   * These are the "outgoing" foreign keys, i.e. columns from the "current" table
   * referencing other tables.
   */
  public static final String TYPE_FK_LIST = "referenced-fk-list";

  /**
   * The node type for the foreign key nodes in a table.
   * These are the "incoming" foreign keys, i.e. columns from the other tables
   * referencing the current table.
   */
  public static final String TYPE_REF_LIST = "referencing-fk-list";

  /**
   * The node type identifying an index column.
   */
  public static final String TYPE_IDX_COL = "index-column";

  /**
   * The node type identifying nodes that group object types together.
   * Those nodes are typically "TABLE", "VIEW" and so on.
   */
  public static final String TYPE_DBO_TYPE_NODE = "dbobject-type";

  public static final String TYPE_FK_DEF = "fk-definition";

  public static final String TYPE_TRIGGERS_NODE = "table-trigger";

  public static final String TYPE_FUNCTION = "function";
  public static final String TYPE_PROCEDURES_NODE = "procedures";
  public static final String TYPE_PROC_PARAMETER = "parameter";
  public static final String TYPE_PACKAGE_NODE = "package";

  private WbConnection connection;
  private DbObjectTreeModel model;
  private ObjectTreeNode root;
  private Collection<String> availableTypes;
  private ProcedureTreeLoader procLoader;
  private DependencyReader dependencyLoader;
  private final Set<String> typesToShow = CollectionUtil.caseInsensitiveSet();
  private IsolationLevelChanger levelChanger = new IsolationLevelChanger();

  public TreeLoader()
  {
    root = new RootNode();
    model = new DbObjectTreeModel(root);
  }

  public void setConnection(WbConnection conn)
  {
    connection = conn;
    clear();
    if (connection != null)
    {
      availableTypes = connection.getMetadata().getObjectTypes();
    }
    if (DbExplorerSettings.getShowTriggerPanel())
    {
      availableTypes.add("TRIGGER");
    }
    procLoader = new ProcedureTreeLoader();
    dependencyLoader = DependencyReaderFactory.getReader(connection);
  }

  private void removeAllChildren(ObjectTreeNode node)
  {
    if (node == null) return;
    int count = node.getChildCount();
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)node.getChildAt(i);
      removeAllChildren(child);
    }
    node.removeAllChildren();
  }

  public void setSelectedTypes(List<String> types)
  {
    typesToShow.clear();
    if (types != null)
    {
      typesToShow.addAll(types);
    }
  }

  private String getRootName()
  {
    if (this.connection == null || connection.isClosed() || connection.getDbSettings() == null)
    {
      return ResourceMgr.getString("TxtDbExplorerTables");
    }

    if (connection.getDbSettings().supportsCatalogs())
    {
      if (connection.getMetadata().getCatalogTerm().toLowerCase().equals("database"))
      {
        return ResourceMgr.getString("LblDatabases");
      }
      return ResourceMgr.getString("LblCatalogs");
    }

    if (connection.getDbSettings().supportsSchemas())
    {
      return ResourceMgr.getString("LblSchemas");
    }

    return ResourceMgr.getString("TxtDbExplorerTables");
  }

  public void clear()
  {
    removeAllChildren(root);
    root.setNameAndType(getRootName(), TYPE_ROOT);
    model.nodeStructureChanged(root);
  }

  public DbObjectTreeModel getModel()
  {
    return model;
  }

  public void load()
    throws SQLException
  {
    boolean loaded = false;

    if (connection == null) return;

    if (connection.getDbSettings().supportsCatalogs())
    {
      loaded = loadCatalogs(root);
    }

    if (!loaded && connection.getDbSettings().supportsSchemas())
    {
      loaded = loadSchemas(root);
    }

    if (!loaded)
    {
      addTypeNodes(root);
      root.setChildrenLoaded(true);
      model.nodeStructureChanged(root);
    }
  }


  public boolean loadSchemas(ObjectTreeNode parentNode)
    throws SQLException
  {
    if (DbTreeSettings.showOnlyCurrentSchema(connection.getDbId()))
    {
      String schema = connection.getCurrentSchema();
      if (schema == null) return false;

      parentNode.setNameAndType(schema, TYPE_SCHEMA);
      SchemaIdentifier id = new SchemaIdentifier(schema);
      parentNode.setUserObject(id);
      parentNode.setAllowsChildren(true);
      parentNode.setChildrenLoaded(false);
      addTypeNodes(parentNode);
    }
    else
    {
      boolean isCatalogChild = parentNode.getType().equals(TYPE_CATALOG);
      CatalogChanger catalogChanger = new CatalogChanger();
      catalogChanger.setFireEvents(false);
      boolean catalogChanged = false;
      String currentCatalog = connection.getMetadata().getCurrentCatalog();
      try
      {
        levelChanger.changeIsolationLevel(connection);
        if (isCatalogChild && connection.getDbSettings().changeCatalogToRetrieveSchemas())
        {
          catalogChanger.setCurrentCatalog(connection, parentNode.getName());
          catalogChanged = true;
        }

        List<String> schemas = connection.getMetadata().getSchemas(connection.getSchemaFilter());
        if (CollectionUtil.isEmpty(schemas)) return false;

        for (String schema : schemas)
        {
          ObjectTreeNode node = new ObjectTreeNode(schema, TYPE_SCHEMA);
          node.setAllowsChildren(true);
          SchemaIdentifier id = new SchemaIdentifier(schema);
          if (isCatalogChild)
          {
            id.setCatalog(parentNode.getName());
          }
          node.setUserObject(id);
          parentNode.add(node);
          addTypeNodes(node);
        }
      }
      finally
      {
        levelChanger.restoreIsolationLevel(connection);
        if (catalogChanged)
        {
          catalogChanger.setCurrentCatalog(connection, currentCatalog);
        }
      }
      parentNode.setChildrenLoaded(true);
    }
    model.nodeStructureChanged(parentNode);
    return true;
  }

  public boolean loadCatalogs(ObjectTreeNode parentNode)
    throws SQLException
  {
    try
    {
      levelChanger.changeIsolationLevel(connection);
      List<String> catalogs = connection.getMetadata().getCatalogInformation(connection.getCatalogFilter());
      for (String cat : catalogs)
      {
        ObjectTreeNode node = new ObjectTreeNode(cat, TYPE_CATALOG);
        node.setAllowsChildren(true);
        CatalogIdentifier id = new CatalogIdentifier(cat);
        id.setTypeName(connection.getMetadata().getCatalogTerm());
        node.setUserObject(id);
        parentNode.add(node);
        if (!connection.getDbSettings().supportsSchemas())
        {
          addTypeNodes(node);
        }
      }
      model.nodeStructureChanged(parentNode);
      return catalogs.size() > 0;
    }
    finally
    {
      levelChanger.restoreIsolationLevel(connection);
    }
  }

  private void addTypeNodes(ObjectTreeNode parentNode)
  {
    if (parentNode == null) return;
    for (String type : availableTypes)
    {
      if (type.equalsIgnoreCase("TRIGGER") || type.equalsIgnoreCase("PROCEDURE")) continue;
      if (typesToShow.isEmpty() || typesToShow.contains(type))
      {
        ObjectTreeNode node = new ObjectTreeNode(type, TYPE_DBO_TYPE_NODE);
        node.setAllowsChildren(true);
        parentNode.add(node);
      }
    }

    if (typesToShow.isEmpty() || typesToShow.contains("PROCEDURE"))
    {
      String label = ResourceMgr.getString("TxtDbExplorerProcs");
      if (connection.getMetadata().isPostgres())
      {
        label = ResourceMgr.getString("TxtDbExplorerFuncs");
      }
      ObjectTreeNode node = new ObjectTreeNode(label, TYPE_PROCEDURES_NODE);
      node.setAllowsChildren(true);
      node.setChildrenLoaded(false);
      parentNode.add(node);
    }

    // always add triggers at the end
    if (typesToShow.isEmpty() || typesToShow.contains("TRIGGER"))
    {
      ObjectTreeNode node = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTriggers"), TYPE_TRIGGERS_NODE);
      node.setAllowsChildren(true);
      parentNode.add(node);
    }

    parentNode.setChildrenLoaded(true);
  }

  public void loadTypesForSchema(ObjectTreeNode schemaNode)
  {
    if (schemaNode == null) return;
    if (!schemaNode.isSchemaNode()) return;

    int count = schemaNode.getChildCount();
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = schemaNode.getChildAt(i);
      try
      {
        child.removeAllChildren();
        loadObjectsForTypeNode(child);
      }
      catch (SQLException ex)
      {
        LogMgr.logError("TreeLoader.loadTypesForSchema()", "Could not load schema nodes for " + child.displayString(), ex);
      }
    }
  }

  public void reloadSchema(ObjectTreeNode schemaNode)
    throws SQLException
  {
    if (schemaNode == null) return;
    if (!schemaNode.isSchemaNode()) return;

    schemaNode.removeAllChildren();
    addTypeNodes(schemaNode);
    loadNodeObjects(schemaNode);
  }

  public void loadNodeObjects(ObjectTreeNode node)
    throws SQLException
  {
    if (node == null) return;

    int count = node.getChildCount();
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)node.getChildAt(i);
      if (!child.isLoaded())
      {
        loadObjectsForTypeNode(child);
      }
    }
    node.setChildrenLoaded(true);
    model.nodeStructureChanged(node);
    model.nodeChanged(node);
  }

  public void reloadTableNode(ObjectTreeNode node)
    throws SQLException
  {
    DbObject dbo = node.getDbObject();
    if (! (dbo instanceof TableIdentifier) ) return;

    node.removeAllChildren();
    addColumnsNode(node);
    addTableSubNodes(node);

    int count = node.getChildCount();

    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = node.getChildAt(i);
      loadChildren(child);
    }
    node.setChildrenLoaded(true);
    model.nodeStructureChanged(node);
  }

  public void reloadNode(ObjectTreeNode node)
    throws SQLException
  {
    if (node == null) return;

    node.removeAllChildren();
    loadChildren(node);
    node.setChildrenLoaded(true);
    model.nodeStructureChanged(node);
  }

  private TableIdentifier getParentInfo(ObjectTreeNode node)
  {
    ObjectTreeNode parent = node.getParent();
    if (parent == null)
    {
      return new TableIdentifier(null, null, "$wb-dummy$", false);
    }
    String schema = null;
    String catalog = null;

    if (parent.getDbObject() != null)
    {
      // this is typically a CatalogIdentifier or SchemaIdentifier
      DbObject dbo = parent.getDbObject();
      schema = dbo.getSchema();
      catalog = dbo.getCatalog();
    }
    else if (parent.isCatalogNode())
    {
      catalog = parent.getName();
    }
    else if (parent.isSchemaNode())
    {
      schema = parent.getName();
    }

    if (connection.getDbSettings().supportsCatalogs() && connection.getDbSettings().supportsSchemas() && catalog == null)
    {
      // if schemas and catalogs are supported, the current node must be a schema
      // and the parent of that must be a catalog
      ObjectTreeNode catNode = parent.getParent();
      if (catNode != null && catNode.getType().equalsIgnoreCase(TYPE_CATALOG))
      {
        catalog = catNode.getName();
      }
    }
    return new TableIdentifier(catalog, schema, "$wb-dummy$", false);
  }

  public void loadObjectsForTypeNode(ObjectTreeNode typeNode)
    throws SQLException
  {
    if (typeNode == null) return;
    if (typeNode.getType().equalsIgnoreCase(TYPE_TRIGGERS_NODE))
    {
      loadTriggers(typeNode);
      return;
    }

    if (typeNode.getType().equalsIgnoreCase(TYPE_PROCEDURES_NODE))
    {
      loadProcedures(typeNode);
      return;
    }

    TableIdentifier info = getParentInfo(typeNode);
    String schema = info.getRawSchema();
    String catalog = info.getRawCatalog();

    boolean loaded = true;
    List<TableIdentifier> objects = connection.getMetadata().getObjectList(null, catalog, schema, new String[] { typeNode.getName() });

    TableNameSorter sorter = new TableNameSorter();
    sorter.setUseNaturalSort(DbTreeSettings.useNaturalSort());
    Collections.sort(objects, sorter);

    for (TableIdentifier tbl : objects)
    {
      ObjectTreeNode node = new ObjectTreeNode(tbl);
      node.setAllowsChildren(false);
      if (hasColumns(tbl))
      {
        node.setAllowsChildren(true);
        if (connection.getMetadata().isExtendedObject(tbl))
        {
          loaded = false;
        }
        else
        {
          addColumnsNode(node);
        }
      }

      if (isTable(tbl))
      {
        addTableSubNodes(node);
        connection.getObjectCache().addTable(new TableDefinition(tbl));
      }
      else if (hasIndexes(node))
      {
        node.setAllowsChildren(true);
        addIndexNode(node);
        addDependencyNodes(node);
      }
      else
      {
        addDependencyNodes(node);
      }

      if (connection.getMetadata().isViewType(typeNode.getName()))
      {
        addViewTriggerNode(node);
        connection.getObjectCache().addTable(new TableDefinition(tbl));
      }

      typeNode.add(node);
      node.setChildrenLoaded(loaded);
    }
    typeNode.setChildrenLoaded(true);
    model.nodeStructureChanged(typeNode);
    model.nodeChanged(typeNode);
  }

  private boolean supportsDependencies(ObjectTreeNode node)
  {
    if (dependencyLoader == null) return false;
    if (node == null) return false;
    if (node.getDbObject() == null) return false;
    return dependencyLoader.supportsDependencies(node.getDbObject().getObjectType());
  }

  private boolean hasIndexes(ObjectTreeNode node)
  {
    if (node == null) return false;
    DbObject dbo = node.getDbObject();
    if (dbo == null) return false;
		DbSettings dbs = connection.getDbSettings();
		if (dbs.isViewType(dbo.getObjectType()) && dbs.supportsIndexedViews())
		{
			return true;
		}
    return dbs.isMview(dbo.getObjectType());
  }

  private void addViewTriggerNode(ObjectTreeNode node)
  {
		TriggerReader reader = TriggerReaderFactory.createReader(connection);
		if (reader == null) return;
		if (reader.supportsTriggersOnViews())
    {
      ObjectTreeNode trg = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTriggers"), TYPE_TRIGGERS_NODE);
      trg.setAllowsChildren(true);
      node.add(trg);
    }
  }

  private void addColumnsNode(ObjectTreeNode node)
  {
    ObjectTreeNode cols = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTableDefinition"), TYPE_COLUMN_LIST);
    cols.setAllowsChildren(true);
    node.add(cols);
  }

  public boolean addDependencyNodes(ObjectTreeNode node)
  {
    if (node == null) return false;
    if (supportsDependencies(node))
    {
      node.setAllowsChildren(true);
      ObjectTreeNode usingNode = new ObjectTreeNode(ResourceMgr.getString("TxtDepsUses"), TYPE_DEPENDENCY_USING);
      usingNode.setAllowsChildren(true);
      node.add(usingNode);

      ObjectTreeNode usedNode = new ObjectTreeNode(ResourceMgr.getString("TxtDepsUsedBy"), TYPE_DEPENDENCY_USED);
      usedNode.setAllowsChildren(true);
      node.add(usedNode);
      return true;
    }
    return false;
  }

  private void addIndexNode(ObjectTreeNode node)
  {
    ObjectTreeNode idx = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerIndexes"), TYPE_INDEX_LIST);
    idx.setAllowsChildren(true);
    node.add(idx);
  }

  private void addTableSubNodes(ObjectTreeNode node)
  {
    addIndexNode(node);

    ObjectTreeNode fk = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerFkColumns"), TYPE_FK_LIST);
    fk.setAllowsChildren(true);
    node.add(fk);

    ObjectTreeNode ref = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), TYPE_REF_LIST);
    ref.setAllowsChildren(true);
    node.add(ref);

    addDependencyNodes(node);

    ObjectTreeNode trg = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTriggers"), TYPE_TRIGGERS_NODE);
    trg.setAllowsChildren(true);
    node.add(trg);
  }

  public void loadProcedures(ObjectTreeNode procNode)
    throws SQLException
  {
    procLoader.loadProcedures(procNode, model, connection, this);
  }

  public boolean isDependencyNode(ObjectTreeNode node)
  {
    if (node == null) return false;
    return (node.getType().equals(TYPE_DEPENDENCY_USED) || node.getType().equals(TYPE_DEPENDENCY_USING));
  }

  private void addNodes(ObjectTreeNode parent, List<ObjectTreeNode> nodes)
  {
    if (CollectionUtil.isEmpty(nodes)) return;
    if (parent == null) return;
    if (!parent.canHaveChildren()) return;

    for (ObjectTreeNode node : nodes)
    {
      parent.add(node);
    }
  }

  private List<ObjectTreeNode> removeDependencyNodes(ObjectTreeNode parent)
  {
    List<ObjectTreeNode> nodes = new ArrayList<>(2);
    if (!supportsDependencies(parent)) return nodes;

    for (int i=0; i < parent.getChildCount(); i++)
    {
      ObjectTreeNode child = parent.getChildAt(i);
      if (isDependencyNode(child))
      {
        nodes.add(child);
      }
    }

    for (ObjectTreeNode child : nodes)
    {
      parent.remove(child);
    }

    return nodes;
  }

  public void loadProcedureParameters(ObjectTreeNode node)
  {
    if (node == null) return;
    ProcedureDefinition proc = (ProcedureDefinition)node.getDbObject();
    if (proc == null) return;

    List<ColumnIdentifier> parameters = proc.getParameters(connection);

    List<ObjectTreeNode> deps = removeDependencyNodes(node);

    for (ColumnIdentifier col : parameters)
    {
      String mode = col.getArgumentMode();
      ObjectTreeNode p = null;
      if (mode.equals("RETURN"))
      {
        p = new ObjectTreeNode("RETURNS " + col.getDbmsType(), TYPE_PROC_PARAMETER);
      }
      else
      {
        p = new ObjectTreeNode(col);
        p.setNameAndType(col.getColumnName(), TYPE_PROC_PARAMETER);
      }
      p.setAllowsChildren(false);
      p.setChildrenLoaded(true);
      node.add(p);
    }
    addNodes(node, deps);
    model.nodeStructureChanged(node);
    node.setChildrenLoaded(true);
  }


  public void loadTriggers(ObjectTreeNode trgNode)
    throws SQLException
  {
    if (trgNode == null) return;

    TableIdentifier info = getParentInfo(trgNode);
    TriggerReader reader = TriggerReaderFactory.createReader(connection);
    List<TriggerDefinition> triggers = reader.getTriggerList(info.getRawCatalog(), info.getRawSchema(), null);
    DbObjectSorter.sort(triggers, DbExplorerSettings.useNaturalSort());

    for (TriggerDefinition trg : triggers)
    {
      ObjectTreeNode node = new ObjectTreeNode(trg);
      boolean supportsDeps = supportsDependencies(node);
      node.setAllowsChildren(supportsDeps);
      addDependencyNodes(node);
      node.setChildrenLoaded(!supportsDeps);
      trgNode.add(node);
    }
    model.nodeStructureChanged(trgNode);
    trgNode.setChildrenLoaded(true);
  }

  public void loadTableTriggers(DbObject dbo, ObjectTreeNode trgNode)
    throws SQLException
  {
    if (trgNode == null) return;
    if (dbo == null)
    {
      trgNode.setAllowsChildren(false);
      return;
    }

    TriggerReader reader = TriggerReaderFactory.createReader(connection);

    TableIdentifier tbl = (TableIdentifier)dbo;
    List<TriggerDefinition> triggers = reader.getTriggerList(tbl.getRawCatalog(), tbl.getRawSchema(), tbl.getRawTableName());
    DbObjectSorter.sort(triggers, DbExplorerSettings.useNaturalSort());

    for (TriggerDefinition trg : triggers)
    {
      ObjectTreeNode node = new ObjectTreeNode(trg);
      node.setAllowsChildren(false);
      node.setChildrenLoaded(true);
      trgNode.add(node);
    }
    model.nodeStructureChanged(trgNode);
    trgNode.setChildrenLoaded(true);
  }

  public void loadTableIndexes(DbObject dbo, ObjectTreeNode indexNode)
    throws SQLException
  {
    if (indexNode == null) return;
    if (dbo == null)
    {
      indexNode.setAllowsChildren(false);
      return;
    }

    DbMetadata meta = connection.getMetadata();

    TableIdentifier tbl = (TableIdentifier)dbo;
    List<IndexDefinition> indexes = meta.getIndexReader().getTableIndexList(tbl, false);
    DbObjectSorter.sort(indexes, DbExplorerSettings.useNaturalSort());

    for (IndexDefinition idx : indexes)
    {
      ObjectTreeNode node = new ObjectTreeNode(idx);
      node.setAllowsChildren(true);
      node.setChildrenLoaded(true);
      for (IndexColumn col : idx.getColumns())
      {
         ObjectTreeNode idxCol = new ObjectTreeNode(col.getExpression(), TYPE_IDX_COL);
         idxCol.setAllowsChildren(false);
         idxCol.setChildrenLoaded(true);
         node.add(idxCol);
      }
      indexNode.add(node);
    }
    model.nodeStructureChanged(indexNode);
    indexNode.setChildrenLoaded(true);
  }

  public void loadDependencies(DbObject dbo, ObjectTreeNode depNode)
  {
    if (depNode == null) return;
    if (dbo == null || dependencyLoader == null)
    {
      depNode.setAllowsChildren(false);
      return;
    }

    List<DbObject> objects = null;
    if (depNode.getType().equals(TYPE_DEPENDENCY_USED))
    {
      objects = dependencyLoader.getUsedBy(connection, dbo);
    }
    else if (depNode.getType().equals(TYPE_DEPENDENCY_USING))
    {
      objects = dependencyLoader.getUsedObjects(connection, dbo);
    }

    for (DbObject obj : objects)
    {
      ObjectTreeNode node = new ObjectTreeNode(obj);
      node.setAllowsChildren(node.canHaveChildren());
      depNode.add(node);
    }
    model.nodeStructureChanged(depNode);
    depNode.setChildrenLoaded(true);
  }

  public void loadTableColumns(DbObject dbo, ObjectTreeNode columnsNode)
    throws SQLException
  {
    if (columnsNode == null) return;
    if (dbo == null)
    {
      columnsNode.setAllowsChildren(false);
      return;
    }

    List<ColumnIdentifier> columns = connection.getMetadata().getObjectColumns(dbo);
    if (columns == null)
    {
      return;
    }

    List<ObjectTreeNode> deps = removeDependencyNodes(columnsNode);

    if (dbo instanceof TableIdentifier)
    {
      TableIdentifier tbl = (TableIdentifier)dbo;
      connection.getObjectCache().addTable(new TableDefinition(tbl, columns));
    }

    for (ColumnIdentifier col : columns)
    {
      ObjectTreeNode node = new ObjectTreeNode(col);
      node.setAllowsChildren(false);
      columnsNode.add(node);
    }

    addNodes(columnsNode, deps);

    model.nodeStructureChanged(columnsNode);
    columnsNode.setChildrenLoaded(true);
  }

  public void loadForeignKeys(DbObject dbo, ObjectTreeNode fkNode, boolean showIncoming)
    throws SQLException
  {
    if (fkNode == null) return;
    if (dbo == null)
    {
      fkNode.setAllowsChildren(false);
      return;
    }

    DbMetadata meta = connection.getMetadata();
    if (!meta.isTableType(dbo.getObjectType())) return;

    TableIdentifier tbl = (TableIdentifier)dbo;
    TableDependency deps = new TableDependency(connection, tbl);

    List<DependencyNode> fklist = null;

    if (showIncoming)
    {
      fklist = deps.getIncomingForeignKeys();
      connection.getObjectCache().addReferencingTables(tbl, fklist);
    }
    else
    {
      fklist = deps.getOutgoingForeignKeys();
      connection.getObjectCache().addReferencedTables(tbl, fklist);
    }


    Collections.sort(fklist, new Comparator<DependencyNode>()
    {
      @Override
      public int compare(DependencyNode o1, DependencyNode o2)
      {
        return o1.getTable().compareTo(o2.getTable());
      }
    });

    for (DependencyNode fk : fklist)
    {
      TableIdentifier table = fk.getTable();
      ObjectTreeNode tblNode = new ObjectTreeNode(table);
      tblNode.setAllowsChildren(true);
      tblNode.setChildrenLoaded(true);
      fkNode.add(tblNode);

      String colDisplay = "<html><b>" + fk.getFkName() + "</b>: ";

      if (showIncoming)
      {
        colDisplay += fk.getTable().getTableExpression(connection) + "(" + fk.getSourceColumnsList() + ") REFERENCES  " +
        tbl.getTableExpression(connection) + "(" + fk.getTargetColumnsList();
      }
      else
      {
        colDisplay += tbl.getTableExpression(connection) + "(" + fk.getTargetColumnsList() + ") REFERENCES  " +
        fk.getTable().getTableExpression(connection) + "(" + fk.getSourceColumnsList();

      }
      colDisplay += ")</html>";

      ObjectTreeNode fkEntry = new ObjectTreeNode(colDisplay, TYPE_FK_DEF);
      fkEntry.setAllowsChildren(false);
      fkEntry.setChildrenLoaded(true);
      tblNode.add(fkEntry);
    }
    model.nodeStructureChanged(fkNode);
    fkNode.setChildrenLoaded(true);
  }


  private boolean isTable(DbObject dbo)
  {
    if (dbo == null) return false;
    DbMetadata meta = connection.getMetadata();
    return meta.isExtendedTableType(dbo.getObjectType());
  }

  private boolean hasColumns(DbObject dbo)
  {
    if (dbo == null) return false;
    DbMetadata meta = connection.getMetadata();
    return meta.hasColumns(dbo);
  }

  public WbConnection getConnection()
  {
    return connection;
  }

  public void loadChildren(ObjectTreeNode node)
    throws SQLException
  {
    if (node == null) return;

    try
    {
      levelChanger.changeIsolationLevel(connection);
      this.connection.setBusy(true);
      String type = node.getType();

      if (node.isCatalogNode())
      {
        if (connection.getDbSettings().supportsSchemas())
        {
          loadSchemas(node);
        }
        else if (!node.childrenAreLoaded())
        {
          addTypeNodes(node);
          loadNodeObjects(node);
        }
      }
      else if (node.isSchemaNode())
      {
        loadTypesForSchema(node);
      }
      else if (TYPE_DBO_TYPE_NODE.equals(type))
      {
        loadObjectsForTypeNode(node);
      }
      else if (TYPE_COLUMN_LIST.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadTableColumns(dbo, node);
      }
      else if (TYPE_INDEX_LIST.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadTableIndexes(dbo, node);
      }
      else if (TYPE_FK_LIST.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadForeignKeys(dbo, node, false);
      }
      else if (TYPE_REF_LIST.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadForeignKeys(dbo, node, true);
      }
      else if (TYPE_TRIGGERS_NODE.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        if (dbo instanceof TableIdentifier)
        {
          loadTableTriggers(dbo, node);
        }
        else
        {
          loadTriggers(node);
        }
      }
      else if (TYPE_DEPENDENCY_USED.equals(type) || TYPE_DEPENDENCY_USING.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadDependencies(dbo, node);
      }
      else if (TYPE_PROCEDURES_NODE.equals(type))
      {
        loadProcedures(node);
      }
      else if (node.getDbObject() instanceof ProcedureDefinition)
      {
        loadProcedureParameters(node);
      }
      else if (connection.getMetadata().isExtendedTableType(type) || connection.getMetadata().isViewType(type))
      {
        reloadTableNode(node);
      }
      else if (connection.getMetadata().hasColumns(type))
      {
        loadTableColumns(node.getDbObject(), node);
      }
    }
    finally
    {
      this.connection.setBusy(false);
      levelChanger.restoreIsolationLevel(connection);
    }
  }
}
