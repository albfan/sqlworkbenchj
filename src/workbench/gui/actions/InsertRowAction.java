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
public class InsertRowAction extends WbAction
{
	private SqlPanel client;

	public InsertRowAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtInsertRow"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("RowInsertAfter"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtInsertRow"));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.insertRow();
	}
}
