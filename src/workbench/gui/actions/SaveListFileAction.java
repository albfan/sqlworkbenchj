/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SaveListFileAction extends WbAction
{
	private FileActions client;

	public SaveListFileAction(FileActions aClient)
	{
		this.client = aClient;
		this.setMenuTextByKey("LabelSaveProfiles");
		this.setIcon(	ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.saveItem();
		}
		catch (Exception ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}