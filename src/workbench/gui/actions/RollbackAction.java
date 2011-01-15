/*
 * RollbackAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Commitable;
import workbench.resource.ResourceMgr;

/**
 *	Send a rollback() to the client
 *	@author  Thomas Kellerer
 */
public class RollbackAction
	extends WbAction
{
	private Commitable client;

	public RollbackAction(Commitable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRollback", KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon("Rollback");
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null) this.client.rollback();
	}

}
