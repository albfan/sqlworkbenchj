/*
 * MakeInListAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  info@sql-workbench.net
 */
public class MakeInListAction
	extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public MakeInListAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.client.addSelectionListener(this);
		this.initMenuDefinition("MnuTxtMakeCharInList");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.makeInListForChar();
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		if(newEnd > newStart)
		{
			int startLine = this.client.getSelectionStartLine();
			int endLine = this.client.getSelectionEndLine();
			this.setEnabled(startLine < endLine);
		}
		else
		{
			this.setEnabled(false);
		}
	}

}