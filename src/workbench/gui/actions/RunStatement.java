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
import workbench.interfaces.RunnableStatement;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class RunStatement 
	extends WbAction
{
	private RunnableStatement target;
	
	public RunStatement(RunnableStatement runner)
	{
		super();
		this.target = runner;
		this.initMenuDefinition("MnuTxtRunStmt");
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_EXEC_SEL));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.isEnabled()) this.target.runStatement();
	}
	
}
