package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
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
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_COPY));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_COPY));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_COPY));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_EDIT);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.copy();
	}
}
