/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.DbUpdater;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	
 */
public class UpdateDatabaseAction extends WbAction
{
	private DbUpdater panel;
	
	public UpdateDatabaseAction(DbUpdater aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtUpdateDatabase");
		this.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(true);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		panel.saveChangesToDatabase();
	}
	
	public void setClient(DbUpdater aPanel)
	{
		this.panel = aPanel;
	}
}
