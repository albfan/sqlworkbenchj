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

import workbench.interfaces.ClipboardSupport;
import workbench.resource.ResourceMgr;


/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class PasteAction extends WbAction
{
	private ClipboardSupport client;

	public PasteAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey(ResourceMgr.TXT_PASTE);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_PASTE));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.paste();
	}
}
