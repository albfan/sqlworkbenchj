/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class SetColumnWidthAction extends WbAction
{
	private ActionListener client;

	public SetColumnWidthAction(ActionListener aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtSetColWidth");
	}

	public void executeAction(ActionEvent e)
	{
		e.setSource(this);
		this.client.actionPerformed(e);
	}
}
