/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
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
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_PASTE));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_PASTE));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_PASTE));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_EDIT);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.paste();
	}
}
