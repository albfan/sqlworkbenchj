package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class SaveSqlHistoryAction extends WbAction
{
	private SqlPanel client;

	public SaveSqlHistoryAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtSaveSqlHistory"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtSaveSqlHistory"));
		//this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.saveSqlStatementHistory();
	}
}
