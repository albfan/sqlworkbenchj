package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.DriverEditorDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class CloseWorkspaceAction extends WbAction
{
	private MainWindow client;

	public CloseWorkspaceAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtCloseWorkspace"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtCloseWorkspace"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_FILE);
		//this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.closeWorkspace();
	}
}
