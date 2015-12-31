/*
 * SaveListFileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.IconMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SaveListFileAction
	extends WbAction
{
	private FileActions client;

	public SaveListFileAction(FileActions aClient)
	{
		this(aClient, "LblSaveProfiles");
	}

	public SaveListFileAction(FileActions aClient, String labelKey)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey(labelKey);
		this.setIcon(IconMgr.IMG_SAVE);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.saveItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}
