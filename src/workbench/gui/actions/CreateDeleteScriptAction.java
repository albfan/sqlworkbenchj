/*
 * CreateDeleteScriptAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.event.ListSelectionListener;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class CreateDeleteScriptAction 
	extends WbAction
	implements ListSelectionListener
{
	private SqlPanel client;

	public CreateDeleteScriptAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCreateDeleteScript", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		
		this.client.getData().getTable().getSelectionModel().addListSelectionListener(this);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.generateDeleteScript();
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		boolean mayUpdate = this.client.getData().hasUpdateableColumns();
		int rows = this.client.getData().getTable().getSelectedRowCount();
		this.setEnabled(mayUpdate && rows > 0);
	}
		

}
