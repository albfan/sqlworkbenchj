/*
 * TabHistoryEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.sql;

/**
 *
 * @author Thomas Kellerer
 */
public class TabHistoryEntry
{
	private final SqlPanel panel;
	private final int originalIndex;

	public TabHistoryEntry(SqlPanel panel, int index)
	{
		this.panel = panel;
		this.originalIndex = index;
	}

	public SqlPanel getPanel()
	{
		return panel;
	}

	public int getOriginalIndex()
	{
		return originalIndex;
	}

	public void dispose()
	{
		if (panel == null) return;
		panel.dispose();
	}
	
}
