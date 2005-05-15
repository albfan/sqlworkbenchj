/*
 * CopySelectedAsTextAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class CopySelectedAsTextAction extends WbAction
{
	private Exporter client;

	public CopySelectedAsTextAction(Exporter aClient)
	{
		super();
		this.client = aClient;

		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		String desc = ResourceMgr.getDescription("MnuTxtCopySelectedAsText");
		String shift = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
		desc = StringUtil.replace(desc, "%shift%", shift);

		this.initMenuDefinition(ResourceMgr.getString("MnuTxtCopySelectedAsText"), desc, null);
		this.setIcon(null);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		boolean ctrlPressed = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
		ctrlPressed = ctrlPressed && ((e.getModifiers() & ActionEvent.MOUSE_EVENT_MASK) == ActionEvent.MOUSE_EVENT_MASK);
		client.copyDataToClipboard(!shiftPressed, true, ctrlPressed);
	}
}
