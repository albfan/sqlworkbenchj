/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.io.Serializable;

import javax.swing.tree.TreePath;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectTreeDragSource
implements DragSourceListener, DragGestureListener, Serializable
{
  private DragSource source;
  private ObjectTreeTransferable transferable;
  private DbObjectsTree sourceTree;

  public ObjectTreeDragSource(DbObjectsTree tree)
  {
    sourceTree = tree;
    source = new DragSource();
    source.createDefaultDragGestureRecognizer(sourceTree, DnDConstants.ACTION_COPY, this);
  }

  /*
   * Drag Gesture Handler
   */
  @Override
  public void dragGestureRecognized(DragGestureEvent dge)
  {
    TreePath[] selected = sourceTree.getSelectionPaths();
    if (selected == null) return;

    ObjectTreeNode[] nodes = new ObjectTreeNode[selected.length];
    for (int i=0; i < selected.length; i++)
    {
      nodes[i] = (ObjectTreeNode)selected[i].getLastPathComponent();
    }
    transferable = new ObjectTreeTransferable(nodes, sourceTree.getConnection().getId());
    source.startDrag(dge, DragSource.DefaultCopyDrop, transferable, this);
  }

  /*
   * Drag Event Handlers
   */
  @Override
  public void dragEnter(DragSourceDragEvent dsde)
  {
  }

  @Override
  public void dragExit(DragSourceEvent dse)
  {
  }

  @Override
  public void dragOver(DragSourceDragEvent dsde)
  {
  }

  @Override
  public void dropActionChanged(DragSourceDragEvent dsde)
  {
  }

  @Override
  public void dragDropEnd(DragSourceDropEvent dsde)
  {
  }
}
