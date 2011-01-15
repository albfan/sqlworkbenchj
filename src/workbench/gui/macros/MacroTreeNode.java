/*
 * MacroTreeNode.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroTreeNode
	extends DefaultMutableTreeNode
{
	private Object data;

	public MacroTreeNode(Object dataObject)
	{
		super(dataObject);
		setDataObject(dataObject);
	}

	public MacroTreeNode(Object dataObject, boolean allowsChildren)
	{
		super(dataObject, allowsChildren);
		setDataObject(dataObject);
	}

	public Object getDataObject()
	{
		return data;
	}

	public void setDataObject(Object dataObject)
	{
		this.data = dataObject;
	}

	public DragType getDropType(TreePath[] source)
	{
		if (source == null) return DragType.none;

		boolean sourceIsGroup = true;
		boolean sourceBelongsToUs = false;

		for (TreePath path : source)
		{
			MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
			sourceIsGroup = sourceIsGroup && node.getAllowsChildren();
			if (!sourceBelongsToUs && getAllowsChildren() && isNodeChild(node))
			{
				sourceBelongsToUs = true;
			}
		}
		if (sourceBelongsToUs) return DragType.none;
		
		if (getAllowsChildren())
		{
			if (sourceIsGroup) return DragType.reorderItems;
			return DragType.moveItems;
		}
		else if (!sourceIsGroup)
		{
			return DragType.reorderItems;
		}
		else
		{
			return DragType.none;
		}
	}
}
