package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Undoable;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class RedoAction extends WbAction
{
	private Undoable client;

	public RedoAction(Undoable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtRedo");
		this.setIcon(ResourceMgr.getImage("Redo"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.redo();
	}
}
