/*
 * ProfileTreeCellRenderer.java
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

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import workbench.gui.components.DividerBorder;

/**
 * A tree cell renderer that can indicate a drop target 
 * @author support@sql-workbench.net
 */
public class MacroTreeCellRenderer
	extends DefaultTreeCellRenderer
{
	private MacroTreeNode dropTarget;
	private DragType type = DragType.none;
	private Border reorderBorder = null;
	private Border moveToGroupBorder = null;
	
	public MacroTreeCellRenderer()
	{
		super();
		reorderBorder = new DividerBorder(DividerBorder.TOP, 1);
		moveToGroupBorder = new LineBorder(Color.GRAY, 1);
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
			setBorder(null);
		}
		return result;
	}
}
