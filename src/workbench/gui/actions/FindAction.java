/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import workbench.interfaces.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import workbench.resource.ResourceMgr;
import javax.swing.Action;
import workbench.gui.sql.SqlPanel;

/**
 *	Action to clear the contents of a entry field
 *	@author  thomas.kellerer@web.de
 */
public class FindAction extends AbstractAction
{
	private SqlPanel client;

	public FindAction(SqlPanel aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("Find"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Find"));
    this.putValue(ACTION_COMMAND_KEY, "Find");
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("Find"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.findData();
	}
}
