/*
 * RenameTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.util.StringUtil;

/**
 *	@author  support@sql-workbench.net
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
		String col = WbSwingUtilities.getUserInput(client, "Find column", null);
		if (col != null)
		{
			scrollToColumn(col);
		}
	}
	
	protected void scrollToColumn(String colname)
	{
		if (StringUtil.isWhitespaceOrEmpty(colname)) return;

		for (int idx = 0; idx < client.getModel().getColumnCount(); idx++)
		{
			String name = client.getModel().getColumnName(idx);
			if (name.toLowerCase().startsWith(colname.toLowerCase()))
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
					}
				});
			}
		}
	}	
}
