/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopySelectedAsSqlInsertAction extends WbAction
{
	private WbTable client;
	
	public CopySelectedAsSqlInsertAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopySelectedAsSqlInsert", null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		this.setIcon(null);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		client.copyAsSqlInsert(true);
	}

}
