package workbench.gui.actions;

import java.awt.event.ActionEvent;

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
		this.initMenuDefinition("MnuTxtPrint");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(ResourceMgr.getImage("Print"));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.print();
	}
}
