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
import workbench.resource.ResourceMgr;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class NewProfileAction extends AbstractAction
{
	private FileActions client;

	public NewProfileAction(FileActions aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("LabelNewProfile"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("LabelNewProfile"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_NEW));
	}

	public void actionPerformed(ActionEvent e)
	{
		try
		{
			this.client.newItem();
		}
		catch (WbException ex)
		{
			LogMgr.logError(this, "Error creating profile", ex);
		}
	}
}
