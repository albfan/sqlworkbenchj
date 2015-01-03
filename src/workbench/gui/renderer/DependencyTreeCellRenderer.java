/*
 * DependencyTreeCellRenderer.java
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
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import workbench.db.DependencyNode;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class DependencyTreeCellRenderer
	extends JLabel
	implements TreeCellRenderer
{
	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;
	private ImageIcon fk;
	private ImageIcon table;
	private boolean isSelected;

	public DependencyTreeCellRenderer()
	{
		super();
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setVerticalAlignment(SwingConstants.TOP);
		this.setHorizontalAlignment(SwingConstants.LEFT);
		this.fk = ResourceMgr.getPicture("key");
		this.table = ResourceMgr.getPicture("table");
		this.selectedForeground = UIManager.getColor("Tree.selectionForeground");
		this.selectedBackground = UIManager.getColor("Tree.selectionBackground");
		this.unselectedForeground = UIManager.getColor("Tree.textForeground");
		this.unselectedBackground = UIManager.getColor("Tree.textBackground");
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		this.isSelected = selected;
		if (selected)
		{
			this.setForeground(this.selectedForeground);
		}
		else
		{
			this.setForeground(this.unselectedForeground);
			//this.setBackground(this.unselectedBackground);
		}

		if (value instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object o = node.getUserObject();
			if (o instanceof DependencyNode)
			{
				this.setIcon(table);
				DependencyNode depnode = (DependencyNode)o;
				String uaction = depnode.getUpdateAction();
				String daction = depnode.getDeleteAction();
				if (uaction.length() > 0 || daction.length() > 0)
				{
					StringBuilder tooltip = new StringBuilder(50);
					tooltip.append("<html>");
					boolean needBreak = false;
					if (uaction.length() > 0)
					{
						tooltip.append("ON UPDATE ");
						tooltip.append(uaction);
						needBreak = true;
					}
					if (daction.length() > 0)
					{
						if (needBreak) tooltip.append("<br>");
						tooltip.append("ON DELETE ");
						tooltip.append(daction);
					}
					tooltip.append("</html>");
					setToolTipText(tooltip.toString());
				}
				else
				{
					setToolTipText(null);
				}
			}
			else
			{
				this.setIcon(fk);
				this.setToolTipText(null);
			}
		}
		else
		{
			this.setIcon(null);
		}
		this.setText(value.toString());

		return this;
	}

	@Override
	public void paint(Graphics g)
	{
		Color bColor;

		if(this.isSelected)
		{
			bColor = this.selectedBackground;
		}
		else
		{
			bColor = this.unselectedBackground;
			if(bColor == null)	bColor = getBackground();
		}
		int imageOffset = -1;
		if(bColor != null)
		{

			imageOffset = getLabelStart();
			Color oldColor = g.getColor();
			g.setColor(bColor);
			if(getComponentOrientation().isLeftToRight())
			{
				g.fillRect(imageOffset, 0, getWidth() - 1 - imageOffset,
				getHeight());
			}
			else
			{
				g.fillRect(0, 0, getWidth() - 1 - imageOffset,
				getHeight());
			}
			g.setColor(oldColor);
		}
		super.paint(g);
	}

	private int getLabelStart()
	{
		Icon currentI = getIcon();
		if(currentI != null && getText() != null)
		{
			return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
		}
		return 0;
	}

}
