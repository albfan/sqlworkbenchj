/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class DeleteRowAction extends WbAction
{
	private DbData client;

	public DeleteRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtDeleteRow"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("Delete"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtDeleteRow"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.deleteRow();
	}
}
