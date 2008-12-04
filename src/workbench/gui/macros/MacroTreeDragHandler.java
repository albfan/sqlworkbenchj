/*
 * MacroTreeDragHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
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
import javax.swing.tree.TreePath;
import workbench.log.LogMgr;

/**
 * Handle drag and drop in the profile Tree
 * @author support@sql-workbench.net
 */
class MacroTreeDragHandler
	implements DragSourceListener, DragGestureListener, DropTargetListener
{
	private DropTarget dropTarget;
	private DragSource dragSource;
	private DragGestureRecognizer recognizer;
	private MacroTree macroTree;
	private TreePath[] draggedEntries;

	public MacroTreeDragHandler(MacroTree tree, int actions)
	{
		macroTree = tree;
		dragSource = new DragSource();
		dropTarget = new DropTarget(macroTree, this);
		recognizer = dragSource.createDefaultDragGestureRecognizer(macroTree, actions, this);
	}

	public void dragGestureRecognized(DragGestureEvent dge)
	{

		// For some reason the TreePaths stored in the TransferableMacroNode
		// are losing their UserObjects, so I'm storing them as a variable
		// as well (as all Drag&Drop processing is done in this class anyway,
		// that should not do any harm).
		draggedEntries = macroTree.getSelectionPaths();
		TransferableMacroNode transferable = new TransferableMacroNode(draggedEntries);

		dragSource.startDrag(dge, DragSource.DefaultMoveNoDrop, transferable, this);
		setCurrentDropTargetItem(null, DragType.none);
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

	public void dragEnter(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	public void dragExit(DragSourceEvent dse)
	{
		dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}

	public void dragOver(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	public void dropActionChanged(DragSourceDragEvent dsde)
	{
		handleDragSourceEvent(dsde);
	}

	public void dragDropEnd(DragSourceDropEvent dsde)
	{
		setCurrentDropTargetItem(null, DragType.none);
	}

	private void handleDragTargetEvent(DropTargetDragEvent dtde)
	{
		Point p = dtde.getLocation();
		macroTree.autoscroll(p);

		TreePath path = macroTree.getClosestPathForLocation(p.x, p.y);

		if (path == null || draggedEntries == null)
		{
			dtde.rejectDrag();
			setCurrentDropTargetItem(null, DragType.none);
			return;
		}
		
		MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
		
		boolean allowDrop = false;
		if (draggedEntries.length == 1)
		{
			allowDrop = !draggedEntries[0].getLastPathComponent().equals(node);
		}

		DragType type = DragType.none;
		if (allowDrop)
		{
			type = node.getDropType(draggedEntries);
		}

		if (type != DragType.none)
		{
			dtde.acceptDrag(dtde.getDropAction());
			setCurrentDropTargetItem(node, type);
		}
		else
		{
			dtde.rejectDrag();
			setCurrentDropTargetItem(null, DragType.none);
		}
	}

	private void setCurrentDropTargetItem(MacroTreeNode item, DragType type)
	{
		MacroTreeCellRenderer rend = (MacroTreeCellRenderer)macroTree.getCellRenderer();
		rend.setDragType(type, item);
		macroTree.repaint();
	}

	public void dragEnter(DropTargetDragEvent dtde)
	{
		handleDragTargetEvent(dtde);
	}

	public void dragOver(DropTargetDragEvent dtde)
	{
		handleDragTargetEvent(dtde);
	}

	public void dragExit(DropTargetEvent dte)
	{
		setCurrentDropTargetItem(null, DragType.none);
	}

	public void dropActionChanged(DropTargetDragEvent dtde)
	{
	}

	public void drop(DropTargetDropEvent dtde)
	{
		Point pt = dtde.getLocation();

		TreePath parentpath = macroTree.getClosestPathForLocation(pt.x, pt.y);
		MacroTreeNode parent = (MacroTreeNode) parentpath.getLastPathComponent();

		if (draggedEntries == null || (draggedEntries.length > 1 && !parent.getAllowsChildren()) )
		{
			dtde.rejectDrop();
			return;
		}

		try
		{
			MacroTreeNode[] nodes = new MacroTreeNode[draggedEntries.length];
			for (int i = 0; i < draggedEntries.length; i++)
			{
				nodes[i] = (MacroTreeNode)draggedEntries[i].getLastPathComponent();
			}
			dtde.acceptDrop(dtde.getDropAction());
			macroTree.handleDroppedNodes(nodes, parent, dtde.getDropAction());
			dtde.dropComplete(true);
		}
		catch (Exception e)
		{
			LogMgr.logError("MacroTreeDragHandler.drop()", "Error when finishing drop", e);
			dtde.rejectDrop();
		}
		finally
		{
			draggedEntries = null;
			setCurrentDropTargetItem(null, DragType.none);
		}
	}

}
