/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.interfaces.Spooler;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	
 */
public class SpoolDataAction extends WbAction
{
	private Spooler client;
	
	public SpoolDataAction(Spooler aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtSpoolData"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtSpoolData"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("SpoolData"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.spoolData();
	}

}
