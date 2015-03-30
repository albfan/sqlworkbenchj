/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import workbench.interfaces.JobErrorHandler;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.RowData;

/**
 *
 * @author Thomas Kellerer
 */
public class VariablesDataStore
	extends DataStore
{
	private static final String[] cols = {ResourceMgr.getString("LblVariableName"), ResourceMgr.getString("LblVariableValue") };
	private static final int[] types =   {Types.VARCHAR, Types.VARCHAR};
	private static final int[] sizes =   {20, 50};
	private static final TableIdentifier TABLE_ID = new TableIdentifier("WB$VARIABLE_DEFINITION");

	public VariablesDataStore()
	{
		super(cols, types, sizes);
		this.setUpdateTable(TABLE_ID);
	}

	@Override
	public List<DmlStatement> getUpdateStatements(WbConnection aConn)
	{
		return Collections.emptyList();
	}

	@Override
	public boolean hasPkColumns()
	{
		return true;
	}

	@Override
	public boolean isUpdateable()
	{
		return true;
	}

	@Override
	public boolean hasUpdateableColumns()
	{
		return true;
	}

	@Override
	public boolean checkUpdateTable()
	{
		return true;
	}

	@Override
	public boolean checkUpdateTable(WbConnection aConn)
	{
		return true;
	}

	@Override
	public int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException, IllegalArgumentException
	{
		int rowcount = this.getRowCount();
		this.resetUpdateRowCounters();

		VariablePool pool = VariablePool.getInstance();
		for (int i=0; i < rowcount; i++)
		{
			String key = this.getValueAsString(i, 0);
			String oldkey = (String)this.getOriginalValue(i, 0);
			if (oldkey != null && !key.equals(oldkey))
			{
				pool.removeVariable(oldkey);
			}
			String value = this.getValueAsString(i, 1);
			// Treat null as an empty value
			pool.setParameterValue(key, value == null ? "" : value);
		}

		RowData row = this.getNextDeletedRow();
		while (row != null)
		{
			String key = (String)row.getValue(0);
			pool.removeVariable(key);
			row = this.getNextDeletedRow();
		}
		this.resetStatus();
		return rowcount;
	}

}