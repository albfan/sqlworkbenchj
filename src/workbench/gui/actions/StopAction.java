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
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  thomas.kellerer@web.de
 */
public class StopAction extends AbstractAction
{
	private SqlPanel panel;
	
	public StopAction(SqlPanel aPanel)
	{
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString(ResourceMgr.TXT_STOP_STMT));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription(ResourceMgr.TXT_STOP_STMT));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_STOP));
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbActionConstants.ADD_TO_TOOLBAR, "true");
		this.putValue(WbActionConstants.MENU_SEPARATOR, "true");
	}

	public void actionPerformed(ActionEvent e)
	{
		this.panel.cancelExecution();
	}
}
