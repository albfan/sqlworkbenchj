/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
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
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import workbench.db.ColumnIdentifier;
import workbench.log.LogMgr;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;

import workbench.gui.sql.SqlPanel;
import workbench.util.CollectionUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class ResultTabDropHandler
	implements DropTargetListener
{
  private SqlPanel sqlPanel;
  private final DropTarget target1;
  private final DropTarget target2;

  public ResultTabDropHandler(SqlPanel panel, JComponent dropTarget, JComponent secondTarget)
  {
    sqlPanel = panel;
    target1 = new DropTarget(dropTarget, DnDConstants.ACTION_COPY, this);
    target2 = new DropTarget(secondTarget, DnDConstants.ACTION_COPY, this);
  }

  public void handleDrop(ObjectTreeTransferable selection)
  {
    if (sqlPanel.isBusy()) return;
    if (selection == null) return;

    ObjectTreeNode[] nodes = selection.getSelectedNodes();
    if (nodes == null) return;

    List<ColumnIdentifier> selectedColumns = getSelectedColumns(nodes);
    TableIdentifier tbl = null;

    if (CollectionUtil.isNonEmpty(selectedColumns))
    {
      // the direct parent of a column is the "Columns" node.
      // the parent of that is the table to which the columns belong
      DbObject dbo = nodes[0].getParent().getParent().getDbObject();
      if (dbo instanceof TableIdentifier)
      {
        tbl = (TableIdentifier)dbo;
      }
    }
    else if (nodes.length == 1)
    {
      // only a single node selected, this must to be a table
      DbObject dbo = nodes[0].getDbObject();
      if (dbo instanceof TableIdentifier)
      {
        tbl = (TableIdentifier)dbo;
      }
    }
    if (tbl != null)
    {
      sqlPanel.showData(tbl, selectedColumns);
    }
  }

  private List<ColumnIdentifier> getSelectedColumns(ObjectTreeNode[] nodes)
  {
    if (nodes == null) return null;
    List<ColumnIdentifier> cols = new ArrayList<>(nodes.length);
    for (ObjectTreeNode node : nodes)
    {
      DbObject dbo = node.getDbObject();
      if (dbo instanceof ColumnIdentifier)
      {
        cols.add((ColumnIdentifier)dbo);
      }
      else
      {
        // if something else than a column was selected, we can't handle that
        return null;
      }
    }
    return cols;
  }

  private boolean canHandleSelection(ObjectTreeNode[] nodes)
  {
    if (nodes == null) return false;

    // we can only handle a single table
    if (nodes.length == 1)
    {
      return nodes[0].getDbObject() instanceof TableIdentifier;
    }

    ObjectTreeNode firstParent = nodes[0].getParent();

    // check if all selected nodes are columns
    // and belong to the same table
    for (ObjectTreeNode node : nodes)
    {
      DbObject dbo = node.getDbObject();
      if (!(dbo instanceof ColumnIdentifier))
      {
        return false;
      }
      if (node.getParent() != firstParent)
      {
        // we can't handle columns from different tables
        return false;
      }
    }
    return true;
  }

	public void dispose()
	{
		if (target1 != null)
		{
			target1.removeDropTargetListener(this);
		}
    if (target2 != null)
    {
			target2.removeDropTargetListener(this);
    }
	}

	@Override
	public void dragEnter(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
    if (sqlPanel.isBusy())
    {
      dropTargetDragEvent.rejectDrag();
      return;
    }

    Transferable tr = dropTargetDragEvent.getTransferable();
    if (tr.isDataFlavorSupported(ObjectTreeTransferable.DATA_FLAVOR))
    {
      try
      {
        ObjectTreeTransferable selection = (ObjectTreeTransferable)tr.getTransferData(ObjectTreeTransferable.DATA_FLAVOR);
        ObjectTreeNode[] nodes = selection.getSelectedNodes();
        if (canHandleSelection(nodes))
        {
          dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY);
          return;
        }
      }
      catch (Exception ex)
      {
        LogMgr.logError("ResultTabDropHandler.dragEnter()", "Error processing drag event", ex);
      }
    }

    dropTargetDragEvent.rejectDrag();
	}

	@Override
	public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
	{
	}

	@Override
	public void dragOver(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

	@Override
	public void drop(java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
	{
		try
		{
			Transferable tr = dropTargetDropEvent.getTransferable();
      if (tr.isDataFlavorSupported(ObjectTreeTransferable.DATA_FLAVOR))
      {
        ObjectTreeTransferable selection = (ObjectTreeTransferable)tr.getTransferData(ObjectTreeTransferable.DATA_FLAVOR);
        handleDrop(selection);
      }
      else
			{
				dropTargetDropEvent.rejectDrop();
			}
		}
    catch (Exception ex)
		{
			LogMgr.logError("ResultTabDropHandler.drop()", "Error processing drop event", ex);
			dropTargetDropEvent.rejectDrop();
		}
	}

	@Override
	public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
	{
	}

}
