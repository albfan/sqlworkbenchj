/*
 * FileReloadAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.resource.ResourceMgr;

/**
 * Reload the currently loaded file in the SQL Editor
 * @author support@sql-workbench.net
 */
public class FileReloadAction 
	extends WbAction
{
	private EditorPanel client;

	public FileReloadAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileReload", KeyStroke.getKeyStroke(KeyEvent.VK_F5,KeyEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setEnabled(aClient.hasFileLoaded());
	}

	public void executeAction(ActionEvent e)
	{
		this.client.reloadFile();
	}
}
