/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.ErrorPositionReader;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleErrorPositionReader
	implements ErrorPositionReader
{
	private final Set<String> verbs = CollectionUtil.caseInsensitiveSet("SELECT", "INSERT", "UPDATE", "DELETE", "MERGE", "WITH");

	/**
	 * Retrieve the error offset for a SQL statement.
	 *
	 * This is done by using an anonymous PL/SQL block to call DBMS_SQL.PARSE() and retrieve the
	 * error position through that.
	 *
	 * See: http://docs.oracle.com/cd/E11882_01/appdev.112/e25788/d_sql.htm#i997676
	 *
	 * @param con  the connection to use
	 * @param sql  the SQL statement which produced the error (without the ; at the end)
	 * @return -1 if no error occurred
	 *         else the starting position in the error if any
	 */
	@Override
	public int getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		String verb = SqlUtil.getSqlVerb(sql);

		// We can only get the information for the defined SQL statements
		if (!verbs.contains(verb)) return -1;

		int errorPos = -1;

		String getErrorSql =
			"/* SQLWorkbench */ \n" +
			"DECLARE \n" +
			"  l_cursor NUMBER; \n" +
			"  l_result NUMBER; \n" +
			"BEGIN \n" +
			"  l_result := -1; \n" +
			"  BEGIN \n" +
			"    l_cursor := DBMS_SQL.OPEN_CURSOR; \n" +
			"    DBMS_SQL.PARSE(l_cursor, :1, DBMS_SQL.NATIVE); \n" +
			"  EXCEPTION \n" +
			"    WHEN OTHERS THEN\n" +
			"     l_result := DBMS_SQL.LAST_ERROR_POSITION; \n" +
			"  END; \n" +
			"  DBMS_SQL.CLOSE_CURSOR(l_cursor); \n"+
			"  :2 := l_result;\n" +
			"END;";

		CallableStatement cstmt = null;
		try
		{
			cstmt = con.getSqlConnection().prepareCall(getErrorSql);
			cstmt.registerOutParameter(2, Types.INTEGER);
			cstmt.setString(1, sql);
			cstmt.execute();
			errorPos = cstmt.getInt(2);
		}
		catch (Throwable e)
		{
			LogMgr.logDebug("OracleErrorPositionReader.getErrorPosition()", "Could not retrieve error offset", e);
		}
		finally
		{
			SqlUtil.closeStatement(cstmt);
		}
		return errorPos;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, int errorPosition)
	{
		originalMessage += "\nAt position: " + errorPosition;

		String indicator = SqlUtil.getErrorIndicator(sql, errorPosition);
		if (indicator != null)
		{
			originalMessage += "\n\n" + indicator;
		}
		return originalMessage;
	}

}
