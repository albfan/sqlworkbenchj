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
 *	Action to cut  the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class CutAction extends WbAction
{
	private ClipboardSupport client;

	public CutAction(ClipboardSupport aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey(ResourceMgr.TXT_CUT);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_CUT));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.client.cut();
	}
}
