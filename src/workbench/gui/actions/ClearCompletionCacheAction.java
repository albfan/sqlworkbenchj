/*
 * ClearCompletionCacheAction.java
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
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;

/**
 * @author  support@sql-workbench.net
 */
public class ClearCompletionCacheAction
	extends WbAction
{
	private WbConnection dbConnection;
	
	public ClearCompletionCacheAction()
	{
		this.initMenuDefinition("MnuTxtClearCompletionCache");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void setConnection(WbConnection conn)
	{
		this.dbConnection = conn;
		this.setEnabled(this.dbConnection != null);
	}
	
	public void executeAction(ActionEvent e)
	{
		if (this.dbConnection != null) this.dbConnection.getObjectCache().clear();
	}
}
