/*
 * PrintPreviewAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class PrintPreviewAction extends WbAction
{
	private WbTable client;

	public PrintPreviewAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtPrintPreview");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.printPreview();
	}
}
