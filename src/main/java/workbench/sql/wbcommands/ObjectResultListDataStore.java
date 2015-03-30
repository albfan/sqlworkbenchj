/*
 * ObjectResultListDataStore.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectResultListDataStore
	extends DataStore
{
	public static final int COL_IDX_OBJECT_NAME = 0;
	public static final int COL_IDX_OBJECT_TYPE = 1;
	public static final int COL_IDX_SOURCE = 2;

	private static final String[] colnames = new String[] { "NAME", "TYPE", "SOURCE" };
	private static final int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.CLOB };
	private static final int[] colSizes = new int[] { 30, 30, 50 };

	public ObjectResultListDataStore()
	{
		super(colnames, colTypes, colSizes);
	}

	public ObjectResultListDataStore(WbConnection con, List<DbObject> resultList, boolean showFullname)
		throws SQLException
	{
		super(colnames, colTypes, colSizes);
		setResultList(con, resultList, showFullname);
	}

	public final void setResultList(WbConnection con, List<DbObject> resultList, boolean showFullname)
		throws SQLException
	{
		if (resultList == null) return;

		for (DbObject object : resultList)
		{
			int row = addRow();
			String name = null;
			String type = object.getObjectType();

			if (showFullname)
			{
				name = object.getFullyQualifiedName(con);
			}
			else
			{
				name = object.getObjectName();
			}

			if (object instanceof ProcedureDefinition)
			{
				ProcedureDefinition def = (ProcedureDefinition)object;
				if (def.isPackageProcedure())
				{
					name = def.getPackageName();
					type = "PACKAGE";
				}
			}
			setValue(row, COL_IDX_OBJECT_NAME, name);
			setValue(row, COL_IDX_OBJECT_TYPE, type);
			setValue(row, COL_IDX_SOURCE, object.getSource(con));
		}
		resetStatus();
	}

}
