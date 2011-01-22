/*
 * ImportFileAction.java
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

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 * Import a file into the current result set
 *	@author  Thomas Kellerer
 */
public class ImportFileAction extends WbAction
{
	private SqlPanel client;

	public ImportFileAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtImportFile");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.importFile();
	}
}
