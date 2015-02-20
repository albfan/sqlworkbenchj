/*
 * ScriptDbObjectAction.java
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
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.DbObject;
import workbench.db.ObjectScripter;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;

/**
 * @author Thomas Kellerer
 */
public class ScriptDbObjectAction
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;
	private ListSelectionModel selection;

	public ScriptDbObjectAction(DbObjectList client, ListSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtCreateScript");
		this.source = client;
		this.selection = list;
		setEnabled(false);
		setIcon("script");
		list.addListSelectionListener(this);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

		List<? extends DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.isEmpty()) return;

		ObjectScripter s = new ObjectScripter(objects, source.getConnection());
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.setDbConnection(source.getConnection());
		scripterUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		setEnabled(this.selection.getMinSelectionIndex() >= 0);
	}

}
