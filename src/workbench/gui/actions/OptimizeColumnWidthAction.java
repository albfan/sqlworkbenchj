/*
 * OptimizeColumnWidthAction.java
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

import java.awt.event.ActionEvent;

import workbench.gui.components.WbTable;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *	@author  support@sql-workbench.net
 */
public class OptimizeColumnWidthAction 
	extends WbAction
{
	protected WbTable client;

	public OptimizeColumnWidthAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtOptimizeCol");
	}

	public void executeAction(ActionEvent e)
	{
		if (client == null) return;
		final boolean respectColName = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) || Settings.getInstance().getIncludeHeaderInOptimalWidth();
		final int column = client.getPopupColumnIndex();
		Thread t = new WbThread("OptimizeCol Thread")
		{
			public void run()	
			{ 
				client.optimizeColWidth(column, respectColName); 
			}
		};
		t.start();
	}
}
