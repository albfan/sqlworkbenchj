package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class FileSaveAsAction extends WbAction
{
	private TextFileContainer client;

	public FileSaveAsAction(TextFileContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileSaveAs");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveFile();
	}
}
