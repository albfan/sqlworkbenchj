/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.resource.ResourceMgr;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class ExecuteAllAction extends WbAction
{
	private ActionListener client;

	public ExecuteAllAction(ActionListener aListener)
	{
		super();
		this.client = aListener;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_EXECUTE_ALL));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_EXECUTE_ALL));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_EXEC_ALL));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.actionPerformed(e);
	}
}
