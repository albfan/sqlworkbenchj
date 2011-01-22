/*
 * PrintAction.java
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
import workbench.print.TablePrinter;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class PrintAction 
		extends WbAction
		implements TableModelListener
{
	private WbTable client;

	public PrintAction(WbTable aClient)
	{
		super();
		this.setClient(aClient);
		this.initMenuDefinition("MnuTxtPrint");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon("Print");
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		TablePrinter printer = new TablePrinter(this.client);
		printer.startPrint();
	}
	
	public void tableChanged(TableModelEvent tableModelEvent)
	{
		this.setEnabled(this.client.getRowCount() > 0);
	}

	public void setClient(WbTable c)
	{
		if (this.client != null)
		{
			this.client.removeTableModelListener(this);
		}
		this.client = c;
		if (this.client != null)
		{
			this.client.addTableModelListener(this);
		}
	}
}
