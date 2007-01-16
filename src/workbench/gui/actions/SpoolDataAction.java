/*
 * SpoolDataAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;
import workbench.interfaces.TextSelectionListener;
import workbench.gui.sql.EditorPanel;

/**
 *	@author  support@sql-workbench.net
 */
public class SpoolDataAction
	extends WbAction
	implements TextSelectionListener
{
	private Exporter client;
	private EditorPanel editor;

	public SpoolDataAction(Exporter aClient)
	{
		this(aClient, "MnuTxtSpoolData");
	}
	public SpoolDataAction(Exporter aClient, String msgKey)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition(msgKey);
		this.setIcon(ResourceMgr.getImage("SpoolData"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.exportData();
	}

	public void setEditor(EditorPanel ed)
	{
		this.editor = ed;
		this.editor.addSelectionListener(this);
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}

}
