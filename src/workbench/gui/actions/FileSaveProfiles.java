/*
 * FileSaveProfiles.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.exception.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

public class FileSaveProfiles extends WbAction
{
	public FileSaveProfiles()
	{
		super();
		this.initMenuDefinition("MnuTxtFilesSaveProfiles");
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			ConnectionMgr.getInstance().saveProfiles();
			WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("MsgProfilesSaved"));
		}
		catch (Exception ex)
		{
			LogMgr.logError("FileSaveProfiles.executeAction()", "Error saving profiles", ex);
			WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("ErrorSavingProfiles") + "\n" + ExceptionUtil.getDisplay(ex));
		}
	}
}
