package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.WbManager;

/**
 *	@author  workbench@kellerer.org
 */
public class FileNewWindowAction extends WbAction
{
	public FileNewWindowAction()
	{
		super();
		this.initMenuDefinition("MnuTxtFileNewWindow");
		//this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().openNewWindow();
	}
}
