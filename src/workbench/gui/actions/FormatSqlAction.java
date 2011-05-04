/*
 * FormatSqlAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.FormattableSql;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	Reformat the currently selected SQL statement
 *
 *	@author  Thomas Kellerer
 */
public class FormatSqlAction extends WbAction
{
	private FormattableSql client;

	public FormatSqlAction(FormattableSql aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtReformatSql",KeyStroke.getKeyStroke(KeyEvent.VK_R, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("format");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateToolbarSeparator(true);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.client.reformatSql();
	}
}
