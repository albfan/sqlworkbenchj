/*
 * CloseOtherTabsAction.java
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
import workbench.gui.sql.SqlPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class CloseOtherResultsAction
	extends WbAction
{
	private SqlPanel client;

	public CloseOtherResultsAction(SqlPanel panel)
	{
		client = panel;
		initMenuDefinition("MnuTxtCloseOtherResults");
		this.setEnabled(client.getResultTabCount() > 2);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		client.closeOtherResults();
	}

}
