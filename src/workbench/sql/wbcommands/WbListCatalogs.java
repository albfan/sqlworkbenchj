/*
 * WbListCatalogs.java
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
public class WbListCatalogs
	extends SqlCommand
{

	public static final String VERB = "WbListDB";
	public static final String VERB_ALTERNATE = "WbListCat";

	public WbListCatalogs()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public String getAlternateVerb()
	{
		return VERB_ALTERNATE;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String verb = SqlUtil.getSqlVerb(sql);

		DataStore ds = null;
		String catName = StringUtil.capitalize(currentConnection.getMetadata().getCatalogTerm());

		if (verb.equalsIgnoreCase(VERB) && currentConnection.getMetadata().isPostgres())
		{
			ds = listPgDatabases();
		}
		else
		{
			ConsoleSettings.getInstance().setNextRowDisplay(RowDisplay.SingleLine);

			List<String> cats = currentConnection.getMetadata().getCatalogs();
			String[] cols = {catName};
			int[] types = {Types.VARCHAR};
			int[] sizes = {10};

			ds = new DataStore(cols, types, sizes);
			for (String cat : cats)
			{
				int row = ds.addRow();
				ds.setValue(row, 0, cat);
			}
			ds.setResultName(catName);
			ds.setGeneratingSql(sql);
		}

		ds.resetStatus();
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

	private DataStore listPgDatabases()
	{
		String name = StringUtil.capitalize(currentConnection.getMetadata().getCatalogTerm());
		String sql =
			"SELECT d.datname as \"" + name + "\",\n" +
			"       pg_catalog.pg_get_userbyid(d.datdba) as \"Owner\",\n" +
			"       pg_catalog.pg_encoding_to_char(d.encoding) as \"Encoding\",\n" +
			"       d.datcollate as \"Collate\",\n" +
			"       d.datctype as \"Ctype\",\n" +
			"       pg_catalog.array_to_string(d.datacl, E'\\n') AS \"Access privileges\", \n" +
			"       pg_catalog.shobj_description(d.oid, 'pg_database') as \"Description\"" +
			"FROM pg_catalog.pg_database d\n" +
			"ORDER BY 1";
		DataStore ds = SqlUtil.getResult(currentConnection, sql, true);
		ds.setGeneratingSql(sql);
		ds.setResultName(ResourceMgr.getString("TxtDbList"));
		return ds;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
