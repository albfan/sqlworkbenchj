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
public class PrevStatementAction extends AbstractAction
{
	private SqlPanel panel;
	
	public PrevStatementAction(SqlPanel aPanel)
	{
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString("PrevStatement"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("PrevStatement"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_UP));
		this.putValue(WbActionConstants.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(WbActionConstants.ADD_TO_TOOLBAR, "true");
	}

	public void actionPerformed(ActionEvent e)
	{
		this.panel.showPrevStatement();
	}
}
