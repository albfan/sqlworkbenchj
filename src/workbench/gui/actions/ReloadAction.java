/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class ReloadAction extends WbAction
{
	private Reloadable client;

	public ReloadAction(Reloadable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("TxtReload");
		this.setIcon(ResourceMgr.getImage("Refresh"));
		//this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.reload();
	}
	
}
