/*
 * MySqlProcedureReader.java
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
package workbench.db.mysql;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.JdbcProcedureReader;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A ProcedureReader for MySQL
 *
 * @author  Thomas Kellerer
 */
public class MySqlProcedureReader
	extends JdbcProcedureReader
{
	public MySqlProcedureReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public StringBuilder getProcedureHeader(String aCatalog, String aSchema, String aProcname, int procType)
	{
		StringBuilder source = new StringBuilder(150);

		String sql =
			"SELECT routine_type, dtd_identifier \n" +
			"FROM information_schema.routines \n" +
			" WHERE routine_schema = ? \n" +
			"  and  routine_name = ? \n";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("MySqlProcedureReader.getProcedureHeader()", "Using query=\n" +
				SqlUtil.replaceParameters(sql, aSchema, aProcname));
		}

		String nl = Settings.getInstance().getInternalEditorLineEnding();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, aSchema);
			stmt.setString(2, aProcname);
			rs = stmt.executeQuery();
			String proctype = "PROCEDURE";
			String returntype = "";
			if (rs.next())
			{
				proctype = rs.getString(1);
				returntype = rs.getString(2);
			}
			source.append("DROP ");
			source.append(proctype);
			source.append(' ');
			source.append(aProcname);
			DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(connection);
			source.append(nl);
			source.append(delim.toString());
			source.append(nl);
			source.append(nl);
			source.append("CREATE ");
			source.append(proctype);
			source.append(' ');
			source.append(aProcname);
			source.append(" (");

			DataStore ds = this.getProcedureColumns(aCatalog, aSchema, aProcname);
			int count = ds.getRowCount();
			int added = 0;
			for (int i=0; i < count; i++)
			{
				String ret = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE);
				if (ret.equals("RETURN") || ret.equals("RESULTSET")) continue;
				String vartype = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE);
				String name = ds.getValueAsString(i,ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME);
				if (added > 0) source.append(',');
				source.append(ret);
				source.append(' ');
				source.append(name);
				source.append(' ');
				source.append(vartype);
				added ++;
			}
			source.append(')');
			source.append(nl);
			if ("FUNCTION".equals(proctype))
			{
				source.append("RETURNS ");
				source.append(returntype);
				source.append(nl);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MySqlProcedureReader.getProcedureHeader()", "Error retrieving procedure header", e);
			source = StringUtil.emptyBuffer();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

	/**
	 * Workaround for a bug in older MySQL drivers that do not return information about the return type.
	 *
	 * @param   rs  the result set
	 * @return  the list of procedures
	 * @throws SQLException
	 */
	@Override
	public DataStore fillProcedureListDataStore(ResultSet rs)
		throws SQLException
	{
		DataStore ds = super.fillProcedureListDataStore(rs);
		int count = ds.getRowCount();
		for (int row=0; row < count; row++)
		{
			int type = ds.getValueAsInt(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
			if (type == DatabaseMetaData.procedureResultUnknown)
			{
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
			}
		}
		return ds;
	}

}
