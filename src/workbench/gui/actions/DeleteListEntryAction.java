/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.exception.WbException;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class DeleteListEntryAction extends WbAction
{
	private FileActions client;

	public DeleteListEntryAction(FileActions aClient)
	{
		this(aClient, "LabelDeleteListEntry");
	}
	
	public DeleteListEntryAction(FileActions aClient, String aKey)
	{
		this.client = aClient;
		this.setMenuTextByKey(aKey);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_DELETE));
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.deleteItem();
		}
		catch (WbException ex)
		{
			LogMgr.logError(this, "Error saving profiles", ex);
		}
	}
}
