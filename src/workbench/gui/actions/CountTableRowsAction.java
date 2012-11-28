/*
 * CompileDbObjectAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;

import workbench.db.TableIdentifier;
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

		List<TableIdentifier> objects = new ArrayList<TableIdentifier>();
		for (DbObject dbo : selected)
		{
			if (canContainData(dbo))
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

	private boolean canContainData(DbObject dbo)
	{
		if (!(dbo instanceof TableIdentifier)) return false;
		TableIdentifier tbl = (TableIdentifier)dbo;

		String type = dbo.getObjectType();
		DbMetadata meta = source.getConnection().getMetadata();
		DbSettings dbs = source.getConnection().getDbSettings();
		if (meta.supportsSynonyms() && dbs.isSynonymType(type))
		{
			TableIdentifier rt = meta.resolveSynonym(tbl);
			if (rt == null) return false;
			type = rt.getType();
		}
		return meta.objectTypeCanContainData(type);
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
