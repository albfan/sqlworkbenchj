/*
 * ProfileTreeDragHandler.java
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
package workbench.gui.profiles;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.log.LogMgr;



/**
 * Handle drag and drop in the profile Tree
 * @author Thomas Kellerer
 */
class ProfileTreeDragHandler
	implements DragSourceListener, DragGestureListener, DropTargetListener
{
	private DragSource dragSource;
	private ProfileTree profileTree;

	ProfileTreeDragHandler(ProfileTree tree, int actions)
	{
		profileTree = tree;
		dragSource = new DragSource();
		new DropTarget(profileTree, this);
		dragSource.createDefaultDragGestureRecognizer(profileTree, actions, this);
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge)
	{
		if (!profileTree.onlyProfilesSelected()) return;

    TreePath[] draggedProfiles = profileTree.getSelectionPaths();
		TransferableProfileNode transferable = new TransferableProfileNode(draggedProfiles);
		dragSource.startDrag(dge, DragSource.DefaultMoveNoDrop, transferable, this);
		setCurrentDropTargetItem(null);
	}

	private void handleDragSourceEvent(DragSourceDragEvent dsde)
	{
		int action = dsde.getDropAction();
		if (action == DnDConstants.ACTION_COPY)
		{
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
		}
		else
		{
			if (action == DnDConstants.ACTION_MOVE)
			{
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
			}
			else
			{
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			}
		}
	}

	@Override
	public void dragEnter(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	@Override
	public void dragExit(DragSourceEvent dse)
	{
		dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde)
	{
		setCurrentDropTargetItem(null);
	}

	private void handleDragTargetEvent(DropTargetDragEvent dtde)
	{
		Point p = dtde.getLocation();
		profileTree.autoscroll(p);

		TreePath path = profileTree.getClosestPathForLocation(p.x, p.y);

		if (path == null)
		{
			dtde.rejectDrag();
			setCurrentDropTargetItem(null);
			return;
		}

		TreeNode node = (TreeNode)path.getLastPathComponent();
    if (dtde.getSource() != profileTree && dtde.getDropAction() != DnDConstants.ACTION_COPY)
    {
      dtde.rejectDrag();
      setCurrentDropTargetItem(null);
    }
    else if (node.getAllowsChildren())
		{
			dtde.acceptDrag(dtde.getDropAction());
			setCurrentDropTargetItem(node);
		}
		else
		{
			dtde.rejectDrag();
			setCurrentDropTargetItem(null);
		}
	}

	private void setCurrentDropTargetItem(Object item)
	{
		ProfileTreeCellRenderer rend = (ProfileTreeCellRenderer)profileTree.getCellRenderer();
		rend.setDropTargetItem(item);
		profileTree.repaint();
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde)
	{
		handleDragTargetEvent(dtde);
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde)
	{
		handleDragTargetEvent(dtde);
	}

	@Override
	public void dragExit(DropTargetEvent dte)
	{
		setCurrentDropTargetItem(null);
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde)
	{
	}

	@Override
	public void drop(DropTargetDropEvent dtde)
	{
		Point pt = dtde.getLocation();

		TreePath parentpath = profileTree.getClosestPathForLocation(pt.x, pt.y);
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentpath.getLastPathComponent();

    Transferable transferable = dtde.getTransferable();
    if (!parent.getAllowsChildren() || !transferable.isDataFlavorSupported(TransferableProfileNode.PROFILE_FLAVOR))
		{
			dtde.rejectDrop();
			dtde.dropComplete(false);
			return;
		}

		try
		{
      TreePath[] paths = (TreePath[])transferable.getTransferData(TransferableProfileNode.PROFILE_FLAVOR);

      if (paths != null)
      {
        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[paths.length];
        for (int i = 0; i < paths.length; i++)
        {
          nodes[i] = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
        }
        dtde.acceptDrop(dtde.getDropAction());
        if (dtde.getSource() == profileTree)
        {
          profileTree.handleDroppedNodes(nodes, parent, dtde.getDropAction());
        }
        else
        {
          profileTree.handleCopiedNodes(nodes, parent);
        }
        dtde.dropComplete(true);
      }
		}
		catch (Exception e)
		{
			LogMgr.logError("ProfileTreeDragHandler.drop()", "Error when finishing drop", e);
			dtde.rejectDrop();
			dtde.dropComplete(false);
		}
		finally
		{
			setCurrentDropTargetItem(null);
		}
	}

}
