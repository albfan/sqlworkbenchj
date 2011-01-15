/*
 * ShowDbmsManualAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.WbSwingUtilities;
import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;

/**
 * @author Thomas Kellerer
 */
public class HelpContactAction
	extends WbAction
{
	private static HelpContactAction instance = new HelpContactAction();

	public static HelpContactAction getInstance()
	{
		return instance;
	}

	private HelpContactAction()
	{
		super();
		initMenuDefinition("MnuTxtHelpContact");
		removeIcon();
	}
	
	public synchronized void executeAction(ActionEvent e)
	{
		try
		{
			BrowserLauncher.openEmail("support@sql-workbench.net");
		}
		catch (Exception ex)
		{
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
		}
	}

}
