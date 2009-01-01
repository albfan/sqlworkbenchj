/*
 * FileSaveAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.sql.EditorPanel;

import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 * Save the current file in the SQL Editor.
 *	@author  support@sql-workbench.net
 */
public class FileSaveAction extends WbAction
{
	private EditorPanel client;

	public FileSaveAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileSave", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(this.client.hasFileLoaded());
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveCurrentFile();
	}
}
