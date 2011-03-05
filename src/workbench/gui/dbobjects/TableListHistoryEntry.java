/*
 * TableListHistoryEntry
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects;

import workbench.db.TableIdentifier;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableListHistoryEntry
{

	private TableIdentifier selectedTable;
	private String regexFilter;
	private int selectedDetailTab;

	public TableListHistoryEntry(TableIdentifier tbl, String filter, int selectedTab)
	{
		selectedTable = tbl;
		if (StringUtil.isNonBlank(filter))
		{
			regexFilter = filter;
		}
		selectedDetailTab = selectedTab;
	}

	public String getRegexFilter()
	{
		return regexFilter;
	}

	public int getSelectedTab()
	{
		return selectedDetailTab;
	}

	public TableIdentifier getSelectedTable()
	{
		return selectedTable;
	}

}
