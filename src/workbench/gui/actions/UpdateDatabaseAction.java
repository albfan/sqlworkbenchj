/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.display.SqlPanel;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class UpdateDatabaseAction extends AbstractAction
{
	private SqlPanel panel;
	
	public UpdateDatabaseAction(SqlPanel aPanel)
	{
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString("UpdateDatabase"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("UpdateDatabase"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(WbActionConstants.TBAR_SEPARATOR, "true");
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.panel.saveChangesToDatabase();
	}
}
