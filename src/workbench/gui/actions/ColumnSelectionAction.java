/*
 * ColumnSelectionAction.java
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

import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to enable column selection for the next selection in the editor
 *	@author  Thomas Kellerer
 */
public class ColumnSelectionAction extends WbAction
{
	private EditorPanel client;

	public ColumnSelectionAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtColumnSelection", KeyStroke.getKeyStroke(KeyEvent.VK_Q,KeyEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.setSelectionRectangular(true);
	}


}
