/*
 * DeleteTablesAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.TableDeleterUI;
import workbench.interfaces.TableDeleteListener;

/**
 * @author Thomas Kellerer
 */
public class DeleteTablesAction
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;
	private ListSelectionModel selection;
	private TableDeleteListener deleteListener;

	public DeleteTablesAction(DbObjectList client, ListSelectionModel list, TableDeleteListener l)
	{
		super();
		this.initMenuDefinition("MnuTxtDeleteTableData");
		this.source = client;
		this.selection = list;
		this.deleteListener = l;
		setEnabled(false);
		list.addListSelectionListener(this);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

		List<TableIdentifier> tables = getSelectedTables();

		TableDeleterUI deleter = new TableDeleterUI();
		deleter.addDeleteListener(this.deleteListener);
		deleter.setObjects(tables);
		deleter.setConnection(source.getConnection());
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(source.getComponent());
		deleter.showDialog(f);
	}

	private List<TableIdentifier> getSelectedTables()
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.isEmpty()) return null;

		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(objects.size());
		for (DbObject dbo : objects)
		{
			if (dbo instanceof TableIdentifier)
			{
				String type = dbo.getObjectType();
				if (!"table".equalsIgnoreCase(type) && !"view".equalsIgnoreCase(type)) continue;
				tables.add((TableIdentifier)dbo);
			}
		}
		return tables;
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		WbConnection conn = this.source.getConnection();
		if (conn == null || conn.isSessionReadOnly())
		{
			setEnabled(false);
		}
		else
		{
			setEnabled(this.selection.getMinSelectionIndex() >= 0);
		}
	}

}
