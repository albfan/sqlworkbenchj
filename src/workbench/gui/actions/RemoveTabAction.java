package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.gui.MainWindow;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class RemoveTabAction extends WbAction
{
	private MainWindow client;

	public RemoveTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtRemoveTab"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtRemoveTab"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_VIEW);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_MASK));
		this.putValue(Action.SMALL_ICON, null);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.removeTab();
	}
}
