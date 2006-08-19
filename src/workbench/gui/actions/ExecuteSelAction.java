/*
 * ExecuteSelAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class ExecuteSelAction 
	extends WbAction
{
	private SqlPanel target;
	
	public ExecuteSelAction(SqlPanel aPanel)
	{
		super();
		this.target = aPanel;
		this.initMenuDefinition("MnuTxtExecuteSel", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		this.setIcon(ResourceMgr.getImage("ExecuteSel"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setAlternateAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.isEnabled()) this.target.runSelectedStatement();
	}
	
}
