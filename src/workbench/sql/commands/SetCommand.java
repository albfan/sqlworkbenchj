/*
 * SetCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * This class implements a wrapper for the SET command
 *
 * Oracle's SET command is only valid from within SQL*Plus.
 * By supplying an implementation for the Workbench, we can ignore the errors
 * reported by the JDBC interface, so that SQL scripts intended for SQL*Plus
 * can also be run from within the workbench
 *
 * For other DBMS this enables the support for controlling autocommit
 * through a SQL command. All parameters except serveroutput and autocommit
 * are passed on to the server.
 *
 * @author  support@sql-workbench.net
 */
public class SetCommand extends SqlCommand
{
	public static final String VERB = "SET";

	public SetCommand()
	{
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = null; //

		try
		{
			String[] words = aSql.split("\\s");
			boolean execSql = true;
			String command = null;
			
			if (words.length > 1)
			{
				command = words[1];
				if (command.equalsIgnoreCase("autocommit"))
				{
					result = this.setAutocommit(aConnection, words);
					execSql = false;
				}
				else if (aConnection.getMetadata().isOracle())
				{
					if (command.equalsIgnoreCase("serveroutput"))
					{
						result = this.setServeroutput(aConnection, words);
						execSql = false;
					}
					else if (command.equalsIgnoreCase("feedback"))
					{
						result = this.setFeedback(aConnection, words);
						execSql = false;
					}
				}
			}

			if (execSql)
			{
				result = new StatementRunnerResult();
				this.currentStatement = aConnection.createStatement();
				this.currentStatement.execute(aSql);
				StringBuffer warnings = new StringBuffer();
				if (this.appendWarnings(aConnection, this.currentStatement , warnings))
				{
					result.setWarning(true);
					result.addMessage(warnings.toString());
				}
			}
			
			if ("SCHEMA".equalsIgnoreCase(command))
			{
				aConnection.schemaChanged(null, null);
				result.addMessage(ResourceMgr.getString("MsgSchemaChanged"));
			}
		}
		catch (Throwable e)
		{
			result = new StatementRunnerResult();
			result.clear();
			if (aConnection.getMetadata().isOracle())
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
			if (this.currentStatement != null) this.currentStatement.close();
			this.done();
		}

		return result;
	}

	private StatementRunnerResult setServeroutput(WbConnection aConnection, String[] words)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		if (words.length > 2)
		{
			if (words[2].equalsIgnoreCase("off"))
			{
				aConnection.getMetadata().disableOutput();
				result.addMessage(ResourceMgr.getString("MsgDbmsOutputDisabled"));
			}
			else if (words[2].equalsIgnoreCase("on"))
			{
				aConnection.getMetadata().enableOutput();
				result.addMessage(ResourceMgr.getString("MsgDbmsOutputEnabled"));
			}
			else
			{
				result.setFailure();
				result.addMessage(ResourceMgr.getString("ErrorServeroutputWrongParameter"));
			}
		}
		else
		{
			result.setFailure();
		}
		return result;
	}

	private StatementRunnerResult setAutocommit(WbConnection aConnection, String[] words)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (words.length <= 2)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrorAutocommitWrongParameter"));
			return result;
		}

		try
		{
			if ("off".equalsIgnoreCase(words[2]) || "false".equalsIgnoreCase(words[2]))
			{
				aConnection.setAutoCommit(false);
				result.addMessage(ResourceMgr.getString("MsgAutocommitDisabled"));
				result.setSuccess();
			}
			else if ("on".equalsIgnoreCase(words[2]) || "true".equalsIgnoreCase(words[2]))
			{
				aConnection.setAutoCommit(true);
				result.addMessage(ResourceMgr.getString("MsgAutocommitEnabled"));
				result.setSuccess();
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrorAutocommitWrongParameter"));
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

	private StatementRunnerResult setFeedback(WbConnection aConnection, String[] words)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		if (words.length <= 2)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrorFeedbackWrongParameter"));
			return result;
		}

		if ("off".equalsIgnoreCase(words[2]) || "false".equalsIgnoreCase(words[2]))
		{
			this.runner.setVerboseLogging(false);
			result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
			result.setSuccess();
		}
		else if ("on".equalsIgnoreCase(words[2]) || "true".equalsIgnoreCase(words[2]))
		{
			this.runner.setVerboseLogging(true);
			result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorFeedbackWrongParameter"));
			result.setFailure();
		}

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

}
