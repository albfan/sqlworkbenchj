/*
 * CopyRowAction.java
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
 * Action to create a copy of the currently selected row in a table.
 * @see workbench.interfaces.DbData
 * @author  support@sql-workbench.net
 */
public class CopyRowAction extends WbAction
{
	private DbData client;

	public CopyRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyRow");
		this.setIcon("CopyRow");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.duplicateRow();
	}
	
	public void setClient(DbData db)
	{
		this.client = db;
	}	
	
}
