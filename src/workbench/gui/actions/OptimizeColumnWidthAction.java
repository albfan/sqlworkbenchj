/*
 * OptimizeColumnWidthAction.java
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
import java.awt.event.ActionListener;

import workbench.gui.components.WbTable;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class OptimizeColumnWidthAction extends WbAction
{
	private ActionListener client;
	private boolean shiftPressed = false;

	public OptimizeColumnWidthAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtOptimizeCol");
	}

	public boolean includeColumnLabels()
	{
		return this.shiftPressed;
	}

	public void executeAction(ActionEvent e)
	{
		this.shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		e.setSource(this);
		this.client.actionPerformed(e);
		this.shiftPressed = false;
	}
}
