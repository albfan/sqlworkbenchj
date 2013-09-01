/*
 * CountTableRowsAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.TableRowCountPanel;

/**
 * @author Thomas Kellerer
 */
public class CountTableRowsAction
	extends WbAction
	implements ListSelectionListener
{
	private JMenuItem menuItem;
	private DbObjectList source;
	private ListSelectionModel selection;

	public CountTableRowsAction(DbObjectList client, ListSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtCountRows");
		this.source = client;
		this.selection = list;
		setVisible(false);
		setEnabled(false);
	}

	public void setVisible(boolean flag)
	{
		if (this.menuItem == null)
		{
			menuItem = getMenuItem();
		}
		menuItem.setVisible(flag);
	}

	public void setConnection(WbConnection conn)
	{
		if (conn != null)
		{
			this.setVisible(true);
			selection.addListSelectionListener(this);
			checkState();
		}
		else
		{
			selection.removeListSelectionListener(this);
			this.setVisible(false);
			this.setEnabled(false);
		}
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		countRows();
	}

	private void countRows()
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection()))
		{
			return;
		}

		List<TableIdentifier> objects = getSelectedObjects();
		if (objects == null || objects.isEmpty())
		{
			return;
		}
		TableRowCountPanel panel = new TableRowCountPanel(objects, source.getConnection());
		panel.showWindow(SwingUtilities.getWindowAncestor(source.getComponent()));
	}

	private List<TableIdentifier> getSelectedObjects()
	{
		List<? extends DbObject> selected = this.source.getSelectedObjects();
		if (selected == null || selected.isEmpty())
		{
			return null;
		}

		DbMetadata meta = source.getConnection().getMetadata();
		Set<String> typesWithData = meta.getObjectsWithData();
		List<TableIdentifier> objects = new ArrayList<TableIdentifier>();

		for (DbObject dbo : selected)
		{
			String type = dbo.getObjectType();
			if (typesWithData.contains(type))
			{
				objects.add((TableIdentifier)dbo);
			}
		}
		return objects;
	}

	private void checkState()
	{
		List<TableIdentifier> selected = getSelectedObjects();
		this.setEnabled(selected != null && selected.size() > 0);
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				checkState();
			}
		});
	}
}
