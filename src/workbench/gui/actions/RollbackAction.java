package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.interfaces.Commitable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class RollbackAction extends WbAction
{
	private Commitable client;
	public RollbackAction(Commitable aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtRollback"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Rollback"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtRollback"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null) this.client.rollback();
	}
	
}
