/*
 * CountTableRowsAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import workbench.interfaces.WbSelectionListener;
import workbench.interfaces.WbSelectionModel;

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
	implements WbSelectionListener
{
	private JMenuItem menuItem;
	private DbObjectList source;
	private WbSelectionModel selection;

	public CountTableRowsAction(DbObjectList client, WbSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtCountRows");
		this.source = client;
		this.selection = list;
		setVisible(false);
		setEnabled(list.hasSelection());
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
			selection.addSelectionListener(this);
			checkState();
		}
		else
		{
			selection.removeSelectionListener(this);
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
		List<TableIdentifier> objects = new ArrayList<>();

		for (DbObject dbo : selected)
		{
			String type = dbo.getObjectType();
			if (typesWithData.contains(type) && dbo instanceof TableIdentifier)
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
	public void selectionChanged(WbSelectionModel model)
	{
		EventQueue.invokeLater(this::checkState);
	}

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
