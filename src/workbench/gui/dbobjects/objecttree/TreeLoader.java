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

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;


/**
 *
 * @author Thomas Kellerer
 */
public class TreeLoader
{
  private WbConnection connection;
  private DbObjectTreeModel model;
  private ObjectTreeNode root;

  public TreeLoader(String name)
  {
    root = new ObjectTreeNode(name, "database");
    root.setAllowsChildren(true);
    model = new DbObjectTreeModel(root);
  }

  public void setConnection(WbConnection conn)
  {
    connection = conn;
  }

  private void removeAllChildren(ObjectTreeNode node)
  {
    int count = node.getChildCount();
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = (ObjectTreeNode)node.getChildAt(i);
      removeAllChildren(child);
    }
    node.removeAllChildren();
  }

  public void clear()
  {
    removeAllChildren(root);
  }

  public DbObjectTreeModel getModel()
  {
    return model;
  }

  public void load()
    throws SQLException
  {
    loadSchemas(root);
  }

  public void loadSchemas(ObjectTreeNode parentNode)
    throws SQLException
  {
    List<String> schemas = connection.getMetadata().getSchemas();
    for (String schema : schemas)
    {
      ObjectTreeNode node = new ObjectTreeNode(schema, "schema");
      node.setAllowsChildren(true);
      parentNode.add(node);
    }
    model.nodeStructureChanged(parentNode);
    parentNode.setChildrenLoaded(true);
  }


  public void loadCatalogs(ObjectTreeNode parentNode)
    throws SQLException
  {
    List<String> catalogs = connection.getMetadata().getCatalogs();
    for (String cat : catalogs)
    {
      ObjectTreeNode node = new ObjectTreeNode(cat, "catalog");
      node.setAllowsChildren(true);
      parentNode.add(node);
    }
    model.nodeStructureChanged(parentNode);
  }

  public void loadTypes(ObjectTreeNode schemaNode)
  {
    if (schemaNode == null) return;
    Collection<String> types = connection.getMetadata().getObjectTypes();
    for (String type : types)
    {
      ObjectTreeNode node = new ObjectTreeNode(type, "type");
      node.setAllowsChildren(true);
      schemaNode.add(node);
    }
    model.nodeStructureChanged(schemaNode);
    schemaNode.setChildrenLoaded(true);
  }

  public void loadSchemaObjects(ObjectTreeNode typeNode)
    throws SQLException
  {
    if (typeNode == null) return;


    ObjectTreeNode parent = typeNode.getParent();

    List<TableIdentifier> objects = connection.getMetadata().getObjectList(parent.getName(), new String[] { typeNode.getName() });
    for (TableIdentifier tbl : objects)
    {
      ObjectTreeNode node = new ObjectTreeNode(tbl);
      node.setAllowsChildren(true);
      typeNode.add(node);
    }
    model.nodeStructureChanged(typeNode);
    typeNode.setChildrenLoaded(true);
  }

  public void loadTableColumns(ObjectTreeNode tableNode)
    throws SQLException
  {
    if (tableNode == null) return;
    DbObject dbo = tableNode.getDbObject();
    if (dbo == null)
    {
      tableNode.setAllowsChildren(false);
      return;
    }

    DbMetadata meta = connection.getMetadata();
    if (!meta.isTableType(dbo.getObjectType())) return;

    TableIdentifier tbl = (TableIdentifier)dbo;
    List<ColumnIdentifier> columns = meta.getTableColumns(tbl);
    for (ColumnIdentifier col : columns)
    {
      ObjectTreeNode node = new ObjectTreeNode(col);
      node.setAllowsChildren(false);
      tableNode.add(node);
    }
    model.nodeStructureChanged(tableNode);
    tableNode.setChildrenLoaded(true);
  }


  public WbConnection getConnection()
  {
    return connection;
  }

  public void loadChildren(ObjectTreeNode node)
    throws SQLException
  {
    if (node == null) return;
    DbObject dbo = node.getDbObject();
    DbMetadata meta = connection.getMetadata();
    try
    {
      this.connection.setBusy(true);
      if ("schema".equals(node.getType()))
      {
        loadTypes(node);
      }
      else if ("type".equals(node.getType()))
      {
        loadSchemaObjects(node);
      }
      else if (dbo != null)
      {
        if (meta.isTableType(dbo.getObjectType()))
        {
          loadTableColumns(node);
        }
      }
      else
      {
        node.setAllowsChildren(false);
      }
    }
    finally
    {
      this.connection.setBusy(false);
    }
  }
}
