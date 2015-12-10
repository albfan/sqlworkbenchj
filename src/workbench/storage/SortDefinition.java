/*
 * SortDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.Arrays;
import java.util.List;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to store the sort definition of a result set (e.g. DataStore,
 * DatastoreTableModel)
 *
 * @author Thomas Kellerer
 */
public class SortDefinition
{
	private boolean[] sortAscending;
	private int[] sortColumns;
	private boolean ignoreCase;
  private boolean naturalSort;

	public SortDefinition()
	{
	}

	public SortDefinition(int column, boolean ascending)
	{
		sortColumns = new int[] { column };
		sortAscending = new boolean[] { ascending };
	}

	public SortDefinition(int[] columns, boolean[] ascending)
	{
		sortColumns = new int[columns.length];
		sortAscending = new boolean[ascending.length];
		System.arraycopy(columns, 0, sortColumns, 0, columns.length);
		System.arraycopy(ascending, 0, sortAscending, 0, ascending.length);
	}

  public boolean useNaturalSort()
  {
    return naturalSort;
  }

  public void setUseNaturalSort(boolean flag)
  {
    this.naturalSort = flag;
  }

	public void setIgnoreCase(boolean ignoreCase)
	{
		this.ignoreCase = ignoreCase;
	}

	public boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	/**
	 * Create a copy of this sort definition.
	 *
	 * @return a new SortDefinition with the same settings as <tt>this</tt>
	 */
	public SortDefinition createCopy()
	{
		SortDefinition copy = new SortDefinition();
		if (sortColumns != null)
		{
			copy.sortColumns = new int[sortColumns.length];
			System.arraycopy(this.sortColumns, 0, copy.sortColumns, 0, this.sortColumns.length);
			copy.sortAscending = new boolean[sortAscending.length];
			System.arraycopy(this.sortAscending, 0, copy.sortAscending, 0, this.sortAscending.length);
		}
		copy.ignoreCase = this.ignoreCase;
		return copy;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 67 * hash + (this.sortAscending != null ? Arrays.hashCode(this.sortAscending) : 0);
		hash = 67 * hash + (this.sortColumns != null ? Arrays.hashCode(this.sortColumns) : 0);
		return hash;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof SortDefinition)
		{
			SortDefinition sd = (SortDefinition)other;
			if (sd.getColumnCount() != this.getColumnCount()) return false;
			if (getColumnCount() == 0) return true;
			for (int i=0; i < sortColumns.length; i++)
			{
				if (sortColumns[i] != sd.sortColumns[i]) return false;
				if (sortAscending[i] != sd.sortAscending[i]) return false;
			}
			return true;
		}
		return false;
	}

	public boolean isValid()
	{
		if (isEmpty()) return true;
		for (int colIndex : sortColumns)
		{
			if (colIndex < 0) return false;
		}
		return true;
	}

	public boolean isEmpty()
	{
		return getColumnCount() == 0;
	}

	public boolean hasColumns()
	{
		return getColumnCount() != 0;
	}

	/**
	 * Returns the numer of sort columns defined.
	 * @return the count of sort columns
	 */
	public int getColumnCount()
	{
		if (this.sortColumns == null) return 0;
		return sortColumns.length;
	}

	/**
	 * Return the column index from the result set based on the
	 * index in the column list (the primary sort column has
	 * index 0)
	 *
	 * @param definitionIndex the index in the list of columns
	 * @return the column index in the result set or -1 if no sort columns are defined
	 */
	public int getSortColumnByIndex(int definitionIndex)
		throws ArrayIndexOutOfBoundsException
	{
		if (sortColumns == null) return -1;
		return sortColumns[definitionIndex];
	}

	/**
	 * Return true if the data is sorted in ascending order for the given column.
	 * @param col the column index in the result set
	 * @return True if sorted in ascending order
	 */
	public boolean isSortAscending(int col)
	{
		int index = findSortColumnIndex(col);
		if (index < 0) return false;
		return sortAscending[index];
	}


	/**
	 * Check if the given column is the first column in the sort definition
	 * @param col the column index in the result set
	 * @return true if the column was found and if it's the first column
	 */
	public boolean isPrimarySortColumn(int col)
	{
		int index = findSortColumnIndex(col);
		return (index == 0);
	}

	/**
	 * Check if the table is sorted by a column
	 * @return true if the given column is a sort column
	 * @see #isSortAscending(int)
	 */
	public boolean isSortColumn(int col)
	{
		int index = findSortColumnIndex(col);
		return (index > -1);
	}

	/**
	 * Remove the specified sort column from this definition.
	 * If the column is not found, nothing is changed.
	 *
	 * @param column the column index in the result set
	 */
	public void removeSortColumn(int column)
	{
		int index = findSortColumnIndex(column);
		if (index < 0) return;

		if (this.sortColumns.length == 1)
		{
			this.sortColumns = null;
			this.sortAscending = null;
			return;
		}

		int[] newColumns = new int[sortColumns.length-1];
		boolean[] newDir = new boolean[sortColumns.length-1];

		// Copy the old values before the removed column
		System.arraycopy(sortColumns, 0, newColumns, 0, index);
		System.arraycopy(sortAscending, 0, newDir, 0, index);

		// Copy the old values after the removed column
		System.arraycopy(sortColumns, index + 1, newColumns, index, sortColumns.length - index - 1);
		System.arraycopy(sortAscending, 0, newDir, index, sortColumns.length - index - 1);

		this.sortColumns = newColumns;
		this.sortAscending = newDir;
	}

	/**
	 * Add a column to the list of sort columns.
	 * If sort columns exist, the new column is added at the end, otherwise
	 * the column will be the primary sort column.
	 *
	 * @param column the column index (inside the result set) to be added
	 * @param ascending true if sorting should be ascending
	 */
	public void addSortColumn(int column, boolean ascending)
	{
		setSortColumn(column, ascending, true);
	}

	/**
	 * Define a single column as the sort column.
	 * If sort columns exist, they are reqplaced with the given column
	 *
	 * @param column the column index (inside the result set) to be set
	 * @param ascending true if sorting should be ascending
	 */
	public void setSortColumn(int column, boolean ascending)
	{
		setSortColumn(column, ascending, false);
	}

	private void setSortColumn(int column, boolean ascending, boolean addSortColumn)
	{
		if (!addSortColumn || sortColumns == null)
		{
			this.sortColumns = new int[] { column };
			this.sortAscending = new boolean[] { ascending };
		}
		else
		{
			int index = findSortColumnIndex(column);

			if (index < 0)
			{
				// No definition for this column found, add the column to the list of columns
				int[] newColumns = new int[sortColumns.length + 1];
				boolean[] newDir = new boolean[sortColumns.length + 1];

				System.arraycopy(sortColumns, 0, newColumns, 0, sortColumns.length);
				System.arraycopy(sortAscending, 0, newDir, 0, sortColumns.length);

				newColumns[sortColumns.length] = column;
				newDir[sortColumns.length] = ascending;

				this.sortColumns = newColumns;
				this.sortAscending = newDir;
			}
			else
			{
				// if this column already existed, simply store the new sort order
				this.sortAscending[index] = ascending;
			}
		}
	}

	/**
	 * Finds the index of a column in the internal array
	 * based on the index of the result set
	 *
	 * @param column
	 * @return -1 if the column was not found. The index in sortColumns otherwise
	 */
	private int findSortColumnIndex(int column)
	{
		if (this.sortColumns == null) return -1;
		for (int i = 0; i < this.sortColumns.length; i++)
		{
			if (sortColumns[i] == column) return i;
		}
		return -1;
	}

	public String getDefinitionString()
	{
		if (sortColumns == null || sortColumns.length == 0) return "";
		StringBuilder result = new StringBuilder(sortColumns.length * 3);
		for (int i=0; i < sortColumns.length; i++)
		{
			if (i > 0) result.append(';');
			result.append(Integer.toString(sortColumns[i]));
			result.append(',');
			result.append(sortAscending[i] ? 'a' : 'd');
		}
		return result.toString();
	}

	public static SortDefinition parseDefinitionString(String definition)
	{
		List<String> elements = StringUtil.stringToList(definition, ";", true, true, false);
		if (CollectionUtil.isEmpty(elements)) return null;

		int[] columns = new int[elements.size()];
		boolean[] ascending = new boolean[elements.size()];
		for (int i=0; i < elements.size(); i++)
		{
			String element = StringUtil.trimQuotes(elements.get(i));
			String[] parts = element.split(",");
			if (parts != null && parts.length == 2)
			{
				int colNr = StringUtil.getIntValue(parts[0], -1);

				if (colNr < 0) return null; // something wrong, assume the whole definition is bogus

				columns[i] = colNr;
				ascending[i] = "a".equalsIgnoreCase(parts[1]);
			}
		}
		return new SortDefinition(columns, ascending);
	}

}
