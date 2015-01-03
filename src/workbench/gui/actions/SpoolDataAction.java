/*
 * SpoolDataAction.java
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

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.interfaces.TextSelectionListener;
import workbench.gui.sql.EditorPanel;

/**
 *	@author  Thomas Kellerer
 */
public class SpoolDataAction
	extends WbAction
	implements TextSelectionListener
{
	private Exporter client;
	private EditorPanel editor;
	private boolean canExport = false;

	public SpoolDataAction(Exporter aClient)
	{
		this(aClient, "MnuTxtSpoolData");
	}
	public SpoolDataAction(Exporter aClient, String msgKey)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition(msgKey);
		this.setIcon("SpoolData");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void canExport(boolean flag)
	{
		this.canExport = flag;
		checkEnabled();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.exportData();
	}

	public void setEditor(EditorPanel ed)
	{
		this.editor = ed;
		this.editor.addSelectionListener(this);
		checkEnabled();
	}

	private void checkEnabled()
	{
		if (this.editor != null)
		{
			this.setEnabled(editor.isTextSelected() && canExport);
		}
		else
		{
			this.setEnabled(false);
		}
	}

	@Override
	public void selectionChanged(int newStart, int newEnd)
	{
		checkEnabled();
	}

}
