/*
 * ScriptDbObjectAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class ScriptDbObjectAction 
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;
	private ListSelectionModel selection;
	
	public ScriptDbObjectAction(DbObjectList client, ListSelectionModel list)
	{
		this.initMenuDefinition("MnuTxtCreateScript");
		this.source = client;
		this.selection = list;
		setEnabled(false);
		setIcon(ResourceMgr.getImage("script"));
		list.addListSelectionListener(this);
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		if (!WbSwingUtilities.checkConnection(source.getComponent(), source.getConnection())) return;
		
		List<? extends DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.size() == 0) return;

		ObjectScripter s = new ObjectScripter(objects, source.getConnection());
		ObjectScripterUI scripterUI = new ObjectScripterUI(s);
		scripterUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		setEnabled(this.selection.getMinSelectionIndex() >= 0);
	}
	
}
