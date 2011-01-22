/*
 * OptimizeRowHeightAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import workbench.gui.components.WbTable;
import workbench.util.WbThread;

/**
 *	@author  Thomas Kellerer
 */
public class OptimizeRowHeightAction 
	extends WbAction
	implements TableModelListener
{
	protected WbTable client;

	public OptimizeRowHeightAction()
	{
		super();
		initMenuDefinition("LblRowHeightOpt");
		this.setEnabled(false);		
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
				client.optimizeRowHeight();
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
