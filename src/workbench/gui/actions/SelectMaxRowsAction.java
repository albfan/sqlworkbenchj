/*
 * SelectMaxRowsAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.DwStatusBar;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class SelectMaxRowsAction
	extends WbAction
{
	private DwStatusBar client;

	public SelectMaxRowsAction(DwStatusBar aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSelectMaxRows", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		client.selectMaxRowsField();
	}
}
