package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.components.WbTable;
import workbench.gui.sql.DwPanel;

import workbench.interfaces.Commitable;
import workbench.resource.ResourceMgr;

/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class SelectKeyColumnsAction extends WbAction
{
	private DwPanel client;
	
	public SelectKeyColumnsAction(DwPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSelectKeyColumns");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(ResourceMgr.getBlankImage());
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		if (this.client != null) 
		{
			this.client.checkAndSelectKeyColumns();
		}
	}
	
}
