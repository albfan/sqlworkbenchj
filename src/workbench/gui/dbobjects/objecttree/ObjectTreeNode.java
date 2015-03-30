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

import java.io.Serializable;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.IndexDefinition;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerLevel;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectTreeNode
  extends DefaultMutableTreeNode
  implements Serializable
{
  private Set<String> typesWithChildren = CollectionUtil.caseInsensitiveSet(
    "database", "catalog", "schema", "table", "view", "materialized view", "type", "package", "enum");
  private String nodeType;
  private String nodeName;
  private boolean isLoaded;

  public ObjectTreeNode(DbObject dbo)
  {
    super(dbo);
    nodeType = dbo.getObjectType();
    nodeName = dbo.getObjectName();
    allowsChildren = false;
  }

  public ObjectTreeNode(String name, String type)
  {
    super();
    nodeType = type;
    nodeName = name;
  }

  public void setNameAndType(String name, String type)
  {
    nodeType = type;
    nodeName = name;
  }

  public void setChildrenLoaded(boolean flag)
  {
    isLoaded = flag;
  }

  public boolean isLoaded()
  {
    return isLoaded;
  }

  @Override
  public void removeAllChildren()
  {
    super.removeAllChildren();
    isLoaded = false;
  }

  public boolean canHaveChildren()
  {
    if (getType() == null) return false;
    return typesWithChildren.contains(getType());
  }

  @Override
  public ObjectTreeNode getParent()
  {
    return (ObjectTreeNode)super.getParent();
  }

  @Override
  public boolean isLeaf()
  {
    return !allowsChildren;
  }

  public DbObject getDbObject()
  {
    return (DbObject)getUserObject();
  }

  public String getType()
  {
    if (getDbObject() == null)
    {
      return nodeType;
    }
    return getDbObject().getObjectType();
  }

  public String getName()
  {
    DbObject db = getDbObject();
    if (db == null)
    {
      return nodeName;
    }
    return db.getObjectName();
  }

  @Override
  public String toString()
  {
    DbObject dbo = getDbObject();
    if (dbo == null)
    {
      return nodeName;
    }
    if (dbo instanceof ColumnIdentifier)
    {
      ColumnIdentifier col = (ColumnIdentifier)dbo;
      return "<html>" + col.getColumnName() + " - <tt>" + col.getDbmsType() + "</tt></html>";
    }
    if (dbo instanceof IndexDefinition)
    {
      IndexDefinition idx = (IndexDefinition)dbo;
      if (idx.isPrimaryKeyIndex())
      {
        return idx.getName() + " (PK)";
      }
      if (idx.isUnique())
      {
        return idx.getName() + " (UNIQUE)";
      }
    }
    return dbo.getObjectName();
  }

  public String getTooltip()
  {
    DbObject dbo = getDbObject();
    if (dbo == null)
    {
      return null;
    }
    if (dbo instanceof TriggerDefinition)
    {
      TriggerDefinition trg = (TriggerDefinition)dbo;
      String tip = trg.getTriggerType() + " " + trg.getTriggerEvent() + " ON " + trg.getRelatedTable().getTableName();
      TriggerLevel level = trg.getLevel();
      if (level == TriggerLevel.row)
      {
        tip += " FOR EACH ROW";
      }
      else if (level == TriggerLevel.statement)
      {
        tip += " FOR EACH STATEMENT";
      }
      return tip;
    }
    String remarks = dbo.getComment();
    if (StringUtil.isEmptyString(remarks)) return null;
    return remarks;
  }

}
