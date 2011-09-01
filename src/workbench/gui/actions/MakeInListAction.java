/*
 * MakeInListAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.editor.CodeTools;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

/**
 * Make an "IN" List
 * @see workbench.gui.editor.CodeTools#makeInListForChar()
 * @author  Thomas Kellerer
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

	@Override
	public void executeAction(ActionEvent e)
	{
		CodeTools tools = new CodeTools(client);
		tools.makeInListForChar();
	}

	@Override
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
