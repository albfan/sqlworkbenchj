package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.FormattableSql;

import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class FormatSqlAction extends WbAction
{
	private FormattableSql client;

	public FormatSqlAction(FormattableSql aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtReformatSql"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("format"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_SQL);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtReformatSql"));
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		this.setCreateToolbarSeparator(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.reformatSql();
	}
}
