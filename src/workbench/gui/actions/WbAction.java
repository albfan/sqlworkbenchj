/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.Insets;
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
	public static final String ADD_TO_TOOLBAR = "AddToToolbar";
	public static final String MAIN_MENU_ITEM = "MainMenuItem";
	public static final String MENU_SEPARATOR = "MenuSepBefore";
	public static final String TBAR_SEPARATOR = "TbarSepBefore";
	
	private String actionName = null;

	public WbAction()
	{
		String c = this.getClass().getName();
		this.actionName = c.substring(c.lastIndexOf('.')  + 1);
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("blank"));
	}

	public KeyStroke getAccelerator()
	{
		return (KeyStroke)this.getValue(Action.ACCELERATOR_KEY);
	}

	public JMenuItem getMenuItem()
	{
		JMenuItem item = new JMenuItem();
		item.setMargin(new Insets(0,0,0,0));
		item.setAction(this);
		item.setAccelerator(this.getAccelerator());
		return item;
	}

	public void setCreateToolbarSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.TBAR_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.TBAR_SEPARATOR, "false");
		}
	}
	public void setCreateMenuSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.MENU_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.MENU_SEPARATOR, "false");
		}
	}
	
	public String getActionName()
	{
		return this.actionName;
	}
}
