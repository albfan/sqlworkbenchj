/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;

import workbench.exception.WbException;
import workbench.gui.actions.WbAction;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	@author  workbench@kellerer.org
 */
public class NewListEntryAction extends WbAction
{
	private FileActions client;

	public NewListEntryAction(FileActions aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("LabelNewListEntry"));
		String tip = ResourceMgr.getDescription("LabelNewListEntry");
		String shift = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
		tip = StringUtil.replace(tip, "%shift%", shift);
		this.putValue(Action.SHORT_DESCRIPTION, tip);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_NEW));
	}

	public void actionPerformed(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		try
		{
			this.client.newItem(shiftPressed);
		}
		catch (WbException ex)
		{
			LogMgr.logError(this, "Error creating profile", ex);
		}
	}
}
