/*
 * CommitAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
 * Action to send a commit to the DBMS
 * @see workbench.gui.sql.SqlPanel#commit()
 * @author  support@sql-workbench.net
 */
public class CommitAction extends WbAction
{
	private Commitable client;
	
	public CommitAction(Commitable aClient)
	{
		super();
		this.client = aClient;
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_MASK);
		this.initMenuDefinition("MnuTxtCommit",key);
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon(ResourceMgr.getImage("Commit"));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null) this.client.commit();
	}
	
}
