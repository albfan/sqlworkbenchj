package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class CloseWorkspaceAction extends WbAction
{
	private MainWindow client;

	public CloseWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCloseWorkspace", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.closeWorkspace();
	}
}
