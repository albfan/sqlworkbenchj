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
 *	@author  workbench@kellerer.org
 */
public class CopyProfileAction 
	extends WbAction
{
	private FileActions client;

	public CopyProfileAction(FileActions aClient)
	{
		this.client = aClient;
		this.setIcon(ResourceMgr.getImage("CopyProfile"));
		this.initMenuDefinition("LabelCopyProfile");
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			this.client.newItem(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error copying profile", ex);
		}
		
	}
}
