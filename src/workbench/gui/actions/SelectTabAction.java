/*
 * SelectTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

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
		this.initName();
	}

	private void initName()
	{
		switch (this.index)
		{
			case 0:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, PlatformShortcuts.getDefaultModifier()));
				break;
			case 1:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, PlatformShortcuts.getDefaultModifier()));
				break;
			case 2:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, PlatformShortcuts.getDefaultModifier()));
				break;
			case 3:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, PlatformShortcuts.getDefaultModifier()));
				break;
			case 4:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, PlatformShortcuts.getDefaultModifier()));
				break;
			case 5:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, PlatformShortcuts.getDefaultModifier()));
				break;
			case 6:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, PlatformShortcuts.getDefaultModifier()));
				break;
			case 7:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, PlatformShortcuts.getDefaultModifier()));
				break;
			case 8:
				this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, PlatformShortcuts.getDefaultModifier()));
				break;
			default:
				this.setAccelerator(null);
		}
		this.setActionName("SelectTab" + (this.index+1));
		this.setMenuText(ResourceMgr.getDefaultTabLabel());// + " &" + Integer.toString(this.index+1));
		this.setIcon(null);
	}

	public int getIndex()
	{
		return this.index;
	}

	public void setNewIndex(int anIndex)
	{
		this.index = anIndex;
		this.initName();
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
