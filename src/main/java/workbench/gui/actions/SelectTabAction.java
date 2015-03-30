/*
 * SelectTabAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.ShortcutManager;

/**
 *	@author  Thomas Kellerer
 */
public class SelectTabAction
	extends WbAction
{
	private JTabbedPane client;
	private int index;

	public SelectTabAction(JTabbedPane aPane, int anIndex)
	{
		super();
		this.client = aPane;
		this.index = anIndex;
		this.setIcon(null);
		this.setMenuText(ResourceMgr.getDefaultTabLabel());
		this.initAccelerator();
	}

	private void initAccelerator()
	{
		KeyStroke key = getKeyStrokeForIndex(index);

		if (ShortcutManager.getInstance().isKeyStrokeAssigned(key))
		{
			this.setAccelerator(null);
		}
		else
		{
			this.setAccelerator(key);
		}
		this.setActionName("SelectTab" + (this.index+1));
	}

	private KeyStroke getKeyStrokeForIndex(int indexValue)
	{
		if (indexValue >= 0 && indexValue <= 8)
		{
			/** VK_0 thru VK_9 are the same as ASCII '0' thru '9' (0x30 - 0x39) */
			return KeyStroke.getKeyStroke(0x31 + indexValue, PlatformShortcuts.getDefaultModifier());
		}
		return null;
	}

	public int getIndex()
	{
		return this.index;
	}

	public void setNewIndex(int anIndex)
	{
		this.index = anIndex;
		this.initAccelerator();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (client != null)
		{
			try
			{
				int count = client.getTabCount();
				if (count > 0 && index < count && index > -1)
				{
					this.client.setSelectedIndex(this.index);
				}
			}
			catch (Exception ex)
			{
				LogMgr.logError("SelectTabAction.executeAction()", "Error when selecting tab " + this.index, ex);
			}
		}
	}
}
