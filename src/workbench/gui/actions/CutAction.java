/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import workbench.interfaces.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
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
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_CUT));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_CUT));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_CUT));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_EDIT);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.cut();
	}
}
