/*
 * WbOraShow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.ErrorInformationReader;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;

/**
 * An implementation of various SQL*Plus "show" commands.
 *
 * Currently supported commands:
 * <ul>
 *    <li>parameters</li>
 *    <li>user</li>
 * </ul>
 * @author Thomas Kellerer
 */
public class WbOraShow
	extends SqlCommand
{

	public static final String VERB = "SHOW";

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String clean = getCommandLine(sql);
		SQLLexer lexer = new SQLLexer(clean);
		SQLToken token = lexer.getNextToken(false, false);
		if (token == null)
		{
			result.addMessage(ResourceMgr.getString("ErrOraShow"));
			result.setFailure();
			return result;
		}
		String verb = token.getText().toLowerCase();
		if (verb.startsWith("parameter"))
		{
			SQLToken name = lexer.getNextToken(false, false);
			String parm = null;
			if (name != null)
			{
				parm = name.getContents();
			}
			return getParameterValues(sql, parm);
		}
		else if (verb.equals("user"))
		{
			result.addMessage("USER is " + currentConnection.getCurrentUser());
		}
		else if (verb.equals("appinfo"))
		{
			return getAppInfo(sql);
		}
		else if (verb.equals("autocommit"))
		{
			if (currentConnection.getAutoCommit())
			{
				result.addMessage("autocommit ON");
			}
			else
			{
				result.addMessage("autocommit OFF");
			}
		}
		else if (verb.startsWith("error"))
		{
			ErrorInformationReader reader = currentConnection.getMetadata().getErrorInformationReader();
			if (reader != null)
			{
				String errors = reader.getErrorInfo(currentConnection.getCurrentUser(), null, null);
				if (errors.length() > 0)
				{
					result.addMessage(errors);
				}
				else
				{
					result.addMessage(ResourceMgr.getString("TxtOraNoErr"));
				}
			}
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrOraShow"));
			result.setFailure();
		}
		return result;
	}

	private StatementRunnerResult getAppInfo(String sql)
	{
		String query = "SELECT module FROM v$session WHERE audsid = USERENV('SESSIONID')";
		Statement stmt = null;
		ResultSet rs = null;
		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
			stmt = this.currentConnection.createStatementForQuery();
			rs = stmt.executeQuery(query);
			if (rs.next())
			{
				String appInfo = rs.getString(1);
				if (appInfo == null)
				{
					result.addMessage("appinfo is OFF");
				}
				else
				{
					result.addMessage("appinfo is \"" + appInfo + "\"");
				}
			}
		}
		catch (SQLException ex)
		{
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private StatementRunnerResult getParameterValues(String sql, String parameter)
	{
		String query =
			"select name,  \n" +
			"       case type \n" +
			"         when 1 then 'boolean'  \n" +
			"         when 2 then 'string' \n" +
			"         when 3 then 'integer' \n" +
			"         when 4 then 'parameter file' \n" +
			"         when 5 then 'reserved' \n" +
			"         when 6 then 'big integer' \n" +
			"         else to_char(type) \n" +
			"       end as type,  \n" +
			"       value, \n" +
			"       description, \n"  +
			"       update_comment \n" +
			"from v$parameter\n ";
		ResultSet rs = null;
		StatementRunnerResult result = new StatementRunnerResult(sql);

		try
		{
			if (parameter != null)
			{
				query += "where name like lower('%" + parameter + "%')\n ";
			}
			query += "order by name";

			currentStatement = this.currentConnection.createStatementForQuery();
			rs = currentStatement.executeQuery(query);
			processResults(result, true, rs);
		}
		catch (SQLException ex)
		{
			result.setFailure();
			result.addMessage(ex.getMessage());
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return result;
	}

}
