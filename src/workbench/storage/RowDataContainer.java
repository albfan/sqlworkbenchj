/*
 * RowDataContainer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public interface RowDataContainer
{
	int getRowCount();
	RowData getRow(int rowIndex);
	ResultInfo getResultInfo();
	TableIdentifier getUpdateTable();

	public static class Factory
	{
		public static RowDataContainer createContainer(DataStore data)
		{
			return data;
		}

		public static RowDataContainer createContainer(RowData row, ResultInfo info)
		{
			return new SingleRowDataContainer(row, info);
		}

		public static RowDataContainer createContainer(DataStore data, int row)
		{
			return new SingleRowDataContainer(data.getRow(row), data.getResultInfo());
		}

		public static RowDataContainer createContainer(DataStore data, int[] selectedRows)
		{
			return new SelectionRowDataContainer(data, selectedRows);
		}
	}
}

class SingleRowDataContainer
	implements RowDataContainer
{
	private RowData row;
	private ResultInfo info;

	SingleRowDataContainer(RowData row, ResultInfo info)
	{
		this.row = row;
		this.info = info;
	}

	@Override
	public int getRowCount()
	{
		return 1;
	}

	@Override
	public RowData getRow(int rowIndex)
	{
		if (rowIndex != 0) throw new ArrayIndexOutOfBoundsException(rowIndex);
		return row;
	}

	@Override
	public ResultInfo getResultInfo()
	{
		return info;
	}

	@Override
	public TableIdentifier getUpdateTable()
	{
		return info.getUpdateTable();
	}
}

class SelectionRowDataContainer
	implements RowDataContainer
{
	private DataStore data;
	private int[] selection;

	SelectionRowDataContainer(DataStore data, int[] rows)
	{
		this.data = data;
		this.selection = rows;
	}

	@Override
	public int getRowCount()
	{
		return selection.length;
	}

	@Override
	public RowData getRow(int rowIndex)
	{
		return data.getRow(selection[rowIndex]);
	}

	@Override
	public ResultInfo getResultInfo()
	{
		return data.getResultInfo();
	}

	@Override
	public TableIdentifier getUpdateTable()
	{
		return data.getUpdateTable();
	}
}
