/*
 * CreatePKAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.DbObjectChanger;
import workbench.db.TableIdentifier;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.RunScriptPanel;
import workbench.resource.ResourceMgr;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CreatePKAction
	extends WbAction
{
	private DbObjectList columns;

	public CreatePKAction(DbObjectList cols)
	{
		super();
		this.columns = cols;
		this.initMenuDefinition("MnuTxtCreatePK");
	}

	public void executeAction(ActionEvent e)
	{
		List<? extends DbObject> selected = columns.getSelectedObjects();
		if (selected == null) return;
		if (selected.size() == 0) return;
		List<ColumnIdentifier> cols = CollectionUtil.arrayList();
		for (DbObject dbo : selected)
		{
			cols.add((ColumnIdentifier)dbo);
		}
		
		TableIdentifier table = columns.getObjectTable();
		
		DbObjectChanger changer = new DbObjectChanger(columns.getConnection());
		String sql = changer.getAddPKScript(table, cols);
		if (StringUtil.isBlank(sql)) return;

		RunScriptPanel panel = new RunScriptPanel(columns.getConnection(), sql);
		
		panel.openWindow(columns.getComponent(), ResourceMgr.getString("TxtCreatePK"));

		if (panel.wasRun() && columns != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					columns.reload();
				}
			});
		}

	}

}
