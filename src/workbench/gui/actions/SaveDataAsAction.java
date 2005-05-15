/*
 * SaveDataAsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class SaveDataAsAction extends WbAction
{
	private Exporter client;

	public SaveDataAsAction(Exporter aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveDataAs");
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_SAVE_AS));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveAs();
	}
}
