/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import workbench.gui.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import workbench.resource.ResourceMgr;
import javax.swing.Action;

/**
 *	Action to clear the contents of a entry field
 *	@author  thomas.kellerer@web.de
 */
public class ClearAction
	extends AbstractAction
{
	private ClipboardSupport client;
	
	public ClearAction(ClipboardSupport aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_CLEAR));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.client.clear();
	}
}
