package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class FileSaveAction extends WbAction
{
	private TextFileContainer client;

	public FileSaveAction(TextFileContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileSave", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveCurrentFile();
	}
}
