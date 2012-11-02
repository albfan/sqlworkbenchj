/*
 * SetNullAction
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.components.WbTable;
import workbench.gui.sql.LookupValuePicker;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectFkValueAction
	extends WbAction
{
	private WbTable client;

	public SelectFkValueAction(WbTable data)
	{
		initMenuDefinition("MnuTxtSelectFkValue");
		client = data;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		LookupValuePicker.openPicker(client);
	}

	@Override
	public boolean allowDuplicate()
	{
		return true;
	}

}
