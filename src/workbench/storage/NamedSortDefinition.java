/*
 * NamedSortDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.List;

import workbench.db.QuoteHandler;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to save the sort definition for a DataStoreTableModel.
 * The sorted columns are saved by name, not by index position.
 *
 * @author Thomas Kellerer
 */
public class NamedSortDefinition
{
	private String[] sortColumns;
	private boolean[] sortAscending;
	private boolean ignoreCase;
  private boolean naturalSort;

	public NamedSortDefinition(String[] columnNames, boolean[] ascending)
	{
		sortColumns = columnNames;
		sortAscending = ascending;
	}

	public NamedSortDefinition(DataStore data, SortDefinition sort)
	{
		if (sort != null && sort.hasColumns())
		{
			sortColumns = new String[sort.getColumnCount()];
			sortAscending = new boolean[sort.getColumnCount()];

			int totalColumns = data.getColumnCount();
			for (int i=0; i < sort.getColumnCount(); i++)
			{
				int dataColumn = sort.getSortColumnByIndex(i);
				if (dataColumn > -1 && dataColumn < totalColumns)
				{
					sortColumns[i] = data.getColumnName(dataColumn);
					sortAscending[i] = sort.isSortAscending(dataColumn);
				}
				else
				{
					// invalid definition
					sortColumns = null;
					sortAscending = null;
					break;
				}
			}
			ignoreCase = sort.getIgnoreCase();
      naturalSort = sort.useNaturalSort();
		}
	}

	public int getColumnCount()
	{
		if (sortColumns == null) return 0;
		return sortColumns.length;
	}

  public void setUseNaturalSort(boolean flag)
  {
    naturalSort = flag;
  }
  
  public boolean useNaturalSort()
  {
    return naturalSort;
  }

	public boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	public void setIgnoreCase(boolean flag)
	{
		this.ignoreCase = flag;
	}

	/**
	 * Return a column-index based SortDefinition for the given DataStore.
	 *
	 * If not all column names are found in the datastore an "empty" sort is returned.
	 *
	 * @param data  the DataStore for which the named sort should be applied
	 * @return the real sort definition to be used. Never null
	 *
	 */
	public SortDefinition getSortDefinition(DataStore data)
	{
		if (sortColumns == null) return new SortDefinition();

		int[] columns = new int[sortColumns.length];
		for (int i=0; i < sortColumns.length; i++)
		{
			int index = data.getColumnIndex(sortColumns[i]);
			if (index < 0) return new SortDefinition();
			columns[i] = index;
		}

		SortDefinition sort = new SortDefinition(columns, sortAscending);
		sort.setIgnoreCase(ignoreCase);
    sort.setUseNaturalSort(naturalSort);
		return sort;
	}

	/**
	 * Returns a SQL ORDER BY expression reflecting this sort definition that can be used in a SQL statement.
	 *
	 * @param quoter  the QuoteHandler to be used for quoting the column names
	 * @return an ORDER BY expression or null if no sort is defined
	 */
	public String getSqlExpression(QuoteHandler quoter)
	{
		if (sortColumns == null || sortColumns.length == 0) return null;
		StringBuilder result = new StringBuilder(sortColumns.length * 20);
		for (int i=0; i < sortColumns.length; i++)
		{
			if (i > 0)
			{
				result.append(',');
			}
			result.append(quoter.quoteObjectname(sortColumns[i]));
			if (sortAscending[i])
			{
				result.append(" ASC");
			}
			else
			{
				result.append(" DESC");
			}
		}
		return result.toString();
	}

	public String getDefinitionString()
	{
		if (sortColumns == null || sortColumns.length == 0) return "";
		StringBuilder result = new StringBuilder(sortColumns.length * 10);
		for (int i=0; i < sortColumns.length; i++)
		{
			if (i > 0) result.append(',');
			result.append('"');
			result.append(sortColumns[i]);
			result.append(';');
			result.append(sortAscending[i] ? 'a' : 'd');
			result.append('"');
}
		return result.toString();
	}

	public static NamedSortDefinition parseDefinitionString(String definition)
	{
		List<String> elements = StringUtil.stringToList(definition, ",", true, true, false);
		if (CollectionUtil.isEmpty(elements)) return null;

		String[] columns = new String[elements.size()];
		boolean[] ascending = new boolean[elements.size()];
		for (int i=0; i < elements.size(); i++)
		{
			String element = StringUtil.trimQuotes(elements.get(i));
			int pos = element.indexOf(';');
			if (pos == -1)
			{
				pos = element.indexOf(':');
			}
			if (pos > -1)
			{
				String colname = element.substring(0, pos);
				String asc = element.substring(pos + 1);
				columns[i] = colname;
				ascending[i] = asc.toLowerCase().startsWith("a");
			}
			else
			{
				columns[i] = element;
				ascending[i] = true;
			}
		}
		return new NamedSortDefinition(columns, ascending);
	}

}
