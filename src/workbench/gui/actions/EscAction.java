/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class EscAction extends WbAction
{
	private ActionListener client;

	public EscAction(ActionListener aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.actionPerformed(e);
	}
}
