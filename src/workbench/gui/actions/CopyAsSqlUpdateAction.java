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
 *	Action to copy the contents of the data as SQL update statements into the clipboard
 *	@author  workbench@kellerer.org
 */
public class CopyAsSqlUpdateAction extends WbAction
{
	private WbTable client;

	public CopyAsSqlUpdateAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyAsSqlUpdate",null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
	}

	public void executeAction(ActionEvent e)
	{
		client.copyAsSqlUpdate();
	}

}