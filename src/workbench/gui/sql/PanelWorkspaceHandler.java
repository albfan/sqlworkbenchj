/*
 * PanelWorkspaceHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.io.IOException;
import java.util.Properties;
import workbench.log.LogMgr;
import workbench.util.WbProperties;
import workbench.util.WbWorkspace;

/**
 *
 * @author support@sql-workbench.net
 */
public class PanelWorkspaceHandler
{
	private SqlPanel client;

	public PanelWorkspaceHandler(SqlPanel panel)
	{
		client = panel;
	}

	public void readFromWorkspace(WbWorkspace w, int index)
	{
		if (client.hasFileLoaded())
		{
			client.closeFile(true, false);
		}
		client.reset();

		try
		{
			w.readHistoryData(index, client.sqlHistory);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlPanel.readFromWorkspace()", "Could not read history data for index=" + index);
			client.clearSqlHistory(false);
		}

		String filename = w.getExternalFileName(index);
		client.setTabName(w.getTabTitle(index));

		int v = w.getMaxRows(index);
		client.statusBar.setMaxRows(v);
		v = w.getQueryTimeout(index);
		client.statusBar.setQueryTimeout(v);

		boolean fileLoaded = false;
		if (filename != null)
		{
			String encoding = w.getExternalFileEncoding(index);
			fileLoaded = client.readFile(filename, encoding);
		}

		if (!fileLoaded)
		{
			try
			{
				client.sqlHistory.showCurrent();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			int cursorPos = w.getExternalFileCursorPos(index);
			if (cursorPos > -1 && cursorPos < client.editor.getText().length()) client.editor.setCaretPosition(cursorPos);
		}

		WbProperties props = w.getSettings();

		int loc = props.getIntProperty("tab" + (index) + ".divider.location", 200);
		client.contentPanel.setDividerLocation(loc);
		loc = props.getIntProperty("tab" + (index) + ".divider.lastlocation", 0);
		if (loc > 0) client.contentPanel.setLastDividerLocation(loc);

		boolean appendResults = props.getBoolProperty("tab" + (index) + ".append.results", false);
		boolean locked = props.getBoolProperty("tab" + (index) + ".locked", false);
		client.setLocked(locked);
		client.setAppendResults(appendResults);
		client.updateAppendAction();
		client.updateTabTitle();
		client.editor.clearUndoBuffer();
		client.editor.resetModified();

	}


	public void saveToWorkspace(WbWorkspace w, int index)
		throws IOException
	{
		client.saveHistory(w);
		Properties props = w.getSettings();

		int location = client.contentPanel.getDividerLocation();
		int last = client.contentPanel.getLastDividerLocation();
		props.setProperty("tab" + (index) + ".divider.location", Integer.toString(location));
		props.setProperty("tab" + (index) + ".divider.lastlocation", Integer.toString(last));
		props.setProperty("tab" + (index) + ".append.results", Boolean.toString(client.getAppendResults()));
		props.setProperty("tab" + (index) + ".locked", Boolean.toString(client.isLocked()));

		w.setMaxRows(index, client.statusBar.getMaxRows());
		w.setQueryTimeout(index, client.statusBar.getQueryTimeout());
		if (client.hasFileLoaded())
		{
			w.setExternalFileName(index, client.getCurrentFileName());
			w.setExternalFileCursorPos(index, client.editor.getCaretPosition());
			w.setExternalFileEncoding(index, client.editor.getCurrentFileEncoding());
		}
		if (client.getTabName() != null)
		{
			w.setTabTitle(index, client.getTabName());
		}
		if (client.hasFileLoaded() && client.editor.isModified())
		{
			client.editor.saveCurrentFile();
		}
	}

}
