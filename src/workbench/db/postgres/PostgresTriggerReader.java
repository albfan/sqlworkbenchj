/*
 * PostgresTriggerReader.java
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
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import workbench.db.DefaultTriggerReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 * A TriggerReader for Postgres that retrieves not only the trigger source but also
 * the source code of the associated function.
 *
 * @author Thomas Kellerer
 */
public class PostgresTriggerReader
	extends DefaultTriggerReader
{
	public PostgresTriggerReader(WbConnection conn)
	{
		super(conn);

	}

	@Override
	public CharSequence getDependentSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable)
		throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String funcName = null;
		String funcSchema = null;
		StringBuilder result = null;
		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			final String sql =
				"SELECT trgsch.nspname as function_schema, proc.proname as function_name \n" +
				"FROM pg_trigger trg  \n" +
				"  JOIN pg_class tbl ON tbl.oid = trg.tgrelid  \n" +
				"  JOIN pg_proc proc ON proc.oid = trg.tgfoid \n" +
				"  JOIN pg_namespace trgsch ON trgsch.oid = proc.pronamespace \n" +
				"  JOIN pg_namespace tblsch ON tblsch.oid = tbl.relnamespace \n";

			StringBuilder query = new StringBuilder(sql.length() + 50);
			query.append(sql);
			query.append("WHERE trg.tgname = ? \n");
			query.append("  AND tblsch.nspname = ? ");

			stmt = dbConnection.getSqlConnection().prepareStatement(query.toString());
			stmt.setString(1, triggerName);
			stmt.setString(2, triggerTable.getSchema());
			rs = stmt.executeQuery();
			if (rs.next())
			{
				funcSchema = rs.getString(1);
				funcName = rs.getString(2);
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException sql)
		{
			dbConnection.rollback(sp);
			throw sql;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (funcName != null && funcSchema != null)
		{
			ProcedureReader reader = dbMeta.getProcedureReader();
			ProcedureDefinition def = new ProcedureDefinition(null, funcSchema, funcName, DatabaseMetaData.procedureResultUnknown);
			try
			{
				reader.readProcedureSource(def);
				CharSequence src = def.getSource();
				if (src != null)
				{
					result = new StringBuilder(src.length() + 50);
					result.append("\n---[ ");
					result.append(funcName);
					result.append(" ]---\n");
					result.append(src);
					result.append('\n');
				}
			}
			catch (NoConfigException cfg)
			{
				// nothing to do
			}
		}
		return result;
	}

	/**
	 * Triggers on views are supported since Version 9.1
	 */
	@Override
	public boolean supportsTriggersOnViews()
	{
		if (dbConnection == null) return false;
		return JdbcUtils.hasMinimumServerVersion(dbConnection, "9.1");
	}

}
