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
public class FindAgainAction extends AbstractAction
{
	private SqlPanel client;

	public FindAgainAction(SqlPanel aClient)
	{
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("FindAgain"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("FindAgain"));
    this.putValue(ACTION_COMMAND_KEY, "FindData");
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("FindAgain"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.findNext();
	}
}
