/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;


/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class FindDataAgainAction extends WbAction
{
	private Searchable client;

	public FindDataAgainAction(Searchable aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtFindDataAgain"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("FindAgain"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtFindDataAgain"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.findNext();
	}
}
