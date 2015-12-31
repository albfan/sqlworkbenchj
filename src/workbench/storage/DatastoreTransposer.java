/*
 * DatastoreTransposer.java
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

import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.resource.ResourceMgr;

import workbench.util.Alias;
import workbench.util.CollectionUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;

/**
 * A class to turn the columns of a datastore into rows.
 *
 * @author Thomas Kellerer
 */
public class DatastoreTransposer
{
	private DataStore source;
	private String resultName;
	private final Set<String> excludeColumns = CollectionUtil.caseInsensitiveSet();

	public DatastoreTransposer(DataStore sourceData)
	{
		this.source = sourceData;
		retrieveResultName();
	}

	public void setColumnsToExclude(Collection<String> toExclude)
	{
		excludeColumns.clear();
		for (String colname : toExclude)
		{
			if (source.getColumnIndex(colname) > -1)
			{
				excludeColumns.add(colname);
			}
		}
	}

	private void retrieveResultName()
	{
		if (source == null)
		{
			resultName = "";
			return;
		}
		resultName = source.getResultName();
		if (resultName == null)
		{
			String sql = source.getGeneratingSql();
			List<Alias> tables = SqlUtil.getTables(sql, false, source.getOriginalConnection());
			if (tables.size() == 1)
			{
				resultName = tables.get(0).getObjectName();
			}
		}
	}

	public DataStore transposeRows(int[] rows)
	{
		if (rows == null || rows.length == 0) return null;

		String[] columns = new String[rows.length + 1];
		int[] types = new int[rows.length + 1];

		columns[0] = ResourceMgr.getString("TxtColumnName");
		types[0] = Types.VARCHAR;

		for (int i=0; i < rows.length; i++)
		{
			columns[i+1] = ResourceMgr.getString("TxtRow") + " " + NumberStringCache.getNumberString(rows[i] + 1);
			types[i+1] = Types.VARCHAR;
		}

		DataStore ds = new DataStore(columns, types);

		int colCount = source.getColumnCount();
		for (int i=0; i < colCount  - excludeColumns.size(); i++)
		{
			ds.addRow();
		}

		for (int ix=0; ix < rows.length; ix++)
		{
			int sourceRow = rows[ix];
			int colRow = 0;

			for (int col=0; col < colCount; col ++)
			{
				String colDisplay = source.getColumnDisplayName(col);
				if (!excludeColumns.contains(colDisplay))
				{
					ds.setValue(colRow, 0, colDisplay);
					ds.setValue(colRow, 1 + ix, source.getValueAsString(sourceRow, col));
					colRow ++;
				}
			}
		}
		ds.setResultName("<[ " + resultName + " ]>");
		ds.resetStatus();
		return ds;
	}

}
