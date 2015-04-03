/*
 * DropDbObjectAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import workbench.interfaces.ObjectDropListener;
import workbench.interfaces.ObjectDropper;
import workbench.interfaces.Reloadable;
import workbench.interfaces.WbSelectionListener;
import workbench.interfaces.WbSelectionModel;

import workbench.db.DbObject;
import workbench.db.GenericObjectDropper;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectDropperUI;

/**
 * @author Thomas Kellerer
 */
public class DropDbObjectAction
	extends WbAction
	implements WbSelectionListener
{
	private DbObjectList source;
	private ObjectDropper dropper;
  private ObjectDropListener dropListener;
	private Reloadable data;
	private boolean available = true;

	public DropDbObjectAction(String labelKey, DbObjectList client, ListSelectionModel list, Reloadable r)
	{
    this("MnuTxtDropDbObject", client, WbSelectionModel.Factory.createFacade(list), r);
	}

	public DropDbObjectAction(DbObjectList client, WbSelectionModel list, Reloadable r)
	{
		this("MnuTxtDropDbObject", client, list, r);
	}

	public DropDbObjectAction(String labelKey, DbObjectList client, WbSelectionModel list, Reloadable r)
	{
		super();
		this.initMenuDefinition(labelKey);
		this.source = client;
		this.data = r;
		selectionChanged(list);
		list.addSelectionListener(this);
	}

  public void addDropListener(ObjectDropListener listener)
  {
    dropListener = listener;
  }

	@Override
	public void executeAction(ActionEvent e)
	{
		dropObjects();
	}

	public void setAvailable(boolean flag)
	{
		this.available = flag;
		if (!available) this.setEnabled(false);
	}

	public void setDropper(ObjectDropper dropperToUse)
	{
		this.dropper = dropperToUse;
	}

	private void dropObjects()
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

		final List<DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.isEmpty()) return;

		ObjectDropper dropperToUse = (this.dropper != null ? this.dropper : new GenericObjectDropper());
		dropperToUse.setObjects(objects);
		dropperToUse.setConnection(source.getConnection());
		dropperToUse.setObjectTable(source.getObjectTable());

		ObjectDropperUI dropperUI = new ObjectDropperUI(dropperToUse);

		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(source.getComponent());
		dropperUI.showDialog(f);

		if (!dropperUI.dialogWasCancelled())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (data != null)
          {
            data.reload();
          }
          if (dropListener != null)
          {
            dropListener.objectsDropped(objects);
          }
				}
			});
		}
	}

  @Override
    public void selectionChanged(WbSelectionModel list)
  {
		WbConnection conn = this.source.getConnection();
		if (conn == null || conn.isSessionReadOnly())
		{
			setEnabled(false);
		}
		else
		{
			setEnabled(this.available && source.getSelectionCount() > 0);
		}
  }

}
