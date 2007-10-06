/*
 * DeleteRowAction.java
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

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 * Delete the currently highlighted row(s) from a table
 * @see workbench.interfaces.DbData
 * @see workbench.gui.sql.DwPanel
 * @author  support@sql-workbench.net
 */
public class DeleteDependentRowsAction extends WbAction
{
	private DbData client;

	public DeleteDependentRowsAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.setEnabled(false);
		this.initMenuDefinition("MnuTxtDelDependentRows");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.deleteRowWithDependencies();
	}
	
	public void setClient(DbData db)
	{
		this.client = db;
	}	
}
