/*
 * DeleteRowAction.java
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

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author  support@sql-workbench.net
 */
public class DeleteRowAction 
	extends WbAction
{
	private DbData client;

	public DeleteRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.setEnabled(false);
		this.initMenuDefinition("MnuTxtDeleteRow");
		this.setIcon(ResourceMgr.getImage("Delete"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.deleteRow();
	}
	
	public void setClient(DbData db)
	{
		this.client = db;
	}	
}
