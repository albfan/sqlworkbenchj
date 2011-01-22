/*
 * SelectResultAction.java
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
 *	Action to select the result display in the SqlPanel
 *	@author  Thomas Kellerer
 */
public class SelectResultAction
	extends WbAction
{
	private SqlPanel client;

	public SelectResultAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSelectResult");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.selectResult();
	}
}
