/*
 * SetCommand.java
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
package workbench.sql.commands;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * This class implements a wrapper for the SET command.
 * <br/>
 *
 * Oracle's SET command is only valid from within SQL*Plus.
 * By supplying an implementation for the Workbench, we can ignore the errors
 * reported by the JDBC interface, so that SQL scripts intended for SQL*Plus
 * can also be run from within the workbench
 * <br/>
 * For other DBMS this enables the support for controlling autocommit
 * through a SQL command. All parameters except serveroutput and autocommit
 * are passed on to the server.
 *
 * @author  Thomas Kellerer
 */
public class SetCommand
	extends SqlCommand
{
	public static final String VERB = "SET";

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = null;

		try
		{
			String command = null;
			int commandEnd = -1;
			String param = null;
			try
			{
				SQLLexer l = new SQLLexer(aSql);
				SQLToken t = l.getNextToken(false, false); // ignore the verb
				t = l.getNextToken(false, false);
				if (t != null)
				{
					command = t.getContents();
					commandEnd = t.getCharEnd();
				}
				t = l.getNextToken(false, false);

				// Ignore a possible equal sign
				if (t != null && t.getContents().equals("="))
				{
					t = l.getNextToken(false, false);
				}

				if (t != null)
				{
					param = t.getContents();
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("SetCommand.execute()", "Could not parse statement", e);
			}

			boolean execSql = true;
			boolean pgSchemaChange = false;

			if (command != null)
			{
				// those SET commands that have a SQL Workbench equivalent (JDBC API)
				// will be "executed" by calling the approriate functions.
				// We don't need to send the SQL to the server in this case
				// Any other command is sent to the server without modification.
				if (command.equalsIgnoreCase("autocommit"))
				{
					result = this.setAutocommit(currentConnection, param);
					execSql = false;
				}
				else if (currentConnection.getMetadata().isOracle())
				{
					Set<String> allowed = CollectionUtil.caseInsensitiveSet("constraints","constraint","transaction","role");
					List<String> options = Settings.getInstance().getListProperty("workbench.db.oracle.set.options", true, "");
					allowed.addAll(options);

					execSql = false;
					if (command.equalsIgnoreCase("serveroutput"))
					{
						result = this.setServeroutput(currentConnection, param);
					}
					else if (command.equalsIgnoreCase("feedback"))
					{
						result = this.setFeedback(param);
					}
					else if (command.equalsIgnoreCase("autotrace"))
					{
						result = handleAutotrace(aSql.substring(commandEnd).trim());
					}
					else if (allowed.contains(command))
					{
						execSql = true;
					}
					else
					{
						result = new StatementRunnerResult();
						String msg = ResourceMgr.getFormattedString("MsgCommandIgnored", aSql);
						result.addMessage(msg);
						result.setSuccess();
					}
				}
				else if (command.equalsIgnoreCase("maxrows"))
				{
					result = new StatementRunnerResult();
					execSql = false;
					try
					{
						int rows = Integer.parseInt(param);
						this.runner.setMaxRows(rows);
						result.setSuccess();
						result.addMessage(ResourceMgr.getFormattedString("MsgSetSuccess", command, rows));
					}
					catch (Exception e)
					{
						result.setFailure();
						result.addMessage(ResourceMgr.getFormattedString("MsgSetFailure", param, command));
					}
				}
				else if (command.equalsIgnoreCase("timeout"))
				{
					result = new StatementRunnerResult();
					execSql = false;
					try
					{
						int timeout = Integer.parseInt(param);
						this.runner.setQueryTimeout(timeout);
						result.setSuccess();
						result.addMessage(ResourceMgr.getFormattedString("MsgSetSuccess", command, timeout));
					}
					catch (Exception e)
					{
						result.setFailure();
						result.addMessage(ResourceMgr.getFormattedString("MsgSetFailure", param, command));
					}
				}
				else if (command.equalsIgnoreCase("schema") && currentConnection.getMetadata().isPostgres())
				{
					pgSchemaChange = true;
				}
			}

			if (execSql)
			{
				String oldSchema = null;
				if (pgSchemaChange)
				{
					oldSchema = currentConnection.getCurrentSchema();
				}
				result = new StatementRunnerResult();
				aSql = getSqlToExecute(aSql);
				this.currentStatement = currentConnection.createStatement();

				// Using a generic execute ensures that servers that
				// can process more than one statement with a single SQL
				// are treated correctly. E.g. when sending a SET and a SELECT
				// as one statement for SQL Server
				boolean hasResult = this.currentStatement.execute(aSql);
				processMoreResults(aSql, result, hasResult);
				result.setSuccess();

				if (!pgSchemaChange)
				{
					appendSuccessMessage(result);
				}

				if (pgSchemaChange)
				{
					String newSchema = handlePgSchemaChange(oldSchema);
					result.addMessage(ResourceMgr.getFormattedString("MsgSchemaChanged", newSchema));
				}
			}
		}
		catch (Exception e)
		{
			result = new StatementRunnerResult();
			result.clear();
			if (currentConnection.getMetadata().isOracle())
			{
				// for oracle we'll simply ignore the error as the SET command
				// is a SQL*Plus command
				result.setSuccess();
				result.setWarning(true);
				result.addMessage(ResourceMgr.getString("MsgSetErrorIgnored") + ": " + e.getMessage());
			}
			else
			{
				// for other DBMS this is an error
				result.setFailure();
				result.addMessage(e.getMessage());
			}
		}
		finally
		{
			// done() will close the currentStatement
			this.done();
		}

		return result;
	}

	private String handlePgSchemaChange(String oldSchema)
	{
		boolean busy = currentConnection.isBusy();
		String newSchema = null;
		try
		{
			currentConnection.setBusy(false);
			LogMgr.logDebug("SetCommand.execute()", "Updating current schema");
			newSchema = currentConnection.getCurrentSchema();

			// schemaChanged will trigger an update of the ConnectionInfo
			// but that only retrieves the current schema if the connection isn't busy
			currentConnection.schemaChanged(oldSchema, newSchema);
		}
		finally
		{
			currentConnection.setBusy(busy);
		}
		return newSchema;
	}

	private StatementRunnerResult handleAutotrace(String parameter)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		if ("off".equalsIgnoreCase(parameter))
		{
			runner.removeSessionProperty("autotrace");
			result.addMessageByKey("MsgAutoTraceOff");
			return result;
		}
		List<String> flags = StringUtil.stringToList(parameter.toLowerCase(), " ");

		if (flags.contains("on") || flags.contains("traceonly"))
		{
			runner.setSessionProperty("autotrace", StringUtil.listToString(flags, ','));
			result.addMessageByKey("MsgAutoTraceOn");
		}
		else
		{
			result.addMessageByKey("MsgAutoTraceUsage");
			result.setFailure();
		}
		return result;
	}

	private StatementRunnerResult setServeroutput(WbConnection aConnection, String param)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		if ("off".equalsIgnoreCase(param))
		{
			aConnection.getMetadata().disableOutput();
			result.addMessageByKey("MsgDbmsOutputDisabled");
		}
		else if ("on".equalsIgnoreCase(param))
		{
			aConnection.getMetadata().enableOutput();
			result.addMessageByKey("MsgDbmsOutputEnabled");
		}
		else
		{
			result.setFailure();
			result.addMessageByKey("ErrServeroutputWrongParameter");
		}
		return result;
	}

	private StatementRunnerResult setAutocommit(WbConnection aConnection, String param)
	{
		StatementRunnerResult result = new StatementRunnerResult();

		if (StringUtil.isEmptyString(param))
		{
			result.setFailure();
			result.addMessageByKey("ErrAutocommitWrongParameter");
			return result;
		}

		Set<String> offValues = CollectionUtil.caseInsensitiveSet("off", "false", "0");
		Set<String> onValues = CollectionUtil.caseInsensitiveSet("on", "true", "1");

		try
		{
			if (offValues.contains(param))
			{
				aConnection.setAutoCommit(false);
				result.addMessageByKey("MsgAutocommitDisabled");
				result.setSuccess();
			}
			else if (onValues.contains(param))
			{
				aConnection.setAutoCommit(true);
				result.addMessageByKey("MsgAutocommitEnabled");
				result.setSuccess();
			}
			else
			{
				result.addMessageByKey("ErrAutocommitWrongParameter");
				result.setFailure();
			}
		}
		catch (SQLException e)
		{
			result.setFailure();
			result.addMessage(e.getMessage());
		}
		return result;
	}

	private StatementRunnerResult setFeedback(String param)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (StringUtil.isEmptyString(param))
		{
			result.setFailure();
			result.addMessageByKey("ErrFeedbackWrongParameter");
			return result;
		}

		Set<String> offValues = CollectionUtil.caseInsensitiveSet("off", "false", "0");
		Set<String> onValues = CollectionUtil.caseInsensitiveSet("on", "true", "1");

		if (offValues.contains(param))
		{
			this.runner.setVerboseLogging(false);
			result.addMessageByKey("MsgFeedbackDisabled");
			result.setSuccess();
		}
		else if (onValues.contains(param))
		{
			this.runner.setVerboseLogging(true);
			result.addMessageByKey("MsgFeedbackEnabled");
			result.setSuccess();
		}
		else
		{
			result.addMessageByKey("ErrFeedbackWrongParameter");
			result.setFailure();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}
