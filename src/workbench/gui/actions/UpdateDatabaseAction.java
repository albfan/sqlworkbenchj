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
 *	
 */
public class UpdateDatabaseAction extends WbAction
{
	private SqlPanel panel;
	
	public UpdateDatabaseAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtUpdateDatabase"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtUpdateDatabase"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(true);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.panel.saveChangesToDatabase();
	}
}
