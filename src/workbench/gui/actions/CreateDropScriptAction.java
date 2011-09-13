/*
 * CreateDummySqlAction.java
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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.WbManager;
import workbench.db.DbObject;
import workbench.db.DropScriptGenerator;
import workbench.db.TableIdentifier;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;
import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class CreateDropScriptAction
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;

	public CreateDropScriptAction(DbObjectList client, ListSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtGenerateDrop");
		this.source = client;
		setEnabled(false);
		list.addListSelectionListener(this);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(objects.size());
		for (DbObject dbo : objects)
		{
			if (dbo instanceof TableIdentifier)
			{
				tables.add((TableIdentifier)dbo);
			}
		}
		DropScriptGenerator generator = new DropScriptGenerator(source.getConnection());
		generator.setTables(tables);
		ObjectScripterUI ui = new ObjectScripterUI(generator);
		ui.show(WbManager.getInstance().getCurrentWindow());
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		if (CollectionUtil.isEmpty(objects))
		{
			setEnabled(false);
		}
		else
		{
			for (DbObject dbo : objects)
			{
				if (!(dbo instanceof TableIdentifier))
				{
					setEnabled(false);
					return;
				}
				setEnabled(true);
			}
		}
	}

}
