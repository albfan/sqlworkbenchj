/*
 * ResultInfoDisplayBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.Types;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultInfoDisplayBuilder
{

	public static DataStore getDataStore(ResultInfo info, boolean showComments)
	{
		String[] cols;
		int[] types;

		if (showComments)
		{
			cols = new String[] {	"INDEX", "COLUMN_NAME", "ALIAS", "DATA_TYPE", "JDBC Type", "REMARKS", "BASE TABLE", "CLASS_NAME" };
			types = new int[]	{ Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		}
		else
		{
			cols = new String[] { "INDEX", "COLUMN_NAME", "ALIAS", "DATA_TYPE", "JDBC Type", "CLASS_NAME" };
			types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR };
		}

		DataStore infoDs = new DataStore(cols, types);
		//for (ColumnIdentifier col : info.getColumns())
		for (int columnPosition=0; columnPosition < info.getColumnCount(); columnPosition++)
		{
			int row = infoDs.addRow();
			int colIndex = 0;
			ColumnIdentifier col = info.getColumns()[columnPosition];
			int colPos = col.getPosition() == 0 ? columnPosition + 1 : col.getPosition();
			infoDs.setValue(row, colIndex++, colPos);
			infoDs.setValue(row, colIndex++, col.getColumnName());
			infoDs.setValue(row, colIndex++, col.getColumnAlias());
			infoDs.setValue(row, colIndex++, col.getDbmsType());
			infoDs.setValue(row, colIndex++, col.getDataType());
			if (showComments)
			{
				infoDs.setValue(row, colIndex++, col.getComment());
				infoDs.setValue(row, colIndex++, col.getSourceTableName());
			}
			infoDs.setValue(row, colIndex++, col.getColumnClassName());
		}
		return infoDs;
	}

}
