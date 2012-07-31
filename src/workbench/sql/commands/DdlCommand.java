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
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.CollectionUtil;
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
	public static final DdlCommand CREATE = new DdlCommand("CREATE");
	public static final DdlCommand DROP = new DdlCommand("DROP");
	public static final DdlCommand ALTER = new DdlCommand("ALTER");
	public static final DdlCommand GRANT = new DdlCommand("GRANT");
	public static final DdlCommand REVOKE = new DdlCommand("REVOKE");

	// Firebird RECREATE VIEW command
	public static final DdlCommand RECREATE = new DdlCommand("RECREATE");
	public static final List<DdlCommand> DDL_COMMANDS = CollectionUtil.readOnlyList(DROP, CREATE, ALTER, GRANT, REVOKE);

	private String verb;
	private Savepoint ddlSavepoint;

	private DdlCommand(String aVerb)
	{
		super();
		this.verb = aVerb;
		this.isUpdatingCommand = true;
	}

	@Override
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
			LogMgr.logError("DdlCommand.execute()", sql, e);
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
	 * Currently this is only implemented for Oracle to read errors
	 * after creating a stored procedure from the ALL_ERRORS view.
	 *
	 * @see DbMetadata#getExtendedErrorInfo(String, String, String)
	 */
	private boolean addExtendErrorInfo(WbConnection aConnection, SqlUtil.DdlObjectInfo info , StatementRunnerResult result)
	{
		if (info == null) return false;
		if (aConnection == null) return false;

		DbMetadata meta = aConnection.getMetadata();
		if (meta == null) return false;

		String msg = meta.getExtendedErrorInfo(null, info.objectName, info.objectType, true);
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

	@Override
	public String getVerb()
	{
		return verb;
	}

}
