package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class ImportFileAction extends WbAction
{
	private SqlPanel client;

	public ImportFileAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtImportFile");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.importFile();
	}
}
