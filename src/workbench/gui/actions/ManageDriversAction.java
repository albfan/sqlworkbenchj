package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

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
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtEditDrivers"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("EditDrivers"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
	}

	public void actionPerformed(ActionEvent e)
	{
		DriverEditorDialog d = new DriverEditorDialog(client, true);
		WbSwingUtilities.center(d, client);
		d.show();
	}
}
