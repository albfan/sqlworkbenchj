/*
 * NamedSortDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
	
	public NamedSortDefinition(DataStore data, SortDefinition sort)
	{
		if (sort != null && sort.hasColumns())
		{
			sortColumns = new String[sort.getColumnCount()];
			sortAscending = new boolean[sort.getColumnCount()];
			for (int i=0; i < sort.getColumnCount(); i++)
			{
				int dataColumn = sort.getSortColumnByIndex(i);
				sortColumns[i] = data.getColumnName(dataColumn);
				sortAscending[i] = sort.isSortAscending(dataColumn);
			}
		}
	}
	
	public SortDefinition getSortDefinition(DataStore data)
	{
		if (sortColumns == null) return new SortDefinition();
		
		int[] columns = new int[sortColumns.length];
		for (int i=0; i < sortColumns.length; i++)
		{
			columns[i] = data.getColumnIndex(sortColumns[i]);
		}	
		return new SortDefinition(columns, sortAscending);
	}
	
}
