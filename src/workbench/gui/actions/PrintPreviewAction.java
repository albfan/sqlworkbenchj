/*
 * PrintPreviewAction.java
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class PrintPreviewAction 		
	extends WbAction
	implements TableModelListener
{
	private WbTable client;

	public PrintPreviewAction(WbTable aClient)
	{
		super();
		this.setClient(aClient);
		this.initMenuDefinition("MnuTxtPrintPreview");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.printPreview();
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
