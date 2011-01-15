/*
 * MacroTreeCellRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import workbench.resource.ResourceMgr;

/**
 * A tree cell renderer that can indicate a drop target 
 * @author Thomas Kellerer
 */
public class MacroTreeCellRenderer
	extends DefaultTreeCellRenderer
{
	private MacroTreeNode dropTarget;
	private DragType type = DragType.none;
	private final ReorderBorder reorderBorder = new ReorderBorder();
	private final Border moveToGroupBorder;
	private final Border standardBorder = new EmptyBorder(2, 2, 2, 2);
	
	public MacroTreeCellRenderer()
	{
		super();
		setIconTextGap(5);
		setLeafIcon(ResourceMgr.getPng("macro"));
		setOpenIcon(ResourceMgr.getImage("Tree"));
		setClosedIcon(ResourceMgr.getImage("Tree"));
		moveToGroupBorder = new CompoundBorder(new LineBorder(Color.DARK_GRAY, 1), new EmptyBorder(1, 1, 1, 1));
	}
	
	public void setDragType(DragType dragType, MacroTreeNode targetItem)
	{
		type = dragType;
		dropTarget = targetItem;
	}

	public Component getTreeCellRendererComponent(JTree tree, 
	                                              Object value, 
	                                              boolean sel, 
	                                              boolean expanded, 
	                                              boolean leaf, 
	                                              int row, 
	                                              boolean hasFocus)
	{
		Component result = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (dropTarget == value && type == DragType.moveItems)
		{
			setBorder(moveToGroupBorder);
		}
		else if (dropTarget == value && type == DragType.reorderItems)
		{
			setBorder(reorderBorder);
		}
		else
		{
			setBorder(standardBorder);
		}
		return result;
	}
}
