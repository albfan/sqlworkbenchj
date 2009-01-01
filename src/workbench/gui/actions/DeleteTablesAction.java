/*
 * DeleteTablesAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * @author support@sql-workbench.net
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

	public void executeAction(ActionEvent e)
	{
		if (!WbSwingUtilities.checkConnection(source.getComponent(), source.getConnection())) return;

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
		if (objects == null || objects.size() == 0) return null;

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

	public void valueChanged(ListSelectionEvent e)
	{
		WbConnection conn = this.source.getConnection();
		if (conn == null || conn.getProfile().isReadOnly())
		{
			setEnabled(false);
		}
		else
		{
			setEnabled(this.selection.getMinSelectionIndex() >= 0);
		}
	}

}
