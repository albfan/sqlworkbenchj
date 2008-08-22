/*
 * OptimizeColumnWidthAction.java
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.ColumnWidthOptimizer;

import workbench.gui.components.RowHeightOptimizer;
import workbench.gui.components.WbTable;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *	@author  support@sql-workbench.net
 */
public class OptimizeRowHeightAction 
	extends WbAction
	implements TableModelListener
{
	protected WbTable client;

	public OptimizeRowHeightAction()
	{
		super();
		this.setMenuTextByKey("LblRowHeightAuto");
	}

	public OptimizeRowHeightAction(WbTable table)
	{
		this();
		setClient(table);
	}
	
	public void setClient(WbTable table)
	{
//		if (client != null)
//		{
//			client.removeTableModelListener(this);
//		}
		this.client = table;
//		if (client != null)
//		{
//			client.addTableModelListener(this);
//		}
		checkEnabled();
	}
	
	public void executeAction(ActionEvent e)
	{
		if (client == null) return;
		Thread t = new WbThread("OptimizeRows Thread")
		{
			public void run()	
			{
				RowHeightOptimizer optimizer = new RowHeightOptimizer(client);
				optimizer.optimizeAllRows();
			}
		};
		t.start();
	}

	private void checkEnabled()
	{
		if (this.client != null)
		{
			this.setEnabled(client.getRowCount() > 0);
		}
		else
		{
			setEnabled(false);
		}
	}
	public void tableChanged(TableModelEvent e)
	{
		checkEnabled();
	}
}
