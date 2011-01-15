/*
 * CopyAsSqlInsertAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.components.ClipBoardCopier;

import workbench.gui.components.WbTable;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Action to copy the contents of a table to the clipboard as SQL INSERT statements
 * @see workbench.gui.components.ClipBoardCopier
 * @author  Thomas Kellerer
 */
public class CopyAsSqlInsertAction extends WbAction
{
	private WbTable client;

	public CopyAsSqlInsertAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyAsSqlInsert",KeyStroke.getKeyStroke(KeyEvent.VK_Q, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public boolean hasCtrlModifier() { return true; }
	public boolean hasShiftModifier() { return true; }

	public void executeAction(ActionEvent e)
	{
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		boolean selectColumns = false;
		if (invokedByMouse(e))
		{
			selectColumns = isCtrlPressed(e) ;
		}
		copier.copyAsSqlInsert(false, selectColumns);
	}

}
