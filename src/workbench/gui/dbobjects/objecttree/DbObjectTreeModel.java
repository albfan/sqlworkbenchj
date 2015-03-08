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

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectTreeModel
  extends DefaultTreeModel
{

  public DbObjectTreeModel(TreeNode root)
  {
    super(root);
  }

  public ObjectTreeNode findNodeByType(String name, String type)
  {
    if (StringUtil.isEmptyString(name) || StringUtil.isEmptyString(type)) return null;
    
    return findNodeByType((ObjectTreeNode)getRoot(), name, type);
  }

  private ObjectTreeNode findNodeByType(ObjectTreeNode node, String name, String type)
  {
    if (node.getName().equalsIgnoreCase(name) && node.getType().equalsIgnoreCase(type)) return node;
    int count = node.getChildCount();
    for (int i=0; i < count; i ++)
    {
      ObjectTreeNode child = (ObjectTreeNode)node.getChildAt(i);
      ObjectTreeNode nd1 = findNodeByType(child, name, type);
      if (nd1 != null) return nd1;
    }
    return null;
  }

}
