/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SaveDataAsAction extends WbAction
{
	private Exporter client;

	public SaveDataAsAction(Exporter aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveDataAs");
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_SAVE_AS));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveAs();
	}
}
