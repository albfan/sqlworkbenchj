package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.macros.MacroManagerDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class ManageMacrosAction extends WbAction
{
	private MainWindow client;

	public ManageMacrosAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtManageMacros"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtManageMacros"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
	}

	public void actionPerformed(ActionEvent e)
	{
		MacroManagerDialog d = new MacroManagerDialog(client, true);
		WbSwingUtilities.center(d, client);
		d.show();
	}
}
