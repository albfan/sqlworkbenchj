/*
 * PrintAction.java
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

import workbench.interfaces.PrintableComponent;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class PrintAction extends WbAction
{
	private PrintableComponent client;

	public PrintAction(PrintableComponent aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtPrint");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(ResourceMgr.getImage("Print"));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.print();
	}
}
