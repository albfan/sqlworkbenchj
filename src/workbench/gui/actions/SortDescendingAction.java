/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;

import workbench.gui.components.SortArrowIcon;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SortDescendingAction extends WbAction
{
	private ActionListener client;

	public SortDescendingAction(ActionListener aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtSortDescending");
		this.putValue(Action.SMALL_ICON, SortArrowIcon.ARROW_UP);
	}

	public void executeAction(ActionEvent e)
	{
		e.setSource(this);
		this.client.actionPerformed(e);
	}
}
