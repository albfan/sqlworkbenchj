/*
 * SqlServerProcedureReader.java
 *
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
package workbench.db.mssql;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A ProcedureReader for Microsoft SQL Server.
 *
 * @author  Thomas Kellerer
 */
public class SqlServerProcedureReader
	extends JdbcProcedureReader
{
	private final String GET_PROC_SQL = "{call sp_stored_procedures (@sp_owner = ?,  @sp_name = ?)}";
	private final StringBuilder header = StringUtil.emptyBuilder();
	private boolean useOwnSQL = true;

	public SqlServerProcedureReader(WbConnection db)
	{
		super(db);
	}

	@Override
	public StringBuilder getProcedureHeader(String catalog, String schema, String procName, int procType)
	{
		return header;
	}


	/**
	 * The MS JDBC driver does not return the PROCEDURE_TYPE column correctly
	 * so we implement it ourselves. The driver always returns RESULT which is
	 * - strictly speaking - true, but as MS still distinguished between
	 * procedures and functions we need to return this correctly.
	 * <br/>
	 * The correct "type" is important because e.g. a DROP from within the DbExplorer
	 * relies on the correct type returned by getProcedures()
	 * <br/>
	 * The SQL seems to be only working with the jTDS driver. The MS driver throws
	 * and error "Incorrect syntax near '{'." which is wrong as the syntax complies
	 * with the JDBC standard.
	 */
	@Override
	public DataStore getProcedures(String catalog, String owner, String namePattern)
		throws SQLException
	{
		if (!useOwnSQL)
		{
			DataStore ds = super.getProcedures(catalog, owner, namePattern);
			updateRemarks(ds, owner);
			return ds;
		}

		CallableStatement cstmt = this.connection.getSqlConnection().prepareCall(GET_PROC_SQL);
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("SqlServerProcedureReader.getProcedures()", "Using query=\n" + GET_PROC_SQL);
		}

		DataStore ds;
		ResultSet rs = null;
		try
		{
			ds = buildProcedureListDataStore(this.connection.getMetadata(), false);

			if (StringUtil.isEmptyString(owner) || "*".equals(owner))
			{
				cstmt.setString(1, "%");
			}
			else
			{
				cstmt.setString(1, owner);
			}

			if (StringUtil.isEmptyString(namePattern))
			{
				cstmt.setString(2, "%");
			}
			else
			{
				cstmt.setString(2, namePattern);
			}

			boolean hasResult = cstmt.execute();

			if (hasResult)
			{
				rs = cstmt.getResultSet();
			}
			else
			{
				useOwnSQL = false;
				LogMgr.logError("SqlServerProcedureReader.getProcedures()", "Could not retrieve procedures using [sp_stored_procedures]", null);
				return super.getProcedures(catalog, owner, null);
			}

			while (rs.next())
			{
				String dbname = rs.getString("PROCEDURE_QUALIFIER");
				String procOwner = rs.getString("PROCEDURE_OWNER");
				String name = rs.getString("PROCEDURE_NAME");
				char procType = 0;
				if (name.indexOf(';') == name.length() - 2)
				{
					procType = name.charAt(name.length() - 1);
					name = name.substring(0, name.length() - 2);
				}
				String remark = rs.getString("REMARKS");
				int type = rs.getShort("PROCEDURE_TYPE");
				Integer iType;
				if (rs.wasNull() || type == DatabaseMetaData.procedureResultUnknown)
				{
					// we can't really handle procedureResultUnknown, so it is treated as "no result"
					iType = Integer.valueOf(DatabaseMetaData.procedureNoResult);
				}
				else
				{
					if (procType != 0)
					{
						if (procType == '0')
						{
							iType = Integer.valueOf(DatabaseMetaData.procedureReturnsResult);
						}
						else
						{
							iType = Integer.valueOf(DatabaseMetaData.procedureNoResult);
						}
					}
					else
					{
						iType = Integer.valueOf(type);
					}
				}
				int row = ds.addRow();
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, dbname);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, procOwner);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, name);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, iType);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
			ds.resetStatus();
		}
		catch (SQLException e)
		{
			LogMgr.logError("SqlServerProcedureReader", "Could not retrieve procedures using [p_stored_procedures]", e);
			useOwnSQL = false;
			return super.getProcedures(catalog, owner, null);
		}
		finally
		{
			SqlUtil.closeAll(rs, cstmt);
		}
		updateRemarks(ds, owner);
		return ds;
	}

	protected void updateRemarks(DataStore ds, String owner)
	{
		if (!Settings.getInstance().getBoolProperty("workbench.db.microsoft_sql_server.remarks.procedure.retrieve", false)) return;

		if (ds == null || ds.getRowCount() == 0) return;

		String object = null;
		if (ds.getRowCount() == 1)
		{
			object = ds.getValueAsString(0, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
		}

		SqlServerObjectListEnhancer reader = new SqlServerObjectListEnhancer();
		Map<String, String> remarks = reader.readRemarks(connection, owner, object, new String[] { "procedure"});

		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			String name = ds.getValueAsString(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			String remark = remarks.get(name);
			if (remark != null)
			{
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
			}
		}
	}

	@Override
	public CharSequence retrieveProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		SpHelpTextRunner runner = new SpHelpTextRunner();
		String procName = stripVersionInfo(def.getProcedureName());
		CharSequence sql = runner.getSource(connection, def.getCatalog(), def.getSchema(), procName);
		return sql;
	}
}
