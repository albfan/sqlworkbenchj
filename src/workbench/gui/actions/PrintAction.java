package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.macros.MacroManagerDialog;
import workbench.interfaces.PrintableComponent;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class PrintAction extends WbAction
{
	private PrintableComponent client;

	public PrintAction(PrintableComponent aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtPrint"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtPrint"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Print"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.print();
	}
}
