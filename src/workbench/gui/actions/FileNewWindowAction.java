package workbench.gui.actions;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.resource.ResourceMgr;
import java.awt.event.ActionListener;
import workbench.WbManager;

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
