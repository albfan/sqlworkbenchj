/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer.
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
import workbench.resource.Settings;

import workbench.db.RegexErrorPositionReader;
import workbench.db.WbConnection;

import workbench.sql.ErrorDescriptor;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleErrorPositionReader
	extends RegexErrorPositionReader
{
	private final Set<String> verbs = CollectionUtil.caseInsensitiveSet("SELECT", "INSERT", "UPDATE", "DELETE", "MERGE", "WITH");

	public OracleErrorPositionReader()
	{
		this(Settings.getInstance().getBoolProperty("workbench.db.oracle.errorposition.check_create", true), Settings.getInstance().getBoolProperty("workbench.db.oracle.errorposition.check_drop", false));
	}

	public OracleErrorPositionReader(boolean checkCreate, boolean checkDrop)
	{
		super("(?i)\\s(line|zeile)\\s[0-9]+", "(?i)\\s(column|spalte)\\s[0-9]+");

		if (checkCreate)
		{
			verbs.add("CREATE");
		}

		// don't run dbms_sql.parse() for drop statements, just in case...
		if (checkDrop)
		{
			verbs.add("DROP");
		}
	}


	/**
	 * Retrieve the error offset for a SQL statement.
	 *
	 * This is done by using an anonymous PL/SQL block to call DBMS_SQL.PARSE() and retrieve the
	 * error position through that.
	 *
	 * See: http://docs.oracle.com/cd/E11882_01/appdev.112/e25788/d_sql.htm#i997676
	 *
	 * For anonymous PL/SQL blocks Oracle already returns a proper error message including line and column
	 * in this case, this information is extraced using regular expresions.
	 *
	 * CREATE and DROP statements will also be handled. Because as dbms_sql.parse() runs a DDL statement immediately
	 * <b>this method should never be called with a valid CREATE or DROP statement</b>
	 *
	 * @param con  the connection to use
	 * @param sql  the SQL statement which produced the error (without the ; at the end)
	 * @return -1 if no error occurred
	 *         else the starting position in the error if any
	 *
	 * @see RegexErrorPositionReader#getErrorPosition(workbench.db.WbConnection, java.lang.String, java.lang.Exception)
	 */
	@Override
	public ErrorDescriptor getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		String verb = con.getParsingUtil().getSqlVerb(sql);

		// We can only use dbms_sql for the defined types of SQL statements
		// in all other cases (e.g. an anonymous PL/SQL block) we try to get the information from the exception
		// Other PL/SQL errors can only happen when using a CREATE ... statements
		// which is handled in DdlCommand using  the OracleErrorInformationReader
		if (!verbs.contains(verb))
		{
			ErrorDescriptor result = super.getErrorPosition(con, sql, ex);
			if (result != null)
			{
				result.setMessageIncludesPosition(true);
			}
			return result;
		}

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
		int errorPos = -1;

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

		if (errorPos > -1)
		{
			ErrorDescriptor error = new ErrorDescriptor();
			error.setErrorOffset(errorPos);
			return error;
		}
		return null;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, ErrorDescriptor errorInfo)
	{
		if (errorInfo == null) return originalMessage;
		if (!errorInfo.hasError()) return originalMessage;

		// messageIncludesPosition is set by RegexErrorPositionReader
		if (!errorInfo.getMessageIncludesPosition())
		{
			SqlUtil.calculateErrorLine(sql, errorInfo);
			if (errorInfo.getErrorColumn() > -1 && errorInfo.getErrorLine() > -1)
			{
				if (StringUtil.isNonEmpty(originalMessage)) originalMessage += "\n";
				originalMessage = "Error at line " + (errorInfo.getErrorLine() + 1) + ":\n" + originalMessage.trim();
			}
			else
			{
				int offset = errorInfo.getErrorPosition();
				originalMessage = originalMessage.trim() + " (position: " + offset + ")";
			}
		}

		String indicator = SqlUtil.getErrorIndicator(sql, errorInfo);
		if (indicator != null)
		{
			if (StringUtil.isNonEmpty(originalMessage)) originalMessage += "\n";
			originalMessage += indicator;
		}
		return originalMessage;
	}

}
