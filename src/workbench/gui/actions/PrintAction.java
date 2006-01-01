/*
 * PrintAction.java
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class PrintAction 
		extends WbAction
		implements TableModelListener
{
	private WbTable client;

	public PrintAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.client.addTableModelListener(this);
		this.initMenuDefinition("MnuTxtPrint");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(ResourceMgr.getImage("Print"));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.printTable();
	}
	
	public void tableChanged(TableModelEvent tableModelEvent)
	{
		this.setEnabled(this.client.getRowCount() > 0);
	}
	
}
