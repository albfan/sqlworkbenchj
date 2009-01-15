/*
 * SetCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;
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
 * @author  support@sql-workbench.net
 */
public class SetCommand extends SqlCommand
{
	public static final String VERB = "SET";

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = null; 

		try
		{
			String command = null;
			String param = null;
			try
			{
				SQLLexer l = new SQLLexer(aSql);
				SQLToken t = l.getNextToken(false, false); // ignore the verb
				t = l.getNextToken(false, false);
				if (t != null) command = t.getContents();
				t = l.getNextToken(false, false);
				if (t != null) param = t.getContents();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			boolean execSql = true;

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
					if (command.equalsIgnoreCase("serveroutput"))
					{
						result = this.setServeroutput(currentConnection, param);
						execSql = false;
					}
					else if (command.equalsIgnoreCase("feedback"))
					{
						result = this.setFeedback(param);
						execSql = false;
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
			}

			if (execSql)
			{
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
				appendSuccessMessage(result);
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
			SqlUtil.closeStatement(this.currentStatement);
			this.done();
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

		try
		{
			if ("off".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param))
			{
				aConnection.setAutoCommit(false);
				result.addMessageByKey("MsgAutocommitDisabled");
				result.setSuccess();
			}
			else if ("on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param))
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

		if ("off".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param))
		{
			this.runner.setVerboseLogging(false);
			result.addMessageByKey("MsgFeedbackDisabled");
			result.setSuccess();
		}
		else if ("on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param))
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

	public String getVerb()
	{
		return VERB;
	}

}
