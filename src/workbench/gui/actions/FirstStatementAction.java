/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class FirstStatementAction extends WbAction
{
	private SqlPanel panel;

	public FirstStatementAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtFirstStatement", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ));
		this.setIcon(ResourceMgr.getImage("First"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(true);
		this.setCreateToolbarSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.showFirstStatement();
	}
}
