package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.PrintableComponent;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class PrintPreviewAction extends WbAction
{
	private PrintableComponent client;

	public PrintPreviewAction(PrintableComponent aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtPrintPreview");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.printPreview();
	}
}
