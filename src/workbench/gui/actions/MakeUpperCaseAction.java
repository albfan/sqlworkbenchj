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
public class MakeUpperCaseAction extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public MakeUpperCaseAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.client.addSelectionListener(this);
		this.initMenuDefinition("MnuTxtMakeUpperCase", KeyStroke.getKeyStroke(KeyEvent.VK_U,KeyEvent.CTRL_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toUpperCase();
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}
}
