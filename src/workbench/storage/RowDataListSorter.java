/*
 * RowDataListSorter.java
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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * A class to sort a RowDataList
 * @author  Thomas Kellerer
 */
public class RowDataListSorter
	implements Comparator<RowData>
{
	private SortDefinition definition;
	private Collator defaultCollator;
	private boolean ignoreCase;

	public RowDataListSorter(SortDefinition sortDef)
	{
		this.definition = sortDef.createCopy();
		initCollator();
	}

	public RowDataListSorter(int column, boolean ascending)
	{
		this.definition = new SortDefinition(column, ascending);
		initCollator();
	}

	public RowDataListSorter(int[] columns, boolean[] order)
	{
		if (columns.length != order.length) throw new IllegalArgumentException("Size of arrays must match");
		this.definition = new SortDefinition(columns, order);
		initCollator();
	}

	public void setIgnoreCase(boolean flag)
	{
		this.ignoreCase = flag;
	}

	private void initCollator()
	{
		// Using a Collator to compare Strings is much slower then
		// using String.compareTo() so by default this is disabled
		Locale l = Settings.getInstance().getSortLocale();
		if (l != null)
		{
			defaultCollator = Collator.getInstance(l);
		}
		else
		{
			defaultCollator = null;
		}
	}

	public void sort(RowDataList data)
	{
		data.sort(this);
	}

	/**
	 * Compares the defined sort column
	 */
	@SuppressWarnings("unchecked")
	protected int compareColumn(int column, RowData row1, RowData row2)
	{
		Object o1 = row1.getValue(column);
		Object o2 = row2.getValue(column);

		if  (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{
			return 1;
		}
		else if (o2 == null)
		{
			return -1;
		}

		// Special handling for String columns
		if (defaultCollator != null)
		{
			if (o1 instanceof String && o2 instanceof String)
			{
				return defaultCollator.compare(o1, o2);
			}
		}

		if (ignoreCase && (o1 instanceof String && o2 instanceof String))
		{
			return ((String)o1).compareToIgnoreCase((String)o2);
		}

		int result = 0;
		try
		{
			result = ((Comparable)o1).compareTo(o2);
		}
		catch (Throwable e)
		{
			// If one of the objects did not implement
			// the comparable interface, we'll use the
			// toString() values to compare them
			String v1 = o1.toString();
			String v2 = o2.toString();
			result = v1.compareTo(v2);
		}
		return result;
	}

	@Override
	public int compare(RowData row1, RowData row2)
	{
		if (this.definition == null) return 0;

		try
		{
			int colIndex = 0;
			int result = 0;
			int numCols = this.definition.getColumnCount();

			while (result == 0 && colIndex < numCols)
			{
				int column = definition.getSortColumnByIndex(colIndex);
				if (column < 0) break;

				result = compareColumn(column, row1, row2);
				boolean ascending = definition.isSortAscending(column) ;
				result = ascending ? result : -result;
				colIndex ++;
			}
			return result;
		}
		catch (Exception e)
		{
			LogMgr.logError("RowDataListSorter.compare()", "Error when comparing rows", e);
		}
		return 0;
	}

}
