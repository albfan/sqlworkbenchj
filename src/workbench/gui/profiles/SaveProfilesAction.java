/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.exception.WbException;
import workbench.gui.actions.WbAction;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SaveProfilesAction extends WbAction
{
	private FileActions client;

	public SaveProfilesAction(FileActions aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("LabelSaveProfiles"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("LabelSaveProfiles"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
	}

	public void actionPerformed(ActionEvent e)
	{
		try
		{
			this.client.saveItem();
		}
		catch (WbException ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}