/*
 * CopyColumnNameAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import workbench.gui.components.WbTable;
import workbench.resource.Settings;

/**
 * Action to copy the names of all columns into the clipboard
 *
 * @author Andreas Krist
 */
public class CopyAllColumnNamesAction
	extends WbAction
{
	private static final long serialVersionUID = 5433513843703540824L;
	private WbTable client;

	public CopyAllColumnNamesAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		isConfigurable = false;
		initMenuDefinition("MnuTxtCopyAllColNames");
		removeIcon();
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		int columnCount = client.getColumnCount();
		if (columnCount > 0)
		{
			boolean spaceAfterComma = Settings.getInstance().getFormatterAddSpaceAfterComma();

			StringBuilder columnNames = new StringBuilder();

			for (int i = 0; i < columnCount; i++)
			{
				String columnName = client.getColumnName(i);
				columnNames.append(columnName);
				if (i + 1 != columnCount)
				{
					columnNames.append(",");
					if (spaceAfterComma)
					{
						columnNames.append(" ");
					}
				}
			}

			Clipboard clipboard = client.getToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(columnNames.toString()),null);
		}
	}
}
