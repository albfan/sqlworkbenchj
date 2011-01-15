/*
 * ClearCompletionCacheAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
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
 * Action to clear the cache for code completion
 *
 * @see workbench.db.DbObjectCache
 * @author  Thomas Kellerer
 */
public class ClearCompletionCacheAction
	extends WbAction
{
	private WbConnection dbConnection;
	
	public ClearCompletionCacheAction()
	{
		super();
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
