/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

/**
 *	Action to paste the contents of the clipboard into the entry field
 *	@author  workbench@kellerer.org
 */
public class FileExitAction extends WbAction
{
	public FileExitAction()
	{
		super();
		this.initMenuDefinition(ResourceMgr.MNU_TXT_EXIT);
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().exitWorkbench();
	}
}
