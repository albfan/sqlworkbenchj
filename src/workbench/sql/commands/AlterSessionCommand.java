/*
 * AlterSessionCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.commands;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * A wrapper for Orcle's AlterSessionCommand
 *
 * @author Thomas Kellerer
 */
public class AlterSessionCommand
	extends SqlCommand
{
	public static final String VERB = "ALTER SESSION";

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		String oldSchema = null;
		SQLLexer lexer = SQLLexerFactory.createLexer(currentConnection, sql);

		DbMetadata meta = currentConnection.getMetadata();

		// Skip the ALTER SESSION verb
		SQLToken token = lexer.getNextToken(false, false);

		token = lexer.getNextToken(false, false);
		if (token.getContents().equals("SET"))
		{
			// check for known statements
			token = lexer.getNextToken(false, false);
			String parm = (token == null ? null : token.getContents());
			if ("CURRENT_SCHEMA".equalsIgnoreCase(parm))
			{
				oldSchema = meta.getCurrentSchema();
			}
			else if ("TIME_ZONE".equalsIgnoreCase(parm) && meta.isOracle())
			{
				// this should be the = sign, skip it
				token = lexer.getNextToken(false, false);

				// this is the parameter for the new timezone
				token = lexer.getNextToken(false, false);
				if (token != null)
				{
					if (changeOracleTimeZone(result, token.getContents()))
					{
						return result;
					}
				}
			}
		}

		try
		{
			this.currentStatement = currentConnection.createStatement();
			this.currentStatement.executeUpdate(sql);
			if (oldSchema == null)
			{
				// A "regular" ALTER SESSION was executed that does not change the
				// current schema (user)
				appendSuccessMessage(result);
			}
			else
			{
				meta.clearCachedSchemaInformation();

				// if the current schema is changed, a schemaChanged should be fired
				String schema = meta.getCurrentSchema();
				if (!oldSchema.equalsIgnoreCase(schema))
				{
					boolean busy = currentConnection.isBusy();
					try
					{
						// schemaChanged will trigger an update of the ConnectionInfo display
						// but that only retrieves the current schema if the connection isn't busy
						currentConnection.setBusy(false);
						currentConnection.schemaChanged(oldSchema, schema);
					}
					finally
					{
						currentConnection.setBusy(busy);
					}
					result.addMessage(ResourceMgr.getFormattedString("MsgSchemaChanged", schema));
				}
			}

			result.setSuccess();
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);
			LogMgr.logUserSqlError("AlterSessionCommand.execute()", sql, e);
		}

		return result;
	}

	private boolean changeOracleTimeZone(StatementRunnerResult result, String tz)
	{
		Connection sqlCon = currentConnection.getSqlConnection();
		Method setTimezone = null;

		try
		{
			Class cls = currentConnection.getSqlConnection().getClass();
			setTimezone = cls.getMethod("setSessionTimeZone", new Class[] {String.class} );
		}
		catch (Exception e)
		{
			// Ignore
			return false;
		}

		if (setTimezone != null)
		{
			try
			{
				String zone = StringUtil.trimQuotes(tz);
				LogMgr.logDebug("AlterSessionCommand.changeOracleTimeZone()", "Calling Oracle's setSessionTimeZone");
				setTimezone.setAccessible(true);
				setTimezone.invoke(sqlCon, new Object[] { zone });
				result.addMessage(ResourceMgr.getString("MsgTimezoneChanged") + " " + zone);
				result.setSuccess();
				return true;
			}
			catch (Exception e)
			{
				result.addErrorMessage(ExceptionUtil.getDisplay(e));
				LogMgr.logError("AlterSessionCommand.changeOracleTimeZone()", "Error setting timezone", e);
			}
		}
		return false;
	}
}
