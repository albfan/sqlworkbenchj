/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.TextFileContainer;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class FileOpenAction extends WbAction
{
	private TextFileContainer client;

	public FileOpenAction(TextFileContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtFileOpen", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		this.setIcon(ResourceMgr.getImage("Open"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.openFile();
	}
}
