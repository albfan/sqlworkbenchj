package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class FileNewWindowAction extends WbAction
{
	public FileNewWindowAction()
	{
		super();
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtFileNewWindow"));
	}

	public void actionPerformed(ActionEvent e)
	{
		WbManager.getInstance().openNewWindow();
	}
}
