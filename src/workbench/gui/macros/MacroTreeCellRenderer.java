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
	private MacroTreeNode dropTargetItem = null;
	private Border itemBorder = null;
	private Border groupBorder = null;
	
	public MacroTreeCellRenderer()
	{
		super();
		Color c = getBackgroundSelectionColor();
		itemBorder = new DividerBorder(DividerBorder.TOP, 1);
		groupBorder = new LineBorder(Color.GRAY, 1);
	}
	
	public void setDropTargetItem(MacroTreeNode target)
	{
		this.dropTargetItem = target;
	}

	public Component getTreeCellRendererComponent(JTree tree, 
	                                              Object value, 
	                                              boolean sel, 
	                                              boolean expanded, 
	                                              boolean leaf, 
	                                              int row, 
	                                              boolean hasFocus)
	{
		if (this.dropTargetItem != null && dropTargetItem == value)
		{
			if (dropTargetItem.getAllowsChildren())
			{
				setBorder(groupBorder);
			}
			else
			{
				setBorder(itemBorder);
			}
		}
		else
		{
			setBorder(null);
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}

}
