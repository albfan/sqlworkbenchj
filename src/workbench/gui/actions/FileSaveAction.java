/*
 * FileSaveAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.EditorPanel;

import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Save the current file in the SQL Editor.
 *	@author  Thomas Kellerer
 */
public class FileSaveAction extends WbAction
{
	private EditorPanel client;

	public FileSaveAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileSave", KeyStroke.getKeyStroke(KeyEvent.VK_S, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(this.client.hasFileLoaded());
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveCurrentFile();
	}
}
