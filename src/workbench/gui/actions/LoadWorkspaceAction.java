package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class LoadWorkspaceAction extends WbAction
{
	private MainWindow client;

	public LoadWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtLoadWorkspace", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_WORKSPACE);
		this.setIcon(null);
		
	}

	public void executeAction(ActionEvent e)
	{
		this.client.loadWorkspace();
	}
}
