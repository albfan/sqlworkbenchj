package workbench.gui.actions;

import workbench.interfaces.ClipboardSupport;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.MainWindow;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class UndoExpandAction extends WbAction
{
	private SqlPanel client;

	public UndoExpandAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtUndoExpand"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtUndoExpand"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_VIEW);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		this.putValue(Action.SMALL_ICON, null);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.undoExpand();
	}
}
