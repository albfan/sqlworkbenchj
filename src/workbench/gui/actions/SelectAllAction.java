/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import workbench.gui.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  thomas.kellerer@web.de
 */
public class SelectAllAction extends AbstractAction
{
	private ClipboardSupport client;
	
	public SelectAllAction(ClipboardSupport aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_SELECTALL));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.client.selectAll();
	}
}
