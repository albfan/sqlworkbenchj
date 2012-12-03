/*
 * DdlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;


import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ErrorInformationReader;
import workbench.db.ReaderFactory;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.SqlUtil.DdlObjectInfo;
import workbench.util.StringUtil;

/**
 * Run a DDL (CREATE, DROP, ALTER, GRANT, REVOKE) command.
 *
 * @author Thomas Kellerer
 */
public class DdlCommand
	extends SqlCommand
{
	// Firebird RECREATE VIEW command
	public static DdlCommand getRecreateCommand()
	{
		return new DdlCommand("RECREATE");
	}

	public static DdlCommand getCreateCommand()
	{
		return new DdlCommand("CREATE");
	}

	public static List<DdlCommand> getDdlCommands()
	{
		return CollectionUtil.readOnlyList(
			new DdlCommand("DROP"),
			getCreateCommand(),
			new DdlCommand("ALTER"),
			new DdlCommand("GRANT"),
			new DdlCommand("REVOKE"));
	}

	private String verb;
	private Savepoint ddlSavepoint;

	private DdlCommand(String sqlVerb)
	{
		super();
		this.verb = sqlVerb;
		this.isUpdatingCommand = true;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		boolean useSavepoint = currentConnection.getDbSettings().useSavePointForDDL() && !this.currentConnection.getAutoCommit();

		if (useSavepoint && !this.currentConnection.supportsSavepoints())
		{
			useSavepoint = false;
			LogMgr.logWarning("DdlCommand.execute()", "A savepoint should be used for this DDL command, but the driver does not support savepoints!");
		}

		SqlUtil.DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql);

		try
		{
			this.currentStatement = currentConnection.createStatement();

			sql = getSqlToExecute(sql);

			result.setSuccess();

			if (useSavepoint)
			{
				this.ddlSavepoint = currentConnection.setSavepoint();
			}

			boolean isDrop = isDropCommand(sql);
			if (isDrop && this.runner.getIgnoreDropErrors())
			{
				try
				{
					this.currentStatement.executeUpdate(sql);
					result.addMessage(getSuccessMessage(info, getVerb()));
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
					result.addMessage(getSuccessMessage(info, getVerb()));
				}
			}
			this.currentConnection.releaseSavepoint(ddlSavepoint);

			if (isDrop && result.isSuccess() && info != null)
			{
				Set<String> types = currentConnection.getMetadata().getObjectsWithData();
				if (types.contains(info.objectType))
				{
					currentConnection.getObjectCache().removeTable(new TableIdentifier(info.objectName, currentConnection));
				}
			}
		}
		catch (Exception e)
		{
			this.currentConnection.rollback(ddlSavepoint);
			result.clear();

			StringBuilder msg = new StringBuilder(150);
			msg.append(ResourceMgr.getString("MsgExecuteError"));
			msg.append('\n');
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
			LogMgr.logUserSqlError("DdlCommand.execute()", sql, e);
		}
		finally
		{
			// we know that we don't need the statement any longer, so to make
			// sure everything is cleaned up, we'll close it here
			done();
		}

		return result;
	}

	@Override
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

	@Override
	protected String getSuccessMessage(DdlObjectInfo info, String verb)
	{
		String msg = super.getSuccessMessage(info, getVerb());
		if (msg == null)
		{
			return getDefaultSuccessMessage(null);
		}
		return msg;
	}

	/**
	 * Retrieve extended error information if the DBMS supports this.
	 *
	 * @return true if an error was added, false otherwise
	 *
	 * @see ErrorInformationReader#getErrorInfo(String, String, String)
	 * @see ReaderFactory#getErrorInformationReader(workbench.db.WbConnection)
	 */
	private boolean addExtendErrorInfo(WbConnection aConnection, SqlUtil.DdlObjectInfo info , StatementRunnerResult result)
	{
		if (info == null) return false;
		if (aConnection == null) return false;

		ErrorInformationReader reader = ReaderFactory.getErrorInformationReader(aConnection);
		if (reader == null) return false;

		String error = reader.getErrorInfo(null, info.objectName, info.objectType, true);
		if (StringUtil.isEmptyString(error)) return false;

		result.addMessageNewLine();
		result.addMessage(error);
		return true;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

}
