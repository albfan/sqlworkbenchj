/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.components.SortArrowIcon;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class OptimizeColumnWidthAction extends WbAction
{
	private ActionListener client;

	public OptimizeColumnWidthAction(ActionListener aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtOptimizeCol"));
	}

	public void actionPerformed(ActionEvent e)
	{
		e.setSource(this);
		this.client.actionPerformed(e);
	}
}
