/*
 * ProfileTreeCellRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * A tree cell renderer that can indicate a drop target 
 * @author support@sql-workbench.net
 */
public class ProfileTreeCellRenderer
	extends DefaultTreeCellRenderer
{
	private Object dropTargetItem = null;
	private Border dropBorder = null;
	
	public ProfileTreeCellRenderer()
	{
		super();
		Color c = getBackgroundSelectionColor();
		dropBorder = new LineBorder(c, 1);
	}
	
	public void setDropTargetItem(Object target)
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
			setBorder(dropBorder);
		}
		else
		{
			setBorder(null);
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}

}
