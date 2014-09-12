/*
 * DatastoreTransposer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import java.util.List;

import workbench.resource.ResourceMgr;

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

	public DatastoreTransposer(DataStore sourceData)
	{
		this.source = sourceData;
		retrieveResultName();
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
			List<String> tables = SqlUtil.getTables(sql, false);
			if (tables.size() == 1)
			{
				resultName = tables.get(0);
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
			columns[i+1] = ResourceMgr.getString("TxtRow") + " #" + Integer.toString(rows[i] + 1);
			types[i+1] = Types.VARCHAR;
		}

		DataStore ds = new DataStore(columns, types);

		int colCount = source.getColumnCount();
		for (int i=0; i < colCount; i++)
		{
			ds.addRow();
		}

		for (int ix=0; ix < rows.length; ix++)
		{
			int row = rows[ix];
			for (int col=0; col < colCount; col ++)
			{
				ds.setValue(col, 0, source.getColumnDisplayName(col));
				ds.setValue(col, 1 + ix, source.getValueAsString(row, col));
			}
		}
		ds.setResultName("<[ " + resultName + " ]>");
		ds.resetStatus();
		return ds;
	}

}
