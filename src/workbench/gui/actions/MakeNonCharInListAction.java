/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class MakeNonCharInListAction extends WbAction implements TextSelectionListener
{
	private EditorPanel client;

	public MakeNonCharInListAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.client.addSelectionListener(this);
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtMakeNonCharInList"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtMakeNonCharInList"));
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.makeInListForNonChar();
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}

}
