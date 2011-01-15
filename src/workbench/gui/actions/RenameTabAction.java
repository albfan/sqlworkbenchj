/*
 * RenameTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.RenameableTab;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class RenameTabAction
	extends WbAction
	implements ChangeListener
{
	private RenameableTab client;

	public RenameTabAction(RenameableTab aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRenameTab");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		client.addTabChangeListener(this);
		setEnabled(client.canRenameTab());
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		String oldName = client.getCurrentTabTitle();
		String newName = WbSwingUtilities.getUserInput(client.getComponent(), ResourceMgr.getString("MsgEnterNewTabName"), oldName);
		
		if (newName != null)
		{
			client.setCurrentTabTitle(newName);
		}
	}

	public void stateChanged(ChangeEvent e)
	{
		setEnabled(client.canRenameTab());
	}
}
