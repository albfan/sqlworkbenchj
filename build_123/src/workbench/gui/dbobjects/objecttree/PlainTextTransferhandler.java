/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
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

import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import static javax.swing.TransferHandler.*;

/**
 * A TransferHandler that only returns the tree nodes as plain text.
 *
 * @author Thomas Kellerer
 */
public class PlainTextTransferhandler
  extends TransferHandler
{
  private DbObjectsTree tree;

  public PlainTextTransferhandler(DbObjectsTree dbTree)
  {
    tree = dbTree;
  }


  /**
   * Creates a Transferable that only contains plain text.
   *
   * @param  component the component for which to create the transeferrable.
   *                   Currently ignored. The tree provided in the constructor is used
   *
   * @return The representation of the data to be transfered.
   *
   * @see PlainTextTransferable
   * @see ObjectTreeNode#getName()
   */
  @Override
  public Transferable createTransferable(JComponent component)
  {
    TreePath[] paths = tree.getSelectionPaths();

    if (paths == null || paths.length == 0)
    {
      return null;
    }

    StringBuilder result = new StringBuilder();

    for (int i=0; i < paths.length; i++)
    {
      if (i > 0) result.append(", ");

      ObjectTreeNode node = (ObjectTreeNode)paths[i].getLastPathComponent();
      result.append(node.getName());
    }

    return new PlainTextTransferable(result.toString());
  }

  @Override
  public int getSourceActions(JComponent c)
  {
    return COPY;
  }
}
