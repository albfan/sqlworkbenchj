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
public class PrintPreviewAction extends WbAction
{
	private PrintableComponent client;

	public PrintPreviewAction(PrintableComponent aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtPrintPreview"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtPrintPreview"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.printPreview();
	}
}
