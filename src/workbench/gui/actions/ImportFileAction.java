package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.Action;
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
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtImportFile"));
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtImportFile"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.client.importFile();
	}
}
