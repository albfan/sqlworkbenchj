/*
 * DatastoreTransposer.java
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

import java.sql.Types;
import java.util.List;
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

	public DataStore transposeRow(int row)
	{
		if (row < 0 || row >= source.getRowCount()) return null;
		DataStore ds = createDatastore();
		int colCount = source.getColumnCount();
		for (int col=0; col < colCount; col ++)
		{
			int newRow = ds.addRow();
			ds.setValue(newRow, 0, source.getColumnDisplayName(col));
			ds.setValue(newRow, 1, source.getValueAsString(row, col));
		}

		String name = "Row " + Integer.toString(row + 1);
		if (resultName != null)
		{
			name = resultName + " (" + name + ")";
		}
		ds.setResultName(name);
		ds.resetStatus();
		return ds;
	}

	private DataStore createDatastore()
	{
		String[] columns = new String[] { "Column", "Value" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		return new DataStore(columns, types);
	}
}
