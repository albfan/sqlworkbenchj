package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class RenameTabAction extends WbAction
{
	private MainWindow client;

	public RenameTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRenameTab");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.renameTab();
	}
}
