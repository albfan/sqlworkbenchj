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
public class LastStatementAction extends WbAction
{
	private SqlPanel panel;

	public LastStatementAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtLastStatement", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ));
		this.setIcon(ResourceMgr.getImage("Last"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(false);
		this.setCreateToolbarSeparator(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.showLastStatement();
	}
}
