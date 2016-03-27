/*
 * DeleteTablesAction.java
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import workbench.interfaces.TableDeleteListener;
import workbench.interfaces.WbSelectionListener;
import workbench.interfaces.WbSelectionModel;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.TableDeleterUI;

/**
 * @author Thomas Kellerer
 */
public class DeleteTablesAction
	extends WbAction
	implements WbSelectionListener
{
	private DbObjectList source;
	private TableDeleteListener deleteListener;
  private WbSelectionModel selection;

	public DeleteTablesAction(DbObjectList client, WbSelectionModel list, TableDeleteListener l)
	{
		super();
		this.initMenuDefinition("MnuTxtDeleteTableData");
		this.source = client;
		this.deleteListener = l;
    selection = list;
    selectionChanged(list);
		selection.addSelectionListener(this);
	}

  @Override
  public void dispose()
  {
    super.dispose();
    if (selection != null)
    {
      selection.removeSelectionListener(this);
    }
  }

	@Override
	public void executeAction(ActionEvent e)
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

		List<TableIdentifier> tables = getSelectedTables();

    boolean autoCommitChanged = false;
    boolean autoCommit = source.getConnection().getAutoCommit();

    // this is essentially here for the DbTree, because the DbTree sets its own connection
    // to autocommit regardless of the profile to reduce locking when retrieving the data
    // from the database. If the profile was not set to autocommit the dropping of the
    // objects should be done in a transaction.
    if (autoCommit && !source.getConnection().getProfile().getAutocommit())
    {
      source.getConnection().changeAutoCommit(false);
      autoCommitChanged = true;
    }

    try
    {
      TableDeleterUI deleter = new TableDeleterUI();
      deleter.addDeleteListener(this.deleteListener);
      deleter.setObjects(tables);
      deleter.setConnection(source.getConnection());
      JFrame f = (JFrame)SwingUtilities.getWindowAncestor(source.getComponent());
      deleter.showDialog(f);
    }
    finally
    {
      if (autoCommitChanged)
      {
        source.getConnection().changeAutoCommit(autoCommit);
      }
    }
	}

	private List<TableIdentifier> getSelectedTables()
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.isEmpty()) return null;

		List<TableIdentifier> tables = new ArrayList<>(objects.size());
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
	public void selectionChanged(WbSelectionModel model)
	{
		WbConnection conn = this.source.getConnection();
		if (conn == null || conn.isSessionReadOnly())
		{
			setEnabled(false);
		}
		else
		{
      List<TableIdentifier> tables = DbObjectList.Util.getSelectedTableObjects(source);
			setEnabled(tables.size() > 0);
		}
	}

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
