/*
 * DeleteRowAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author  Thomas Kellerer
 */
public class DeleteRowAction
	extends WbAction
{
	private DbData client;

	public DeleteRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.setEnabled(false);
		this.initMenuDefinition("MnuTxtDeleteRow");
		this.setPngIcon("delete");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.deleteRow();
	}

	public void setClient(DbData db)
	{
		this.client = db;
	}
}
