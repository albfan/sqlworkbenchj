/*
 * DdlCommand.java
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
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Run a DDL (CREATE, DROP, ALTER, GRANT, REVOKE) command.
 * 
 * @author  support@sql-workbench.net
 */
public class DdlCommand
	extends SqlCommand
{
	public static final DdlCommand CREATE = new DdlCommand("CREATE");
	public static final DdlCommand DROP = new DdlCommand("DROP");
	public static final DdlCommand ALTER = new DdlCommand("ALTER");
	public static final DdlCommand GRANT = new DdlCommand("GRANT");
	public static final DdlCommand REVOKE = new DdlCommand("REVOKE");

	// Firebird RECREATE VIEW command
	public static final DdlCommand RECREATE = new DdlCommand("RECREATE");
	public static final List<DdlCommand> DDL_COMMANDS;

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
	private Savepoint ddlSavepoint;

	private DdlCommand(String aVerb)
	{
		super();
		this.verb = aVerb;
		this.isUpdatingCommand = true;
	}

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		DbSettings dbset = this.currentConnection.getMetadata().getDbSettings();
		boolean useSavepoint = dbset.useSavePointForDDL() && !this.currentConnection.getAutoCommit();

		if (useSavepoint && !this.currentConnection.supportsSavepoints())
		{
			useSavepoint = false;
			LogMgr.logWarning("DdlCommand.execute()", "A savepoint should be used for this DDL command, but the driver does not support savepoints!");
		}

		SqlUtil.DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql);
		
		try
		{
			this.currentStatement = currentConnection.createStatement();

			sql = currentConnection.getMetadata().filterDDL(sql);
			sql = getSqlToExecute(sql);

			result.setSuccess();

			if (useSavepoint)
			{
				this.ddlSavepoint = currentConnection.setSavepoint();
			}

			if (isDropCommand(sql) && this.runner.getIgnoreDropErrors())
			{
				try
				{
					this.currentStatement.executeUpdate(sql);
					result.addMessage(getSuccessMessage(info));
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
				boolean hasResult = this.currentStatement.execute(sql);

				// Using a generic execute and result processing ensures that DBMS that
				// can process more than one statement with a single SQL are treated correctly.
				// e.g. when sending a SELECT and other statements as a "batch" with SQL Server
				processMoreResults(sql, result, hasResult);

				// Process result will have added any warnings and set the warning flag
				if (result.hasWarning())
				{
					if (this.addExtendErrorInfo(currentConnection, info, result))
					{
						result.setFailure();
					}
				}

				if (result.isSuccess())
				{
					result.addMessage(getSuccessMessage(info));
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
				msg.append(sql);
			}
			else
			{
				int maxLen = 150;
				msg.append(StringUtil.getMaxSubstring(sql.trim(), maxLen));
			}
			result.addMessage(msg);
			result.addMessageNewLine();
			result.addMessage(ExceptionUtil.getAllExceptions(e));

			addExtendErrorInfo(currentConnection, info, result);
			result.setFailure();
			LogMgr.logSqlError("DdlCommand.execute()", sql, e);
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
		if ("DROP".equals(this.verb))
		{
			return true;
		}
		if (!"ALTER".equals(this.verb))
		{
			return false;
		}
		// If this is an ALTER ... command it might also be a DROP
		// e.g. ALTER TABLE someTable DROP PRIMARY KEY
		Pattern p = Pattern.compile("DROP\\s+(PRIMARY\\s+KEY|CONSTRAINT)\\s+", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sql);
		return m.find();
	}

	private String getSuccessMessage(SqlUtil.DdlObjectInfo info)
	{
		if ("DROP".equals(verb))
		{
			if (info == null)
			{
				return ResourceMgr.getString("MsgGenDropSuccess");
			}
			return ResourceMgr.getFormattedString("MsgDropSuccess", info.getDisplayType(), info.objectName);
		}
		else if ("CREATE".equals(verb) || "RECREATE".equals(verb))
		{
			if (info == null)
			{
				return ResourceMgr.getString("MsgGenCreateSuccess");
			}
			return ResourceMgr.getFormattedString("MsgCreateSuccess", info.getDisplayType(), info.objectName);
		}
		return getDefaultSuccessMessage();
	}

	/**
	 * Retrieve extended error information if the DBMS supports this.
	 * Currently this is only implemented for Oracle to read errors
	 * after creating a stored procedure from the ALL_ERRORS view.
	 *
	 * @see #getObjectName(String)
	 * @see #getObjectType(String)
	 */
	private boolean addExtendErrorInfo(WbConnection aConnection, SqlUtil.DdlObjectInfo info , StatementRunnerResult result)
	{
		if (info == null) return false;

		String msg = aConnection.getMetadata().getExtendedErrorInfo(null, info.objectName, info.objectType);
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
