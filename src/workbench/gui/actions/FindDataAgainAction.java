/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
		this.initMenuDefinition("MnuTxtFindDataAgain", KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK));
		this.setIcon(ResourceMgr.getImage("FindAgain"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.findNext();
	}
}
