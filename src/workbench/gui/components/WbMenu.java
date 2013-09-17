/*
 * WbMenu.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

import workbench.util.NumberStringCache;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbMenu
	extends JMenu
{
	private String parentMenuId;
	private boolean createSeparator;

	public WbMenu(String aText)
	{
		super(aText);
	}

	public WbMenu(String aText, int index)
	{
		super();
		String title = aText;
		if (index < 10)
		{
			title = "&" + NumberStringCache.getNumberString(index) + " - " + aText;
		}
		setText(title);
	}

	public void setParentMenuId(String id)
	{
		this.parentMenuId = id;
	}

	public String getParentMenuId()
	{
		return this.parentMenuId;
	}

	public void setCreateMenuSeparator(boolean aFlag)
	{
		this.createSeparator = aFlag;
	}

	public boolean getCreateMenuSeparator()
	{
		return this.createSeparator;
	}

	@Override
	public void setText(String aText)
	{
		int pos = aText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = aText.charAt(pos + 1);
			aText = aText.substring(0, pos) + aText.substring(pos + 1);
			this.setMnemonic(mnemonic);
		}
		super.setText(aText);
	}

	@Override
	public void removeAll()
	{
		for (int i = 0; i < this.getItemCount(); i++)
		{
			JMenuItem item = this.getItem(i);
			if (item != null)
			{
				item.removeAll();
			}
		}
		super.removeAll();
	}

	public void dispose()
	{
		WbSwingUtilities.removeAllListeners(this);
		this.itemListener = null;
		this.actionListener = null;
		this.changeListener = null;
		this.removeAll();
	}

	public static void disposeMenu(JPopupMenu menu)
	{
		if (menu == null) return;

		for (Component comp : menu.getComponents())
		{
			if (comp instanceof JMenuItem)
			{
				JMenuItem item = (JMenuItem) comp;
				Action action = item.getAction();
				if (action instanceof WbAction)
				{
					((WbAction) action).dispose();
				}
			}
			else if (comp instanceof Container)
			{
				((Container)comp).removeAll();
			}
		}
		menu.removeAll();
	}
}
