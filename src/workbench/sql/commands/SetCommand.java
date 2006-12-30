/*
 * SetCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
				// those SET commands that have a SQL Workbench equivalent
				// will be "executed" by calling the approriate functions
				// we don't need to send the SQL to the server in this case
				// everything else is sent to the server
				if (command.equalsIgnoreCase("autocommit"))
				{
					result = this.setAutocommit(aConnection, param);
					execSql = false;
				}
				else if (aConnection.getMetadata().isOracle())
				{
					if (command.equalsIgnoreCase("serveroutput"))
					{
						result = this.setServeroutput(aConnection, param);
						execSql = false;
					}
					else if (command.equalsIgnoreCase("feedback"))
					{
						result = this.setFeedback(param);
						execSql = false;
					}
				}
			}

			if (execSql)
			{
				result = new StatementRunnerResult();
				this.currentStatement = aConnection.createStatement();
				this.currentStatement.execute(aSql);
				StringBuilder warnings = new StringBuilder();
				if (this.appendWarnings(aConnection, this.currentStatement , warnings))
				{
					result.setWarning(true);
					result.addMessage(warnings.toString());
				}
			}
			String regex = "set\\s*(current|)\\s*schema";
			Matcher m = Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(aSql);
			// I'm not using the Lexer to test this, because Oracle's Syntax 
			// includes an ALTER SESSION
			if (m.find())
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
			result.addMessage(ResourceMgr.getString("MsgDbmsOutputDisabled"));
		}
		else if ("on".equalsIgnoreCase(param))
		{
			aConnection.getMetadata().enableOutput();
			result.addMessage(ResourceMgr.getString("MsgDbmsOutputEnabled"));
		}
		else
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrServeroutputWrongParameter"));
		}
		return result;
	}

	private StatementRunnerResult setAutocommit(WbConnection aConnection, String param)
	{
		StatementRunnerResult result = new StatementRunnerResult();
		
		if (StringUtil.isEmptyString(param))
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrAutocommitWrongParameter"));
			return result;
		}

		try
		{
			if ("off".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param))
			{
				aConnection.setAutoCommit(false);
				result.addMessage(ResourceMgr.getString("MsgAutocommitDisabled"));
				result.setSuccess();
			}
			else if ("on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param))
			{
				aConnection.setAutoCommit(true);
				result.addMessage(ResourceMgr.getString("MsgAutocommitEnabled"));
				result.setSuccess();
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrAutocommitWrongParameter"));
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
			result.addMessage(ResourceMgr.getString("ErrFeedbackWrongParameter"));
			return result;
		}

		if ("off".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param))
		{
			this.runner.setVerboseLogging(false);
			result.addMessage(ResourceMgr.getString("MsgFeedbackDisabled"));
			result.setSuccess();
		}
		else if ("on".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param))
		{
			this.runner.setVerboseLogging(true);
			result.addMessage(ResourceMgr.getString("MsgFeedbackEnabled"));
			result.setSuccess();
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrFeedbackWrongParameter"));
			result.setFailure();
		}

		return result;
	}

	public String getVerb()
	{
		return VERB;
	}

}
