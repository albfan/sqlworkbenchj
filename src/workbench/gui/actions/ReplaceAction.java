package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.Replaceable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class ReplaceAction extends WbAction
{
	private Replaceable client;

	public ReplaceAction(Replaceable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtReplace",KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.replace();
	}
}
