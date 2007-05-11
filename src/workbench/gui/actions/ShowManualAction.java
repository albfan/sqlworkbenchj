/*
 * ShowManualAction.java
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
import java.io.File;
import javax.swing.JFrame;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

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
	}
	
	public void executeAction(ActionEvent e)
	{
		showHelp();
	}
	
	public void showHelp()
	{
		try
		{
			String readerPath = Settings.getInstance().getPDFReaderPath();
			if (StringUtil.isEmptyString(readerPath))
			{
				String msg = ResourceMgr.getString("ErrNoReaderDefined");
				WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			
			File reader = new File(readerPath);
			if (!reader.exists() || !reader.canRead())
			{
				String msg = ResourceMgr.getString("ErrExeNotAvail");
				msg = StringUtil.replace(msg, "%exepath%", readerPath);
				WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			
			String pdf = Settings.getInstance().getManualPath();
			if (pdf == null)
			{
				String msg = ResourceMgr.getString("ErrManualNotFound");
				msg = StringUtil.replace(msg, "%jarpath%", WbManager.getInstance().getJarPath());
				WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			String[] cmd = new String[] { readerPath, pdf };
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowManualAction.showManual()", "Error when running PDF Viewer", ex);
		}
	}


}
