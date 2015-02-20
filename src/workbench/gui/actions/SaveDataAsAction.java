/*
 * SaveDataAsAction.java
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

import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;
import workbench.gui.dialogs.export.DataStoreExporter;

import workbench.util.EncodingUtil;

/**
 * Save the content of the ResultSet as an external file.
 *
 * @see workbench.gui.dialogs.export.DataStoreExporter
 * @author  Thomas Kellerer
 */
public class SaveDataAsAction
	extends WbAction
{
	private WbTable client;

	public SaveDataAsAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveDataAs");
		this.setIcon("save-as");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		EncodingUtil.fetchEncodings();
		final DataStoreExporter exporter = new DataStoreExporter(client.getDataStore(), client);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				exporter.saveAs();
			}
		});
	}
}
