/*
 * ShowDbmsManualAction.java
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
import workbench.gui.WbSwingUtilities;
import workbench.resource.Settings;
import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class ShowDbmsManualAction
	extends WbAction
{
	private String onlineManualUrl;

	private static ShowDbmsManualAction instance = new ShowDbmsManualAction();

	public static ShowDbmsManualAction getInstance()
	{
		return instance;
	}

	private ShowDbmsManualAction()
	{
		super();
		initMenuDefinition("MnuTxtDbmsHelp");
		removeIcon();
	}
	
	public synchronized void executeAction(ActionEvent e)
	{
		if (StringUtil.isNonBlank(onlineManualUrl))
		{
			try
			{
				BrowserLauncher.openURL(onlineManualUrl);
			}
			catch (Exception ex)
			{
				WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
			}
		}
	}
	
	public synchronized void setDbms(String dbid)
	{
		if (StringUtil.isNonBlank(dbid))
		{
			onlineManualUrl = Settings.getInstance().getProperty("workbench.db." + dbid + ".manual", null);
		}
		else
		{
			onlineManualUrl = null;
		}
		setEnabled(StringUtil.isNonBlank(onlineManualUrl));
	}

}
