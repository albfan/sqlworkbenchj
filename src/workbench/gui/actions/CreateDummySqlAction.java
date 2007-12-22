/*
 * CreateDummySqlAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.db.TableIdentifier;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;

/**
 * @author support@sql-workbench.net
 */
public class CreateDummySqlAction 
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;
	private ListSelectionModel selection;
	private String scriptType;
	
	public static CreateDummySqlAction createDummyInsertAction(DbObjectList client, ListSelectionModel list)
	{
		return new CreateDummySqlAction("MnuTxtCreateDummyInsert", client, list, ObjectScripter.TYPE_INSERT);
	}
	
	public static CreateDummySqlAction createDummySelectAction(DbObjectList client, ListSelectionModel list)
	{
		return new CreateDummySqlAction("MnuTxtCreateDefaultSelect", client, list, ObjectScripter.TYPE_SELECT);
	}
	
	private CreateDummySqlAction(String key, DbObjectList client, ListSelectionModel list, String type)
	{
		this.initMenuDefinition(key);
		this.source = client;
		this.selection = list;
		this.scriptType = type;
		setEnabled(false);
		list.addListSelectionListener(this);
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		for (DbObject dbo : objects)
		{
			if (dbo instanceof TableIdentifier)
			{
				TableIdentifier tbl = (TableIdentifier)dbo;
				tbl.setType(scriptType);
			}
		}
		ObjectScripter s = new ObjectScripter(objects, source.getConnection());
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		setEnabled(this.selection.getMinSelectionIndex() >= 0);
	}
	
}
