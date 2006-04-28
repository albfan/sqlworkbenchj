/*
 * RunMacroAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.Action;

import workbench.gui.MainWindow;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	@author  support@sql-workbench.net
 */
public class RunMacroAction extends WbAction
{
	private MainWindow client;
	private String macroName;

	public RunMacroAction(MainWindow aClient, String aName, int index)
	{
		super();
		this.macroName = aName;
		this.client = aClient;
		String menuTitle = aName;
		
		if (index < 10)
		{
			menuTitle = "&" + Integer.toString(index) + " - " + aName;
		}
		this.setMenuText(menuTitle);
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		String desc = ResourceMgr.getDescription("MnuTxtRunMacro", true);
		desc = StringUtil.replace(desc, "%macro%", aName);
		this.putValue(Action.SHORT_DESCRIPTION, desc);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null && this.macroName != null)
		{
			boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
			SqlPanel sql = this.client.getCurrentSqlPanel();
			if (sql != null)
			{	
				sql.executeMacro(macroName, shiftPressed);
			}
			else
			{
				LogMgr.logWarning("RunMacroAction.actionPerformed()", "Don't have a curretn SqlPanel!");
			}
		}
	}

}
