/*
 * FirebirdProcedureReader.java
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
package workbench.db.firebird;

import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 * An implementation of the ProcedureReader interface for the
 * <a href="http://www.firebirdsql.org">Firebird</a> database server
 *
 * @author  Thomas Kellerer
 */
public class FirebirdProcedureReader
	extends JdbcProcedureReader
{
	public FirebirdProcedureReader(WbConnection conn)
	{
		super(conn);
	}

	@Override
	public StringBuilder getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StringBuilder source = new StringBuilder();
		try
		{
			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname);
			source.append("CREATE PROCEDURE ");
			source.append(aProcname);
			String retType = null;
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String vartype = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String name = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				String ret = ds.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if ("OUT".equals(ret))
				{
					retType = "(" + name + " " + vartype + ")";
				}
				else
				{
					if (added > 0)
					{
						source.append(',');
					}
					else
					{
						source.append(" (");
					}
					source.append(name);
					source.append(' ');
					source.append(vartype);
					added ++;
				}
			}
			if (added > 0) source.append(')');
			if (retType != null)
			{
				source.append("\nRETURNS ");
				source.append(retType);
			}
			source.append("\nAS\n");
		}
		catch (Exception e)
		{
			source = StringUtil.emptyBuffer();
		}
		return source;
	}

}
