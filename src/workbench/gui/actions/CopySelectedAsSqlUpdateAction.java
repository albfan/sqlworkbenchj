package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of the data as SQL update statements into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopySelectedAsSqlUpdateAction extends WbAction
{
	private WbTable client;
	
	public CopySelectedAsSqlUpdateAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopySelectedAsSqlUpdate",null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		this.setIcon(null);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		client.copyAsSqlUpdate(true);
	}

}