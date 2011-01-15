/*
 * WhatsNewAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
public class WhatsNewAction
	extends WbAction
{
	private static WhatsNewAction instance = new WhatsNewAction();
	public static WhatsNewAction getInstance()
	{
		return instance;
	}
	
	private WhatsNewAction()
	{
		super();
		this.initMenuDefinition("MnuTxtWhatsNew");
		this.removeIcon();
	}

	public void executeAction(ActionEvent e)
	{
		HelpManager.showHistory();
	}
	
}
