/*
 * DependencyNode.java
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
package workbench.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * A node in the dependency tree for a table
 *
 * @see workbench.db.TableDependency
 * @see workbench.db.DeleteScriptGenerator
 * @see workbench.db.importer.TableDependencySorter
 *
 * @author Thomas Kellerer
 */
public class DependencyNode
  implements Serializable
{
  private static final long serialVersionUID = DbObjectCacheFactory.CACHE_VERSION_UID;

  private DependencyNode parentNode;
  private final TableIdentifier table;
  private String updateAction = "";
  private String deleteAction = "";
  private String fkName;
  private String deferrable;

  private int updateActionValue;
  private int deleteActionValue;
  private int deferrableValue;

  private boolean enabled = true;
  private boolean validated = true;

  /**
   * Maps the columns of the base table (this.table) to the matching column
   * of the parent table (parentNode.getTable()).
   *
   * The LinkedHashMap is used to preserve the order of the columns in the FK definition.
   */
  private final Map<String, String> columns = new LinkedHashMap<>();

  private final List<DependencyNode> childTables = new ArrayList<>();

  public DependencyNode(TableIdentifier aTable)
  {
    this.table = aTable.createCopy();
    this.parentNode = null;
  }

  public void addColumnDefinition(String aColumn, String aParentColumn)
  {
    Object currentParent = this.columns.get(aColumn);
    if (currentParent == null)
    {
      this.columns.put(aColumn, aParentColumn);
    }
  }

  public boolean isEnabled()
  {
    return enabled;
  }

  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  public boolean isValidated()
  {
    return validated;
  }

  public void setValidated(boolean validated)
  {
    this.validated = validated;
  }

  public String getDeferrableType()
  {
    return deferrable;
  }

  public void setDeferrableType(String type)
  {
    this.deferrable = type;
  }

  /**
   * Returns the level of this node in the dependency hierarchy.
   * @return 0 if no parent is available (i.e. the root of the tree)
   *         -1 if this is a self referencing dependency
   */
  public int getLevel()
  {
    if (parentNode == null) return 0;
    if (parentNode == this) return -1;
    return 1 + parentNode.getLevel();
  }

  public void setParent(DependencyNode aParent, String aFkName)
  {
    if (aFkName == null) throw new NullPointerException("FK Name may not be null");
    this.parentNode = aParent;
    this.fkName = aFkName;
  }

  @Override
  public String toString()
  {
    String result = this.table.getTableName();
    if (this.fkName != null)
    {
      result += " (" + this.fkName + ")";
    }
    return result;
  }

  public String debugString()
  {

    if (parentNode == null)
    {
      return "root node for table " + this.table.getTableName();
    }

    StringBuilder result = new StringBuilder(20);
    if (fkName != null)
    {
      result.append("foreign key " + this.fkName + ", ");
    }

    result.append(this.table.getTableName());
    if (columns.size() > 0)
    {
      String cols = StringUtil.listToString(columns.keySet(), ',');
      result.append('(');
      result.append(cols);
      result.append(')');
    }

    if (parentNode != null)
    {
      result.append(" references ");
      result.append(parentNode.table.getTableName());
      result.append('(');
    }
    else if (columns.size() > 0)
    {
      result.append(" --> (");
    }
    boolean first = true;
    for (String col : columns.keySet())
    {
      if (!first)
      {
        result.append(',');
      }
      result.append(columns.get(col));
      first = false;
    }
    if (columns.size() > 0)
    {
      result.append(')');
    }
    result.append(", level:");
    result.append(getLevel());
    return result.toString();
  }

  public List<String> getSourceColumns()
  {
    return new ArrayList<>(columns.keySet());
  }

  public String getSourceColumnsList()
  {
    if (CollectionUtil.isEmpty(columns)) return "";
    return StringUtil.listToString(columns.keySet(), ',');
  }

  public List<String> getTargetColumns()
  {
    List<String> result = new ArrayList<>(columns.size());
    for (String col : columns.keySet())
    {
      result.add(columns.get(col));
    }
    return result;
  }

  public String getTargetColumnsList()
  {
    if (CollectionUtil.isEmpty(columns)) return "";
    StringBuilder result = new StringBuilder(columns.size() * 15);
    boolean first = true;
    for (String col : columns.keySet())
    {
      if (!first)
      {
        result.append(',');
      }
      result.append(columns.get(col));
      first = false;
    }
    return result.toString();
  }

  public String getFkName()
  {
    return this.fkName;
  }

  public TableIdentifier getParentTable()
  {
    if (parentNode == null) return null;
    return this.parentNode.getTable();
  }

  public TableIdentifier getTable()
  {
    return this.table;
  }

  /**
   * Returns a Map that maps the columns of the base table to the matching column
   * of the related (parent/child) table.
   *
   * The keys to the map are columns from this node's table {@link #getTable()}
   * The values in this map are columns found in this node's "parent" table
   *
   * @see #getTable()
   * @see #getParentTable()
   */
  public Map<String, String> getColumns()
  {
    if (this.columns == null)
    {
      return Collections.emptyMap();
    }
    else
    {
      return Collections.unmodifiableMap(this.columns);
    }
  }

  /**
   *  Checks if this node defines the foreign key constraint name aFkname
   *  to the given table
   */
  public boolean isDefinitionFor(TableIdentifier tbl, String aFkname)
  {
    if (aFkname == null) return false;
    return this.table.equals(tbl) && aFkname.equals(this.fkName);
  }

  @Override
  public boolean equals(Object other)
  {
    if (other instanceof DependencyNode)
    {
      DependencyNode node = (DependencyNode) other;
      return this.isDefinitionFor(node.getTable(), node.getFkName());
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    StringBuilder sb = new StringBuilder(60);
    sb.append(this.table.getTableExpression() + "-" + this.fkName);
    return StringUtil.hashCode(sb);
  }

  public boolean isRoot()
  {
    return this.parentNode == null;
  }

  public DependencyNode getParent()
  {
    return this.parentNode;
  }

  public List<DependencyNode> getChildren()
  {
    return this.childTables;
  }

  /**
   * Recursively finds a DependencyNode in the tree of nodes
   */
  public DependencyNode findNode(DependencyNode toFind)
  {
    if (toFind == null) return null;
    if (toFind.equals(this)) return this;
    for (DependencyNode node : childTables)
    {
      if (toFind.equals(node))
      {
        return node;
      }
      else
      {
        DependencyNode n = node.findNode(toFind);
        if (n != null) return n;
      }
    }
    return null;
  }

  public DependencyNode addChild(TableIdentifier table, String aFkname)
  {
    if (aFkname == null) throw new NullPointerException("FK Name may not be null");
    for (DependencyNode node : childTables)
    {
      if (node.isDefinitionFor(table, aFkname))
      {
        return node;
      }
    }
    DependencyNode node = new DependencyNode(table);
    node.setParent(this, aFkname);
    this.childTables.add(node);
    return node;
  }

  public DependencyNode findChildTree(TableIdentifier table)
  {
    if (table == null) return null;
    if (this.table.equals(table)) return this;
    for (DependencyNode node : childTables)
    {
      DependencyNode tree = node.findChildTree(table);
      if (tree != null) return tree;
    }
    return null;
  }

  public boolean containsParentTable(TableIdentifier toCheck)
  {
    if (this.parentNode == null) return false;
    DependencyNode parent = parentNode;
    while (parent != null)
    {
      if (parent.table.equals(toCheck)) return true;
      parent = parent.parentNode;
    }
    return false;
  }

  public boolean containsChildTable(TableIdentifier toCheck)
  {
    for (DependencyNode node : childTables)
    {
      if (node.getTable().equals(toCheck)) return true;
    }
    return false;
  }

  public boolean containsChild(DependencyNode aNode)
  {
    if (aNode == null) return false;
    return this.childTables.contains(aNode);
  }

  public boolean addChild(DependencyNode aTable)
  {
    if (this.containsChild(aTable)) return false;
    this.childTables.add(aTable);
    return true;
  }

  public String getDeleteAction()
  {
    return this.deleteAction;
  }

  public void setDeleteAction(String anAction)
  {
    this.deleteAction = anAction;
  }

  public String getUpdateAction()
  {
    return this.updateAction;
  }

  public void setUpdateAction(String anAction)
  {
    this.updateAction = anAction;
  }

  public void setUpdateActionValue(int action)
  {
    this.updateActionValue = action;
  }

  public int getUpdateActionValue()
  {
    return updateActionValue;
  }

  public int getDeleteActionValue()
  {
    return deleteActionValue;
  }

  public void setDeleteActionValue(int action)
  {
    this.deleteActionValue = action;
  }

  public int getDeferrableValue()
  {
    return deferrableValue;
  }

  public void setDeferrableValue(int deferrable)
  {
    this.deferrableValue = deferrable;
  }

  public void printAll(File debugFile)
  {
    PrintWriter out = null;
    try
    {
      out = new PrintWriter(new FileWriter(debugFile));
      printAll(out);
    }
    catch (IOException io)
    {

    }
    finally
    {
      FileUtil.closeQuietely(out);
    }
  }

  public void printAll(PrintWriter out)
  {
    int level = getLevel();
    StringBuilder indent = new StringBuilder(level * 2);
    for (int i=0; i < level; i++) indent.append("  ");

    out.println(indent + debugString());
    for (DependencyNode node : childTables)
    {
      node.printAll(out);
    }
  }

  public void printParents(PrintWriter out)
  {
    int level = getLevel();
    StringBuilder indent = new StringBuilder(level * 2);
    for (int i=0; i < level; i++) indent.append("  ");

    out.println(indent + debugString());
    DependencyNode parent = parentNode;
    while (parent != null)
    {
      out.print(indent + parent.debugString());
      parent = parent.parentNode;
    }
  }

}
