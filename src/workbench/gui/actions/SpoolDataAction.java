/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

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
		this.initMenuDefinition("MnuTxtSpoolData");
		this.setIcon(ResourceMgr.getImage("SpoolData"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.spoolData();
	}

}
