/*
 * ShowManualAction.java
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
import workbench.gui.help.HelpManager;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class ShowManualAction
	extends WbAction
{
	public ShowManualAction()
	{
		super();
		initMenuDefinition("MnuTxtHelpManual");
		setIcon(ResourceMgr.getImage("pdf"));
	}
	
	public void executeAction(ActionEvent e)
	{
		HelpManager.showPdfHelp();
	}

}
