package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.DriverEditorDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class ManageDriversAction extends WbAction
{
	private MainWindow client;

	public ManageDriversAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtEditDrivers");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		DriverEditorDialog d = new DriverEditorDialog(client, true);
		WbSwingUtilities.center(d, client);
		d.show();
	}
}
