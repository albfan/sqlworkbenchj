/*
 * ManageMacroAction.java
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

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import javax.swing.SwingUtilities;
import workbench.WbManager;
import workbench.gui.MainWindow;
import workbench.gui.macros.MacroManagerDialog;
import workbench.gui.sql.SqlPanel;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class ManageMacroAction
	extends WbAction
{
	private MainWindow client;

	public ManageMacroAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtManageMacros",KeyStroke.getKeyStroke(KeyEvent.VK_M, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		SqlPanel sql = this.client.getCurrentSqlPanel();
		if (sql != null)
		{
			Window w = SwingUtilities.getWindowAncestor(sql);
			Frame parent = null;
			if (w instanceof Frame)
			{
				parent = (Frame)w;
			}
			MacroManagerDialog d = new MacroManagerDialog(parent, sql);
			d.setVisible(true);
		}
		else
		{
			MacroManagerDialog d = new MacroManagerDialog(WbManager.getInstance().getCurrentWindow(), null);
			d.setVisible(true);
		}
	}

}
