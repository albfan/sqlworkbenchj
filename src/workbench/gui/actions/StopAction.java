/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class StopAction extends WbAction
{
	private SqlPanel panel;

	public StopAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_STOP_STMT));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_STOP_STMT));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_STOP));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbAction.ADD_TO_TOOLBAR, "true");
		this.setCreateMenuSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.cancelExecution();
	}
}
