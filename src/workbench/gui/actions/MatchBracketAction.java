/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class MatchBracketAction extends WbAction
{
	private EditorPanel client;

	public MatchBracketAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtMatchBracket", KeyStroke.getKeyStroke(KeyEvent.VK_B,KeyEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.matchBracket();
	}

}