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
public class PrevStatementAction extends WbAction
{
	private SqlPanel panel;

	public PrevStatementAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.initMenuDefinition("MnuTxtPrevStatement", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK));
		this.setIcon(ResourceMgr.getImage("Back"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(false);
		this.setCreateToolbarSeparator(false);
		//this.putValue(ALTERNATE_ACCELERATOR, KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
	}

	public void executeAction(ActionEvent e)
	{
		this.panel.showPrevStatement();
	}
}
