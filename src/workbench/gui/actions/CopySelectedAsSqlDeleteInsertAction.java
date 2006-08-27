/*
 * CopySelectedAsSqlDeleteInsertAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.components.ClipBoardCopier;

import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class CopySelectedAsSqlDeleteInsertAction extends WbAction
{
	private WbTable client;

	public CopySelectedAsSqlDeleteInsertAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopySelectedAsSqlDeleteInsert");
		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		this.setEnabled(false);
	}

	public boolean hasCtrlModifier() { return true; }
	public boolean hasShiftModifier() { return true; }
	
	public void executeAction(ActionEvent e)
	{
		boolean ctrlPressed = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
		ctrlPressed = ctrlPressed && ((e.getModifiers() & ActionEvent.MOUSE_EVENT_MASK) == ActionEvent.MOUSE_EVENT_MASK);
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		copier.copyAsSqlDeleteInsert(true, ctrlPressed);
	}

}
