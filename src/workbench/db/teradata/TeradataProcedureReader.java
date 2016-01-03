/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
package workbench.db.teradata;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.log.LogMgr;

import workbench.db.JdbcProcedureReader;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TeradataProcedureReader
	extends JdbcProcedureReader
{

	public TeradataProcedureReader(WbConnection conn)
	{
		super(conn);
	}

	@Override
	public CharSequence retrieveProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{

		String query = "show procedure " + SqlUtil.buildExpression(connection, def);

		LogMgr.logDebug("TeradataProcedureReader.retrieveProcedureSource", "Query to retrieve procedure source: " + query);

		Statement stmt = null;
		ResultSet rs = null;
		StringBuilder source = new StringBuilder(100);

		try
		{
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(query);
			while (rs.next())
			{
				if (source.length() > 0) source.append('\n');
				source.append(rs.getString(1));
			}
		}
		catch (Exception ex)
		{
			LogMgr.logDebug("TeradataProcedureReader.retrieveProcedureSource()", "Error retrieving procedure source using: \n" + query, ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;
	}

}
