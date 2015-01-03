/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.DbDriver;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;

/**
 * List all defined profiles
 *
 * @author Thomas Kellerer
 */
public class WbListDrivers
	extends SqlCommand
{
	public static final String VERB = "WbListDrivers";

	public WbListDrivers()
	{
		super();
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		List<DbDriver> drivers = ConnectionMgr.getInstance().getDrivers();

		String[] columns = new String[] { ResourceMgr.getString("LblDriver"), ResourceMgr.getString("LblDriverClass"), ResourceMgr.getString("LblDriverLibrary")};
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 30, 40, 40 };
		DataStore ds = new DataStore(columns, types, sizes);

		for (DbDriver driver : drivers)
		{
			int row = ds.addRow();
			ds.setValue(row, 0, driver.getName());
			ds.setValue(row, 1, driver.getDriverClass());
			ds.setValue(row, 2, StringUtil.listToString(driver.getLibraryList(), System.getProperty("path.separator"), false));
		}
		ds.resetStatus();
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
