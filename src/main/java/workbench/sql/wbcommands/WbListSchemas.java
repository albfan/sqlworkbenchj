/*
 * WbListSchemas.java
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

import workbench.console.ConsoleSettings;
import workbench.console.RowDisplay;
import workbench.resource.ResourceMgr;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbListSchemas
	extends SqlCommand
{
	public static final String VERB = "WbListSchemas";

	public WbListSchemas()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

		List<String> schemas = currentConnection.getMetadata().getSchemas();

		DataStore ds = null;
		if (currentConnection.getMetadata().isPostgres())
		{
			ds = listPgSchemas();
		}
		else
		{
			String schemaName = StringUtil.capitalize(currentConnection.getMetadata().getSchemaTerm());
			String[] cols = { schemaName };
			int[] types = { Types.VARCHAR };
			int[] sizes = { 10 };

			ds = new DataStore(cols, types, sizes);
			for (String cat : schemas)
			{
				int row = ds.addRow();
				ds.setValue(row, 0, cat);
			}
		}
		ds.setResultName(ResourceMgr.getString("TxtSchemaList"));
		ds.setGeneratingSql(VERB);
		ds.resetStatus();
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

	private DataStore listPgSchemas()
	{
		String sql =
			"SELECT n.nspname AS \"Schema\",\n" +
			"       pg_catalog.pg_get_userbyid(n.nspowner) AS \"Owner\", \n" +
			"				pg_catalog.array_to_string(n.nspacl, ', ') as \"Access privileges\", " +
			"       pg_catalog.obj_description(n.oid, 'pg_namespace') AS \"Description\" " +
			"FROM pg_catalog.pg_namespace n\n" +
			"WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'\n" +
			"ORDER BY 1";

		DataStore ds = SqlUtil.getResult(currentConnection, sql, true);
		ds.setGeneratingSql(sql);
		return ds;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
