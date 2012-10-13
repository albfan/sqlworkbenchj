/*
 * TableListSorter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import workbench.storage.RowData;
import workbench.storage.RowDataListSorter;
import workbench.storage.SortDefinition;

/**
 *
 * @author Thomas Kellerer
 */
public class TableListSorter
	extends RowDataListSorter
{
	private static final String TABLE_TYPE = "TABLE";
	private boolean mviewAsTable = false;

	public TableListSorter(SortDefinition sortDef)
	{
		super(sortDef);
	}

	public TableListSorter(int column, boolean ascending)
	{
		super(column, ascending);
	}

	public TableListSorter(int[] columns, boolean[] order)
	{
		super(columns, order);
	}

	public void setSortMViewAsTable(boolean flag)
	{
		this.mviewAsTable = flag;
	}


	@Override
	protected int compareColumn(int column, RowData row1, RowData row2)
	{
		if (mviewAsTable && column == DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE)
		{
			String value1 = (String)row1.getValue(column);
			String value2 = (String)row2.getValue(column);
			if (value1.equals(DbMetadata.MVIEW_NAME) && value2.equals(TABLE_TYPE)) return 0;
			if (value1.equals(TABLE_TYPE) && value2.equals(DbMetadata.MVIEW_NAME)) return 0;
		}
		return super.compareColumn(column, row1, row2);
	}
}
