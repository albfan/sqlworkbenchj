/*
 * CloseAllResultsAction.java
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
import workbench.resource.ResourceMgr;

/**
 * An action to close all result tabs of a SqlPanel.
 *
 * @author  Thomas Kellerer
 */
public class CloseAllResultsAction
	extends WbAction
{
	private SqlPanel panel;

	public CloseAllResultsAction(SqlPanel sqlPanel)
	{
		super();
		panel = sqlPanel;
		this.initMenuDefinition("MnuTxtCloseAllResults");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(null);
		this.setEnabled(panel.getResultTabCount() > 0);
	}

	public void executeAction(ActionEvent e)
	{
		panel.clearResultTabs();
	}

}
