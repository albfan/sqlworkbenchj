package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.ClipboardSupport;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopyAction extends WbAction
{
	private ClipboardSupport client;

	public CopyAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey(ResourceMgr.TXT_COPY);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_COPY));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.copy();
	}
}
