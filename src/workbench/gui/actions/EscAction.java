/*
 * EscAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 *	Action to clear the contents of a entry field
 *	@author  info@sql-workbench.net
 */
public class EscAction extends WbAction
{
	private ActionListener client;

	public EscAction(ActionListener aClient)
	{
		super();
		this.client = aClient;
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.actionPerformed(e);
	}
}
