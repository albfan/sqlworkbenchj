/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.ClipboardSupport;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  sql.workbench@freenet.de
 */
public class NextStatementAction extends WbAction
{
	private SqlPanel panel;
	public NextStatementAction(SqlPanel aPanel)
	{
		super();
		this.panel = aPanel;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtNextStatement"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtNextStatement"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage(ResourceMgr.IMG_DOWN));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(true);
		this.setCreateToolbarSeparator(true);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.panel.showNextStatement();
	}
}
