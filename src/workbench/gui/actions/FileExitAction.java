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
import workbench.WbManager;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class FileExitAction extends WbAction
{
	public FileExitAction()
	{
		super();
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.MNU_TXT_EXIT));
	}

	public void actionPerformed(ActionEvent e)
	{
		WbManager.getInstance().exitWorkbench();
	}
}
