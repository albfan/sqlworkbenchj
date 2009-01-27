/*
 * UndoAction.java
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

import workbench.interfaces.Undoable;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class UndoAction extends WbAction
{
	private Undoable client;

	public UndoAction(Undoable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtUndo",KeyStroke.getKeyStroke(KeyEvent.VK_Z, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("Undo");
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.undo();
	}
}
