package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class CreateDeleteScriptAction extends WbAction
{
	private SqlPanel client;

	public CreateDeleteScriptAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtCreateDeleteScript"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtCreateDeleteScript"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.generateDeleteScript();
	}
}
