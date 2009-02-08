/*
 * ShowDbExplorerAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.util.List;
import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.interfaces.MainPanel;
import workbench.interfaces.ToolWindow;
import workbench.resource.PlatformShortcuts;
import workbench.resource.Settings;

/**
 *	@author  support@sql-workbench.net
 */
public class ShowDbExplorerAction
	extends WbAction
{
	private MainWindow mainWin;
	public ShowDbExplorerAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtShowDbExplorer",KeyStroke.getKeyStroke(KeyEvent.VK_D, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("Database");
		setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		boolean useTab = Settings.getInstance().getShowDbExplorerInMainWindow();
		if (useTab)
		{
			int index = findFirstExplorerTab();
			if (index > -1)
			{
				mainWin.selectTab(index);
			}
			else
			{
				mainWin.newDbExplorerPanel(true);
			}
		}
		else
		{
			List<ToolWindow> windows = mainWin.getExplorerWindows();
			if (windows.size() > 0)
			{
				ToolWindow w = windows.get(0);
				w.activate();
			}
			else
			{
				mainWin.newDbExplorerWindow();
			}
		}
	}

	/**
	 *	Returns the index of the first explorer tab
	 */
	protected int findFirstExplorerTab()
	{
		int count = mainWin.getTabCount();
		if (count <= 0) return -1;

		for (int i=0; i < count; i++)
		{
			MainPanel p = mainWin.getSqlPanel(i);
			if (p instanceof DbExplorerPanel) return i;
		}
		return -1;
	}
}
