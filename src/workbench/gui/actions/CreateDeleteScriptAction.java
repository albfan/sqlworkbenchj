package workbench.gui.actions;

import java.awt.event.ActionEvent;

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
		this.initMenuDefinition("MnuTxtCreateDeleteScript", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.generateDeleteScript();
	}
}
