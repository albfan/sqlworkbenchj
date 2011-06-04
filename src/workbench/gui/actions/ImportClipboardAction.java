/*
 * ImportClipboardAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * Import data from the clipboard into a table
 *
 * @author Thomas Kellerer
 */
public class ImportClipboardAction
	extends WbAction
{
	private SqlPanel client;

	public ImportClipboardAction(SqlPanel panel)
	{
		super();
		this.initMenuDefinition("MnuTxtImportClip");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		client = panel;
		this.setEnabled(false);
	}

	@Override
	public boolean hasCtrlModifier()
	{
		return true;
	}

	@Override
	public void executeAction(ActionEvent evt)
	{
		String content = getClipboardContents();
		if (content == null) return;
		client.importString(content, isCtrlPressed(evt));
	}

	private String getClipboardContents()
	{
		if (client == null)
		{
			return null;
		}
		Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = clp.getContents(client);
		try
		{
			String s = (String) content.getTransferData(DataFlavor.stringFlavor);
			return s;
		}
		catch (Throwable e)
		{
			LogMgr.logError("ImportClipboardAction.checkContents()", "Error accessing clipboard", e);
		}
		return null;
	}
}
