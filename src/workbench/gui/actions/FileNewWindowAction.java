package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

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
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		WbManager.getInstance().openNewWindow();
	}
}
