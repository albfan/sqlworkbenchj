/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.DbData;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopyRowAction extends WbAction
{
	private DbData client;

	public CopyRowAction(DbData aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyRow");
		this.setIcon(ResourceMgr.getImage("CopyRow"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.duplicateRow();
	}
}
