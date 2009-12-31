/*
 * ShowManualAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.help.HelpManager;

/**
 * @author Thomas Kellerer
 */
public class ShowManualAction
	extends WbAction
{
	public ShowManualAction()
	{
		super();
		initMenuDefinition("MnuTxtHelpManual");
		setIcon("pdf");
	}
	
	public void executeAction(ActionEvent e)
	{
		HelpManager.showPdfHelp();
	}

}
