/*
 * ExecuteSelAction.java
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  info@sql-workbench.net
 */
public class ExecuteSelAction extends WbAction
{
	private SqlPanel target;
	
	public ExecuteSelAction(SqlPanel aPanel)
	{
		super();
		this.target = aPanel;
		this.initMenuDefinition(ResourceMgr.TXT_EXECUTE_SEL, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_EXEC_SEL));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbAction.ADD_TO_TOOLBAR, "true");
		this.putValue(ALTERNATE_ACCELERATOR, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.isEnabled()) this.target.runSelectedStatement();
	}
	
}
