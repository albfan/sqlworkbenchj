/*
 * MatchBracketAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.EditorPanel;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class MatchBracketAction extends WbAction
{
	private EditorPanel client;

	public MatchBracketAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtMatchBracket", KeyStroke.getKeyStroke(KeyEvent.VK_B, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.matchBracket();
	}

}
