/*
 * ProfileTreeDragHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
	private TreePath[] draggedProfiles;

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

		// For some reason the TreePaths stored in the TransferableProfileNode
		// are losing their UserObjects, so I'm storing them as a variable
		// as well (as all Drag&Drop processing is done in this class anyway,
		// that should not do any harm.
		draggedProfiles = profileTree.getSelectionPaths();
		TransferableProfileNode transferable = new TransferableProfileNode(draggedProfiles);

		dragSource.startDrag(dge, DragSource.DefaultMoveNoDrop, transferable, this);
		setCurrentDropTargetItem(null);
	}

	// ------------ DragSourceListener ---------------------

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

	// ----------- DropTargetListener implementation -----------------
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
		if (node.getAllowsChildren())
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

		if (!parent.getAllowsChildren() || draggedProfiles == null)
		{
			dtde.rejectDrop();
			dtde.dropComplete(false);
			return;
		}

		try
		{
			DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[draggedProfiles.length];
			for (int i = 0; i < draggedProfiles.length; i++)
			{
				nodes[i] = (DefaultMutableTreeNode)draggedProfiles[i].getLastPathComponent();
			}
			dtde.acceptDrop(dtde.getDropAction());
			profileTree.handleDroppedNodes(nodes, parent, dtde.getDropAction());
			dtde.dropComplete(true);
		}
		catch (Exception e)
		{
			LogMgr.logError("ProfileTreeDragHandler.drop()", "Error when finishing drop", e);
			dtde.rejectDrop();
			dtde.dropComplete(false);
		}
		finally
		{
			draggedProfiles = null;
			setCurrentDropTargetItem(null);
		}
	}

}
