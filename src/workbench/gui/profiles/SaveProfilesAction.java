/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.exception.WbException;
import workbench.gui.actions.WbAction;
import workbench.resource.ResourceMgr;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;

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