/*
 * CopyAsTextAction.java
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
import workbench.gui.components.ClipBoardCopier;
import workbench.gui.components.WbTable;

import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Action to copy the contents of a WbTable as tab-separated text to the clipboard
 * @see workbench.gui.components.ClipBoardCopier
 * @author  Thomas Kellerer
 */
public class CopyAsTextAction
	extends WbAction
{
	private WbTable client;
	protected boolean copySelected;

	public CopyAsTextAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.initMenuDefinition("MnuTxtDataToClipboard", KeyStroke.getKeyStroke(KeyEvent.VK_Y, PlatformShortcuts.getDefaultModifier()));
		copySelected = false;
		this.setEnabled(false);
	}

	public boolean hasCtrlModifier() { return true; }
	public boolean hasShiftModifier() { return true; }

	public void executeAction(ActionEvent e)
	{
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		boolean copyHeaders = true;
		boolean selectColumns = false;
		if (invokedByMouse(e))
		{
			copyHeaders = !isShiftPressed(e);
			selectColumns = isCtrlPressed(e) ;
		}
		copier.copyDataToClipboard(copyHeaders, copySelected, selectColumns);
	}

}
