/*
 * ClearStatementHistoryAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.sql.SqlHistory;
import workbench.resource.ResourceMgr;

/**
 * Action to remove all entries from the SQL history
 * @see workbench.gui.sql.SqlHistory
 * 
 * @author  Thomas Kellerer
 */
public class ClearStatementHistoryAction extends WbAction
{
	private SqlHistory history;

	public ClearStatementHistoryAction(SqlHistory aHistory)
	{
		super();
		this.history = aHistory;
		this.initMenuDefinition("MnuTxtClearSqlHistory");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(false);
		this.setCreateToolbarSeparator(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.history.clear();
	}
}
