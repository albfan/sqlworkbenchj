/*
 * DataStoreValueProvider.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
		Set<String> result = new TreeSet<String>();
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
