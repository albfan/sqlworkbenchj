/*
 * ManageMacroAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;


import workbench.gui.MainWindow;
import workbench.gui.macros.MacroPopup;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class ShowMacroPopupAction extends WbAction
{
	private MainWindow client;

	public ShowMacroPopupAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtMacroPopup");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			MacroPopup p = new MacroPopup(client);
			p.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowMacroPopupAction", "Error show popup", ex);
		}
	}

}
