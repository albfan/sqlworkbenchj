/*
 * CopyRowAction.java
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

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  info@sql-workbench.net
 */
public class CopyRowAction extends WbAction
{
	private DbData client;

	public CopyRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyRow");
		this.setIcon(ResourceMgr.getImage("CopyRow"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.duplicateRow();
	}
}
