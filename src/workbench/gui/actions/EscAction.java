/*
 * EscAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 *	An action mapped to the ESC key
 *	@author  Thomas Kellerer
 */
public class EscAction 
	extends WbAction
{
	private ActionListener client;

	public EscAction(JDialog d, ActionListener aClient)
	{
		super();
		client = aClient;
		isConfigurable = false;
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
		addToInputMap(d.getRootPane());
	}

	public void actionPerformed(ActionEvent e)
	{
		e.setSource(this);
		this.client.actionPerformed(e);
	}

	public void addToInputMap(JComponent c)
	{
		super.addToInputMap(c, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

}
