/*
 * DeleteListEntryAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;

/**
 *	Delete an etry from a List
 *	@author  Thomas Kellerer
 */
public class DeleteListEntryAction extends WbAction
{
	private FileActions client;

	public DeleteListEntryAction(FileActions aClient)
	{
		this(aClient, "LblDeleteListEntry");
	}

	public DeleteListEntryAction(FileActions aClient, String aKey)
	{
		super();
		client = aClient;
		isConfigurable = false;
		setMenuTextByKey(aKey);
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
		setIcon("delete");
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		try
		{
			client.deleteItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
