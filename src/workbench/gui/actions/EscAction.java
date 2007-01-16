/*
 * EscAction.java
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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 *	An action mapped to the ESC key
 *	@author  support@sql-workbench.net
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
		e.setSource(this);
		this.client.actionPerformed(e);
	}
}
