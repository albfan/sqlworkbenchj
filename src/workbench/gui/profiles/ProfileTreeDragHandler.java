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

import workbench.db.ConnectionProfile;

/**
 * Handle drag and drop in the profile Tree.
 *
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
		ProfileTreeTransferable transferable = new ProfileTreeTransferable(draggedProfiles, profileTree.getName());
		dragSource.startDrag(dge, null, transferable, this);
		setCurrentDropTargetItem(null);
	}

  // --- DragSourceListener ---
	private void handleDragSourceEvent(DragSourceDragEvent dsde)
	{
		int action = dsde.getDropAction();

    if (action == DnDConstants.ACTION_COPY)
    {
      dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
    }
    else if (action == DnDConstants.ACTION_MOVE)
    {
      dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }
    else
    {
      dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
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
		dse.getDragSourceContext().setCursor(null);
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
    if (dsde.getDropSuccess() && dsde.getDropAction() == DnDConstants.ACTION_MOVE)
    {
      System.out.println("dropEnd on: " + profileTree.getName());
      Transferable transferable = dsde.getDragSourceContext().getTransferable();
      System.out.println("remove profiles from the source");
    }
		setCurrentDropTargetItem(null);
	}

  // --- DropTargetListener ---
	private void handleDragTargetEvent(DropTargetDragEvent dtde)
	{
		Point dropLocation = dtde.getLocation();
		profileTree.autoscroll(dropLocation);

    TransferableProfileNode transferNode = getTransferNode(dtde.getTransferable());
    TreePath path = profileTree.getClosestPathForLocation(dropLocation.x, dropLocation.y);
    if (path == null || transferNode == null)
    {
			dtde.rejectDrag();
			setCurrentDropTargetItem(null);
			return;
    }

		TreeNode node = (TreeNode)path.getLastPathComponent();
    int action = dtde.getDropAction();
    if (node.getAllowsChildren() && (action == DnDConstants.ACTION_COPY || action == DnDConstants.ACTION_MOVE))
		{
      dtde.acceptDrag(action);
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

    TransferableProfileNode transferNode = getTransferNode(dtde.getTransferable());

    if (!parent.getAllowsChildren() || transferNode == null)
		{
			dtde.rejectDrop();
			dtde.dropComplete(false);
			return;
		}

		try
		{
      TreePath[] paths = transferNode.getPath();

      if (paths != null)
      {
        DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[paths.length];
        for (int i = 0; i < paths.length; i++)
        {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
          ConnectionProfile profile = (ConnectionProfile)node.getUserObject();
          DefaultMutableTreeNode realNode = profileTree.getModel().findProfileNode(profile);
          if (realNode == null)
          {
            // should only happen when dragging between two trees
            nodes[i] = node;
          }
          else
          {
            nodes[i] = realNode;
          }
        }

        int action = dtde.getDropAction();
        dtde.acceptDrop(action);

        profileTree.handleDroppedNodes(nodes, parent, action);

//        if (action == DnDConstants.ACTION_MOVE && transferNode.getSource() != null && !isSameTree(transferNode))
//        {
//          ProfileTree source = transferNode.getSource();
//          source.getModel().deleteNodes(nodes);
//        }
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

  private TransferableProfileNode getTransferNode(Transferable transferable)
  {
    if (transferable == null) return null;
    if (!transferable.isDataFlavorSupported(ProfileTreeTransferable.PROFILE_FLAVOR)) return null;

    try
    {
      return (TransferableProfileNode)transferable.getTransferData(ProfileTreeTransferable.PROFILE_FLAVOR);
    }
    catch (Exception ex)
    {
      return null;
    }
  }

//  private boolean isSameTree(TransferableProfileNode transferNode)
//  {
//    if (transferNode == null) return false;
//    ProfileTree source = transferNode.getSource();
//    return source == profileTree;
//  }

}
