/*
 * RenameGroupAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import workbench.gui.actions.WbAction;
import workbench.log.LogMgr;

/**
 * @author support@sql-workbench.net
 */
public class RenameGroupAction
	extends WbAction
	implements TreeSelectionListener
{
	private ProfileTree client;
	
	public RenameGroupAction(ProfileTree panel)
	{
		this.client = panel;
		this.client.addTreeSelectionListener(this);
		this.initMenuDefinition("LblRenameProfileGroup");
	}
	
	public void executeAction(ActionEvent e)
	{
		client.renameGroup();
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		this.setEnabled(client.onlyGroupSelected());
	}
	
}
