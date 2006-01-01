/*
 * KeyAction.java
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
import java.awt.event.KeyEvent;
import javax.swing.Action;

import javax.swing.KeyStroke;
import workbench.gui.WbSwingUtilities;

/**
 *	Action to clear the contents of a entry field
 *	@author  support@sql-workbench.net
 */
public class KeyAction extends WbAction
{
	public KeyAction(ActionListener aClient, KeyStroke stroke)
	{
		super(aClient, getKeyName(stroke));
		this.setAccelerator(stroke);
	}
	
	private static final String getKeyName(KeyStroke stroke)
	{
		String keyName;
		if (stroke.getModifiers() == 0)
			keyName = WbSwingUtilities.getKeyName(stroke.getKeyCode());
		else
			keyName = KeyEvent.getKeyModifiersText(stroke.getModifiers()) + "-" +WbSwingUtilities.getKeyName(stroke.getKeyCode());
		return keyName;
	}
}
