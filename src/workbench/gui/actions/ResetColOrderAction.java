/*
 * ResetColOrderAction.java
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
import workbench.gui.components.ColumnOrderMgr;
import workbench.gui.components.WbTable;

/**
 *
 * @author Thomas Kellerer
 */
public class ResetColOrderAction
	extends WbAction
{
	private WbTable table;

	public ResetColOrderAction(WbTable client)
	{
		super();
		initMenuDefinition("MnuTxtClearColOrder");
		table = client;
	}

	@Override
	public boolean isEnabled()
	{
		return ColumnOrderMgr.getInstance().isOrderSaved(table);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		ColumnOrderMgr.getInstance().resetColumnOrder(table);
	}
	
}
