/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.resource.ResourceMgr;

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
		this.initMenuDefinition(ResourceMgr.TXT_EXECUTE_ALL, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_EXEC_ALL));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.actionPerformed(e);
	}
}
