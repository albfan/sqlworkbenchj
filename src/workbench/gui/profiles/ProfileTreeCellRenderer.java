/*
 * ProfileTreeCellRenderer.java
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
package workbench.gui.profiles;


import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Method;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import workbench.resource.IconMgr;

import workbench.gui.lnf.LnFHelper;

/**
 * A tree cell renderer that can indicate a drop target.
 *
 * @author Thomas Kellerer
 */
public class ProfileTreeCellRenderer
	extends DefaultTreeCellRenderer
{
  private TreeCellRenderer delegate;
	private Object dropTargetItem;
	private Border dropBorder;
	private Border defaultBorder;

	public ProfileTreeCellRenderer(TreeCellRenderer original)
	{
		super();
    boolean iconsSet = false;

    if (LnFHelper.isNonStandardLookAndFeel())
    {
      delegate = original;
      iconsSet = setupDelegateIcons();
    }
    else
    {
      defaultBorder = super.getBorder();
    }

    Color c = UIManager.getDefaults().getColor("Tree.textForeground");
		dropBorder = new LineBorder(c, 1);

    if (!iconsSet)
    {
      setLeafIcon(IconMgr.getInstance().getLabelIcon("profile"));
      setOpenIcon(IconMgr.getInstance().getLabelIcon("folder-open"));
      setClosedIcon(IconMgr.getInstance().getLabelIcon("folder"));
    }
	}

	public void setDropTargetItem(Object target)
	{
		this.dropTargetItem = target;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
    JComponent result = null;
    if (delegate != null)
    {
      result = (JComponent)delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
      if (defaultBorder == null)
      {
        defaultBorder = result.getBorder();
      }
    }
    else
    {
      result = (JComponent)super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }

		if (this.dropTargetItem != null && dropTargetItem == value)
		{
			result.setBorder(dropBorder);
		}
		else
		{
			result.setBorder(defaultBorder);
		}

		return result;
	}

  private boolean setupDelegateIcons()
  {
    if (delegate == null) return false;

    boolean isSet = false;
    isSet = setIcon("setLeafIcon", IconMgr.getInstance().getLabelIcon("profile"));
    isSet = isSet && setIcon("setOpenIcon", IconMgr.getInstance().getLabelIcon("folder-open"));
    isSet = isSet && setIcon("setClosedIcon", IconMgr.getInstance().getLabelIcon("folder"));
    return isSet;
  }

  private boolean setIcon(String setter, Icon toSet)
  {
    try
    {
      Method setIcon = delegate.getClass().getMethod(setter, Icon.class);
      if (setIcon != null)
      {
        setIcon.invoke(delegate, toSet);
      }
      return true;
    }
    catch (Throwable th)
    {
    }
    return false;
  }

}
