/*
 * SpoolDataAction.java
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

import workbench.interfaces.Spooler;
import workbench.resource.ResourceMgr;
import workbench.interfaces.TextSelectionListener;
import workbench.gui.sql.EditorPanel;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *
 */
public class SpoolDataAction
	extends WbAction
	implements TextSelectionListener
{
	private Spooler client;
	private EditorPanel editor;

	public SpoolDataAction(Spooler aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtSpoolData");
		this.setIcon(ResourceMgr.getImage("SpoolData"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.spoolData();
	}

	public void setEditor(EditorPanel ed)
	{
		this.editor = ed;
		this.editor.addSelectionListener(this);
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		if(newEnd > newStart)
		{
			int startLine = this.editor.getSelectionStartLine();
			int endLine = this.editor.getSelectionEndLine();
			this.setEnabled(startLine < endLine);
		}
		else
		{
			this.setEnabled(false);
		}
	}

}