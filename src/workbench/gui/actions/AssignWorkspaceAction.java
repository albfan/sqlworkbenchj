package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class AssignWorkspaceAction extends WbAction
{
	private MainWindow client;

	public AssignWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtAssignWorkspace", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.assignWorkspace();
	}
	
}
