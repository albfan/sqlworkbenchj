/*
 * MacroTreeCellRenderer.java
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
package workbench.gui.macros;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

import workbench.resource.IconMgr;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;


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
		setLeafIcon(IconMgr.getInstance().getLabelIcon("macro"));
		setOpenIcon(IconMgr.getInstance().getLabelIcon("folder-open"));
		setClosedIcon(IconMgr.getInstance().getLabelIcon("folder"));
		moveToGroupBorder = new CompoundBorder(new LineBorder(Color.DARK_GRAY, 1), new EmptyBorder(1, 1, 1, 1));
	}

	public void setDragType(DragType dragType, MacroTreeNode targetItem)
	{
		type = dragType;
		dropTarget = targetItem;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean isLeaf, int row, boolean hasFocus)
	{
		Component result = super.getTreeCellRendererComponent(tree, value, isSelected, expanded, isLeaf, row, hasFocus);
		if (value instanceof MacroTreeNode)
		{
			MacroTreeNode node = (MacroTreeNode)value;
      Object dataObject = node.getDataObject();

			if (dataObject instanceof MacroDefinition)
			{
				MacroDefinition macro = (MacroDefinition)dataObject;
				if (macro.getExpandWhileTyping())
				{
					setIcon(IconMgr.getInstance().getLabelIcon("macro_expand"));
				}
				else
				{
					setIcon(IconMgr.getInstance().getLabelIcon("macro"));
				}
        setToolTipText(macro.getDisplayTooltip());
			}
      else if (dataObject instanceof MacroGroup)
			{
				setToolTipText(((MacroGroup)dataObject).getTooltip());
			}
		}

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
