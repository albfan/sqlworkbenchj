/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	@author  workbench@kellerer.org
 */
public class NewListEntryAction 
	extends WbAction
{
	private FileActions client;

	public NewListEntryAction(FileActions aClient)
	{
		this.client = aClient;
		String tip = ResourceMgr.getDescription("LabelNewListEntry");
		String shift = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
		tip = StringUtil.replace(tip, "%shift%", shift);
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_NEW));
		this.initMenuDefinition(ResourceMgr.getString("LabelNewListEntry"), tip, null);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		try
		{
			this.client.newItem(shiftPressed);
		}
		catch (Exception ex)
		{
			LogMgr.logError("NewListEntryAction.executeAction()", "Error creating new list entry", ex);
		}
		
	}
}
