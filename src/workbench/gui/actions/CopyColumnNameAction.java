/*
 * CopyAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import workbench.gui.components.WbTable;
import workbench.util.StringUtil;

/**
 *	Action to copy the contents of an entry field into the clipboard
 * 
 *	@author  support@sql-workbench.net
 */
public class CopyColumnNameAction
	extends WbAction
{
	private WbTable client;

	public CopyColumnNameAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		isConfigurable = false;
		initMenuDefinition("MnuTxtCopyColName");
		removeIcon();
	}

	public void executeAction(ActionEvent e)
	{
		int col = client.getPopupColumnIndex();
		if (col > -1)
		{
			String name = client.getColumnName(col);
			if (StringUtil.isNonBlank(name))
			{
				Clipboard clipboard = client.getToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(name),null);
			}
		}
	}
}
