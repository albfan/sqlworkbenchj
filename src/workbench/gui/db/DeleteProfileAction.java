/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.db;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class DeleteProfileAction extends AbstractAction
{
	private FileActions client;
	
	public DeleteProfileAction(FileActions aClient)
	{
		this.client = aClient;		
		this.putValue(Action.NAME, ResourceMgr.getString("DeleteProfile"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("DeleteProfile"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_DELETE));
	}
	
	public void actionPerformed(ActionEvent e)
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
