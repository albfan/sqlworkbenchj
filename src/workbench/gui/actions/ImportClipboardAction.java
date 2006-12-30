/*
 * ImportClipboardAction.java
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.gui.components.WbTable;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.WbStringTokenizer;

/**
 * @author support@sql-workbench.net
 */
public class ImportClipboardAction
	extends WbAction
{
	private SqlPanel client;
	private WbTable dataTable;
	
	public ImportClipboardAction(SqlPanel panel)
	{
		this.initMenuDefinition("MnuTxtImportClip");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		client = panel;
		this.setEnabled(false);
	}
	
	public void setTargetTable(WbTable table)
	{
		this.dataTable = table;
		this.setEnabled(table != null);
	}
	
	public boolean hasCtrlModifier() { return true; }
	
	public void executeAction(ActionEvent evt)
	{
		String content = getClipboardContents();
		if (content == null) return;
		client.importString(content, isCtrlPressed(evt));
	}
	
	private String getClipboardContents()
	{
		if (client == null) return null;
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
