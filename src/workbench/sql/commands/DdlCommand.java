/*
 * DdlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DdlCommand extends SqlCommand
{
	public static final DdlCommand CREATE = new DdlCommand("CREATE");
	public static final DdlCommand DROP = new DdlCommand("DROP");
	public static final DdlCommand ALTER = new DdlCommand("ALTER");
	public static final DdlCommand GRANT = new DdlCommand("GRANT");
	public static final DdlCommand REVOKE = new DdlCommand("REVOKE");

	// Firebird RECREATE VIEW command
	public static final SqlCommand RECREATE = new DdlCommand("RECREATE");

	public static final List<DdlCommand> DDL_COMMANDS;

	private Savepoint ddlSavepoint;

	static
	{
		List<DdlCommand> l = new ArrayList<DdlCommand>(5);
		l.add(DROP);
		l.add(CREATE);
		l.add(ALTER);
		l.add(GRANT);
		l.add(REVOKE);
		DDL_COMMANDS = Collections.unmodifiableList(l);
	}

	private String verb;

	private DdlCommand(String aVerb)
	{
		this.verb = aVerb;
		this.isUpdatingCommand = true;
	}

	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		DbSettings dbset = this.currentConnection.getMetadata().getDbSettings();
		boolean useSavepoint = dbset.useSavePointForDDL() && !this.currentConnection.getAutoCommit();

		if (useSavepoint && !this.currentConnection.supportsSavepoints())
		{
			useSavepoint = false;
			LogMgr.logWarning("DdlCommand.execute()", "A savepoint should be used for this DDL command, but the driver does not support savepoints!");
		}

		try
		{
			this.currentStatement = currentConnection.createStatement();

			aSql = currentConnection.getMetadata().filterDDL(aSql);

			result.setSuccess();

			if (useSavepoint)
			{
				this.ddlSavepoint = currentConnection.setSavepoint();
			}

			if (isDropCommand(aSql) && this.runner.getIgnoreDropErrors())
			{
				try
				{
					this.currentStatement.executeUpdate(aSql);
					result.addMessage(ResourceMgr.getString("MsgDropSuccess"));
				}
				catch (Exception th)
				{
					this.currentConnection.rollback(ddlSavepoint);
					this.ddlSavepoint = null;
					result.addMessage(ResourceMgr.getString("MsgDropWarning"));
					result.addMessage(ExceptionUtil.getDisplay(th));
					result.setSuccess();
				}
			}
			else
			{
				boolean hasResult = this.currentStatement.execute(aSql);

				// Using a generic execute and result processing ensures that DBMS that
				// can process more than one statement with a single SQL are treated correctly.
				// e.g. when sending a SELECT and other statements as a "batch" with SQL Server
				processMoreResults(aSql, result, hasResult);

				// Process result will have added any warnings and set the warning flag
				if (result.hasWarning())
				{
					if (this.addExtendErrorInfo(currentConnection, aSql, result))
					{
						result.setFailure();
					}
				}

				if (result.isSuccess())
				{
					if ("DROP".equals(verb))
					{
						result.addMessage(ResourceMgr.getString("MsgDropSuccess"));
					}
					else if ("CREATE".equals(verb) || "RECREATE".equals(verb))
					{
						result.addMessage(ResourceMgr.getString("MsgCreateSuccess"));
					}
					else
					{
						result.addMessage(this.verb + " " + ResourceMgr.getString("MsgKnownStatementOK"));
					}
				}

			}
			this.currentConnection.releaseSavepoint(ddlSavepoint);
		}
		catch (Exception e)
		{
			this.currentConnection.rollback(ddlSavepoint);
			result.clear();

			StringBuilder msg = new StringBuilder(150);
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			if (reportFullStatementOnError)
			{
				msg.append(aSql);
			}
			else
			{
				int maxLen = 150;
				msg.append(StringUtil.getMaxSubstring(aSql.trim(), maxLen));
			}
			result.addMessage(msg);
			result.addMessageNewLine();
			result.addMessage(ExceptionUtil.getAllExceptions(e));

			addExtendErrorInfo(currentConnection, aSql, result);
			result.setFailure();
			LogMgr.logSqlError("DdlCommand.execute()", aSql, e);
		}
		finally
		{
			// we know that we don't need the statement any longer, so to make
			// sure everything is cleaned up, we'll close it here
			done();
		}

		return result;
	}

	public void done()
	{
		super.done();
		this.ddlSavepoint = null;
	}

	public boolean isDropCommand(String sql)
	{
		if ("DROP".equals(this.verb)) return true;
		if (!"ALTER".equals(this.verb)) return false;
		// If this is an ALTER ... command it might also be a DROP 
		// e.g. ALTER TABLE someTable DROP PRIMARY KEY
		Pattern p = Pattern.compile("DROP\\s+(PRIMARY\\s+KEY|CONSTRAINT)\\s+", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sql);
		return m.find();
	}

	/**
	 * Extract the name of the created object for Oracle stored procedures.
	 * @see #addExtendErrorInfo(workbench.db.WbConnection, String, workbench.sql.StatementRunnerResult)
	 */
	protected String getObjectName(String sql)
	{
		SQLLexer l = new SQLLexer(sql);
		SQLToken t = l.getNextToken(false, false);
		if (t == null) return null;
		String v = t.getContents();
		if (!v.equals("CREATE") && !v.equals("CREATE OR REPLACE")) return null;

		// next token must be the type
		t = l.getNextToken(false, false);
		if (t == null) return null;

		// the token after the type must be the object's name
		t = l.getNextToken(false, false);
		return t.getContents();
	}

	/**
	 * Retrieve extended error information if the DBMS supports this.
	 * Currently this is only implemented for Oracle to read errors
	 * after creating a stored procedure from the ALL_ERRORS view.
	 *
	 * @see #getObjectName(String)
	 * @see #getObjectType(String)
	 */
	private boolean addExtendErrorInfo(WbConnection aConnection, String sql, StatementRunnerResult result)
	{
		String type = SqlUtil.getCreateType(sql);
		if (type == null) return false;

		String name = getObjectName(sql);
		if (name == null) return false;

		String msg = aConnection.getMetadata().getExtendedErrorInfo(null, name, type);
		if (msg != null && msg.length() > 0)
		{
			result.addMessageNewLine();
			result.addMessage(msg);
			return true;
		}
		else
		{
			return false;
		}
  }

	public String getVerb()
	{
		return verb;
	}

}
