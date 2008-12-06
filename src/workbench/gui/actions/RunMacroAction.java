/*
 * RunMacroAction.java
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

import javax.swing.Action;

import javax.swing.KeyStroke;
import workbench.gui.MainWindow;
import workbench.gui.macros.MacroRunner;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.StoreableKeyStroke;
import workbench.sql.macros.MacroDefinition;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 *	@author  support@sql-workbench.net
 */
public class RunMacroAction
	extends WbAction
{
	private MainWindow client;
	private MacroDefinition macro;

	public RunMacroAction(MainWindow aClient, MacroDefinition def, int index)
	{
		super();
		this.macro = def;
		this.client = aClient;
		String menuTitle = def.getName();
		
		if (index < 10)
		{
			menuTitle = "&" + NumberStringCache.getNumberString(index) + " - " + def.getName();
		}
		this.setMenuText(menuTitle);
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		String desc = ResourceMgr.getDescription("MnuTxtRunMacro", true);
		desc = StringUtil.replace(desc, "%macro%", def.getName());
		this.putValue(Action.SHORT_DESCRIPTION, desc);
		this.setIcon(null);
		StoreableKeyStroke key = macro.getShortcut();
		if (key != null)
		{
			KeyStroke stroke = key.getKeyStroke();
			setAccelerator(stroke);
		}
		setEnabled(macro != null && client != null);
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null && this.macro != null)
		{
			boolean shiftPressed = isShiftPressed(e);
			SqlPanel sql = this.client.getCurrentSqlPanel();
			if (sql != null)
			{
				MacroRunner runner = new MacroRunner();
				runner.runMacro(macro, sql, shiftPressed);
			}
			else
			{
				LogMgr.logWarning("RunMacroAction.actionPerformed()", "Don't have a curretn SqlPanel!");
			}
		}
	}


}
