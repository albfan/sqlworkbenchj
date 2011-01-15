/*
 * ScrollToColumnAction.java
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

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *	@author  Thomas Kellerer
 */
public class ScrollToColumnAction extends WbAction
{
	private WbTable client;

	public ScrollToColumnAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFindColumn");
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		String lastValue = Settings.getInstance().getProperty("workbench.gui.findcolumn.lastvalue", null);
		String col = WbSwingUtilities.getUserInput(client, ResourceMgr.getPlainString("MnuTxtFindColumn"), lastValue);
		if (col != null)
		{
			Settings.getInstance().setProperty("workbench.gui.findcolumn.lastvalue", col);
			scrollToColumn(col.toLowerCase());
		}
	}
	
	protected void scrollToColumn(String toFind)
	{
		if (StringUtil.isBlank(toFind)) return;

		for (int idx = 0; idx < client.getModel().getColumnCount(); idx++)
		{
			String name = client.getModel().getColumnName(idx);
			if (name.toLowerCase().indexOf(toFind) > -1)
			{
				int row = client.getSelectedRow();
				if (row < 0) 
				{
					row = client.getFirstVisibleRow();
				}
				final Rectangle rect = client.getCellRect(row, idx, true);
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						client.scrollRectToVisible(rect);
						client.getTableHeader().repaint();
						client.repaint();
					}
				});
			}
		}
	}	
}
