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
 *	@author  sql.workbench@freenet.de
 */
public class SaveProfileAction extends AbstractAction
{
	private FileActions client;

	public SaveProfileAction(FileActions aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("LabelSaveProfile"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("LabelSaveProfile"));
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