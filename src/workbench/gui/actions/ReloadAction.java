/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

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
		this.putValue(Action.NAME, ResourceMgr.getString("TxtReload"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Refresh"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("TxtReload"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.reload();
	}
	
}
