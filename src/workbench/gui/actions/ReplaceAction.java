package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.interfaces.Replaceable;

import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class ReplaceAction extends WbAction
{
	private Replaceable client;

	public ReplaceAction(Replaceable aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtReplace"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_EDIT);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtReplace"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.replace();
	}
}
