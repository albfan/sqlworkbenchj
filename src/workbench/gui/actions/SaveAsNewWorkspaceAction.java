package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class SaveAsNewWorkspaceAction extends WbAction
{
	private MainWindow client;

	public SaveAsNewWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveAsNewWorkspace");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveWorkspace(null);
	}
	
}
