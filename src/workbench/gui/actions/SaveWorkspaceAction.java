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
public class SaveWorkspaceAction extends WbAction
{
	private MainWindow client;

	public SaveWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSaveWorkspace",KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.saveWorkspace(this.client.getCurrentWorkspaceFile());
	}
	
}
