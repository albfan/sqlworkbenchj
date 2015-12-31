/*
 * DataStoreValueProvider.java
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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import workbench.interfaces.ValueProvider;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStoreValueProvider
	implements ValueProvider
{
	private DataStore data;

	public DataStoreValueProvider(DataStore data)
	{
		this.data = data;
	}

	@Override
	public ResultInfo getResultInfo()
	{
		return data.getResultInfo();
	}

	@Override
	public Collection<String> getColumnValues(String columnName)
	{
		int rowCount = data.getRowCount();
		Set<String> result = new TreeSet<>();
		int col = data.getColumnIndex(columnName);
		if (col < 0) return result;

		for (int row=0; row < rowCount; row ++)
		{
			String value = data.getValueAsString(row, columnName);
			if (value != null)
			{
				result.add(value);
			}
		}
		return result;
	}

}
