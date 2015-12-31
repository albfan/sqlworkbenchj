/*
 * DropPKAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Map;
import workbench.db.DbObjectChanger;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.dbobjects.FkDisplayPanel;
import workbench.gui.dbobjects.RunScriptPanel;
import workbench.resource.ResourceMgr;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DropForeignKeyAction
	extends WbAction
{
	private final FkDisplayPanel fkDisplay;

	public DropForeignKeyAction(FkDisplayPanel display)
	{
		super();
		this.initMenuDefinition("MnuTxtDropFK");
		this.fkDisplay = display;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		TableIdentifier tbl = fkDisplay.getCurrentTable();
		if (tbl == null) return;

		WbConnection con = fkDisplay.getConnection();
		if (con == null || con.isBusy()) return;

		Map<TableIdentifier, String> selectedConstraints = fkDisplay.getSelectedForeignKeys();
		if (CollectionUtil.isEmpty(selectedConstraints)) return;

		DbObjectChanger changer = new DbObjectChanger(con);

		String sql = changer.getDropFKScript(selectedConstraints);

		if (StringUtil.isBlank(sql)) return;

		RunScriptPanel panel = new RunScriptPanel(con, sql);

		panel.openWindow(fkDisplay, ResourceMgr.getString("TxtDropConstraint"));

		if (panel.wasRun())
		{
			EventQueue.invokeLater(fkDisplay::reloadTable);
		}
	}

}
