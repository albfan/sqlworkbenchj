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

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  thomas.kellerer@web.de
 */
public class ExecuteAllAction extends AbstractAction
{
	private ActionListener client;
	
	public ExecuteAllAction(ActionListener aListener)
	{
		this.client = aListener;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_EXECUTE_ALL));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_EXEC_ALL));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.client.actionPerformed(e);
	}
}
