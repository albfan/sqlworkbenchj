/*
 * ProfileTreeCellRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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


import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

import workbench.resource.ResourceMgr;

/**
 * A tree cell renderer that can indicate a drop target.
 *
 * @author Thomas Kellerer
 */
public class ProfileTreeCellRenderer
	extends DefaultTreeCellRenderer
{
	private Object dropTargetItem;
	private Border dropBorder;

	public ProfileTreeCellRenderer()
	{
		super();
		Color c = getBackgroundSelectionColor();
		dropBorder = new LineBorder(c, 1);
		setLeafIcon(ResourceMgr.getGifIcon("profile"));
		setOpenIcon(ResourceMgr.getGifIcon("Tree"));
		setClosedIcon(ResourceMgr.getGifIcon("Tree"));
	}

	public void setDropTargetItem(Object target)
	{
		this.dropTargetItem = target;
	}

	@Override
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
