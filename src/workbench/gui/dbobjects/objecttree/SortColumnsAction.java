/*
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
package workbench.gui.dbobjects.objecttree;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.gui.actions.WbAction;

/**
 * @author Thomas Kellerer
 */
public class SortColumnsAction
  extends WbAction
{
  private ObjectTreeNode columnsNode;
  private DbObjectsTree client;
  private boolean sortByName;

  public SortColumnsAction(DbObjectsTree tree, ObjectTreeNode node, boolean byName)
  {
    super();
    columnsNode = node;
    client = tree;
    sortByName = byName;
    if (byName)
    {
      initMenuDefinition("MnuTxtSortColByName");
    }
    else
    {
      initMenuDefinition("MnuTxtSortColByPos");
    }
    setEnabled(columnsNode != null && columnsNode.getChildCount() > 0);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    sortColumns();
  }

  private void sortColumns()
  {
    if (columnsNode == null) return;
    if (client == null) return;

    int count = columnsNode.getChildCount();
    if (count <= 0) return;

    List<ColumnIdentifier> columns = new ArrayList<>(count);

    for (int i=0; i < count; i++)
    {
      columns.add((ColumnIdentifier)columnsNode.getChildAt(i).getDbObject());
    }

    if (sortByName)
    {
      DbObjectSorter.sort(columns, true);
    }
    else
    {
      ColumnIdentifier.sortByPosition(columns);
    }

    columnsNode.removeAllChildren();

    for (ColumnIdentifier column : columns)
    {
      ObjectTreeNode node = new ObjectTreeNode(column);
      columnsNode.add(node);
    }

    // removeAllChildren() will reset the "loaded" state
    columnsNode.setChildrenLoaded(true);

    client.getModel().nodeStructureChanged(columnsNode);
  }

}
