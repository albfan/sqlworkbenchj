/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import workbench.interfaces.ClipboardSupport;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  sql.workbench@freenet.de
 */
public abstract class WbAction extends AbstractAction
{
	private String actionName = null;

	public WbAction()
	{
		String c = this.getClass().getName();
		this.actionName = c.substring(c.lastIndexOf('.')  + 1);
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
	}

	public KeyStroke getAccelerator()
	{
		return (KeyStroke)this.getValue(Action.ACCELERATOR_KEY);
	}

	public JMenuItem getMenuItem()
	{
		JMenuItem item = new JMenuItem();
		item.setAction(this);
		item.setAccelerator(this.getAccelerator());
		return item;
	}

	public String getActionName()
	{
		return this.actionName;
	}
}
