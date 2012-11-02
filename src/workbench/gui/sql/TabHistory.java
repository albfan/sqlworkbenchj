/*
 * TabHistory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.sql;

import workbench.util.FixedSizeList;

/**
 *
 * @author Thomas Kellerer
 */
public class TabHistory
{
	private FixedSizeList<TabHistoryEntry> history;

	public TabHistory()
	{
		this(10);
	}

	public TabHistory(int maxSize)
	{
		history = new FixedSizeList<TabHistoryEntry>(maxSize);
	}

	public void addToHistory(SqlPanel panel, int index)
	{
		if (panel == null) return;
		TabHistoryEntry entry = new TabHistoryEntry(panel, index);
		history.add(entry);
	}

	public int getHistorySize()
	{
		return history.size();
	}
}
