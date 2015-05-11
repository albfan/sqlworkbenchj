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
import java.util.Collection;
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
import workbench.db.SchemaIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableDependency;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;

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

  public static final String TYPE_TRIGGERS = "table-trigger";

  private WbConnection connection;
  private DbObjectTreeModel model;
  private ObjectTreeNode root;
  private Collection<String> availableTypes;
  private final Set<String> typesToShow = CollectionUtil.caseInsensitiveSet();

  public TreeLoader()
  {
    root = new RootNode();
    model = new DbObjectTreeModel(root);
  }

  public void setConnection(WbConnection conn, String name)
  {
    setRootName(name);
    connection = conn;
    if (connection != null)
    {
      availableTypes = connection.getMetadata().getObjectTypes();
    }
    if (DbExplorerSettings.getShowTriggerPanel())
    {
      availableTypes.add("TRIGGER");
    }
  }

  private void setRootName(String name)
  {
    removeAllChildren(root);
    root = new RootNode(name);
    model = new DbObjectTreeModel(root);
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

  public void clear()
  {
    removeAllChildren(root);
    root.setNameAndType(ResourceMgr.getString("TxtDbExplorerTables"), TYPE_ROOT);
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
      CatalogChanger changer = new CatalogChanger();
      changer.setFireEvents(false);
      boolean catalogChanged = false;
      String currentCatalog = connection.getMetadata().getCurrentCatalog();
      try
      {
        if (isCatalogChild && connection.getDbSettings().changeCatalogToRetrieveSchemas())
        {
          changer.setCurrentCatalog(connection, parentNode.getName());
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
        if (catalogChanged)
        {
          changer.setCurrentCatalog(connection, currentCatalog);
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

  private void addTypeNodes(ObjectTreeNode parentNode)
  {
    if (parentNode == null) return;
    for (String type : availableTypes)
    {
      if (type.equalsIgnoreCase("TRIGGER")) continue;
      if (typesToShow.isEmpty() || typesToShow.contains(type))
      {
        ObjectTreeNode node = new ObjectTreeNode(type, TYPE_DBO_TYPE_NODE);
        node.setAllowsChildren(true);
        parentNode.add(node);
      }
    }
    // always add triggers at the end
    if (typesToShow.isEmpty() || typesToShow.contains("TRIGGER"))
    {
      ObjectTreeNode node = new ObjectTreeNode("TRIGGER", TYPE_DBO_TYPE_NODE);
      node.setAllowsChildren(true);
      parentNode.add(node);
    }
    parentNode.setChildrenLoaded(true);
  }

  public void loadTypesForSchema(ObjectTreeNode schemaNode)
  {
    if (schemaNode == null) return;
    if (!schemaNode.getType().equals(TYPE_SCHEMA)) return;

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
    loadSchemaTypes(schemaNode);
  }

  public void loadSchemaTypes(ObjectTreeNode schemaNode)
    throws SQLException
  {
    if (schemaNode == null) return;
    if (!schemaNode.isSchemaNode()) return;

    int count = schemaNode.getChildCount();
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)schemaNode.getChildAt(i);
      if (!child.isLoaded())
      {
        loadObjectsForTypeNode(child);
      }
    }
    schemaNode.setChildrenLoaded(true);
    model.nodeStructureChanged(schemaNode);
    model.nodeChanged(schemaNode);
  }

  public void reloadTableNode(ObjectTreeNode node)
    throws SQLException
  {
    DbObject dbo = node.getDbObject();
    if (! (dbo instanceof TableIdentifier) ) return;

    node.removeAllChildren();
    addColumnsNode(node);
    addTableNodes(node);

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
    else if (parent.getType().equalsIgnoreCase(TYPE_CATALOG))
    {
      catalog = parent.getName();
    }
    else if (parent.getType().equalsIgnoreCase(TYPE_SCHEMA))
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
    if (typeNode.getName().equalsIgnoreCase("TRIGGER"))
    {
      loadTriggers(typeNode);
      return;
    }

    TableIdentifier info = getParentInfo(typeNode);
    String schema = info.getRawSchema();
    String catalog = info.getRawCatalog();

    boolean loaded = true;
    List<TableIdentifier> objects = connection.getMetadata().getObjectList(null, catalog, schema, new String[] { typeNode.getName() });
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
        addTableNodes(node);
        connection.getObjectCache().addTable(new TableDefinition(tbl));
      }
      else if (hasIndexes(node))
      {
        node.setAllowsChildren(true);
        addIndexNode(node);
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
      ObjectTreeNode trg = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTriggers"), TYPE_TRIGGERS);
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

  private void addIndexNode(ObjectTreeNode node)
  {
    ObjectTreeNode idx = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerIndexes"), TYPE_INDEX_LIST);
    idx.setAllowsChildren(true);
    node.add(idx);
  }

  private void addTableNodes(ObjectTreeNode node)
  {
    addIndexNode(node);

    ObjectTreeNode fk = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerFkColumns"), TYPE_FK_LIST);
    fk.setAllowsChildren(true);
    node.add(fk);

    ObjectTreeNode ref = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerReferencedColumns"), TYPE_REF_LIST);
    ref.setAllowsChildren(true);
    node.add(ref);

    ObjectTreeNode trg = new ObjectTreeNode(ResourceMgr.getString("TxtDbExplorerTriggers"), TYPE_TRIGGERS);
    trg.setAllowsChildren(true);
    node.add(trg);
  }

  public void loadTriggers(ObjectTreeNode trgNode)
    throws SQLException
  {
    if (trgNode == null) return;

    TableIdentifier info = getParentInfo(trgNode);
    TriggerReader reader = TriggerReaderFactory.createReader(connection);
    List<TriggerDefinition> triggers = reader.getTriggerList(info.getRawCatalog(), info.getRawSchema(), null);

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
    List<IndexDefinition> indexes = meta.getIndexReader().getTableIndexList(tbl);
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
    }
    else
    {
      fklist = deps.getOutgoingForeignKeys();
    }

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
      this.connection.setBusy(true);
      String type = node.getType();

      if (TYPE_CATALOG.equals(type) && connection.getDbSettings().supportsSchemas())
      {
        loadSchemas(node);
      }
      else if (TYPE_SCHEMA.equals(type))
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
      else if (TYPE_TRIGGERS.equals(type))
      {
        ObjectTreeNode parent = node.getParent();
        DbObject dbo = parent.getDbObject();
        loadTableTriggers(dbo, node);
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
    }
  }
}
