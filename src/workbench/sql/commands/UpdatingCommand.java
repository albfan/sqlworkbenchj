/*
 * UpdatingCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.LobFileStatement;

/**
 * Handles DML statements (UPDATE, DELETE, INSERT, TRUNCATE)
 *
 * @author Thomas Kellerer
 */
public class UpdatingCommand
	extends SqlCommand
{
	public static final SqlCommand UPDATE = new UpdatingCommand("UPDATE");
	public static final SqlCommand DELETE = new UpdatingCommand("DELETE");
	public static final SqlCommand INSERT = new UpdatingCommand("INSERT");
	public static final SqlCommand TRUNCATE = new UpdatingCommand("TRUNCATE");

	private String verb;
	private boolean checkLobParameter = false;

	public UpdatingCommand(String aVerb)
	{
		super();
		this.verb = aVerb;
		this.isUpdatingCommand = true;
		checkLobParameter = aVerb.equals("UPDATE") || aVerb.equals("INSERT");
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		LobFileStatement lob = null;

		try
		{
			boolean isPrepared = false;

			if (checkLobParameter)
			{
				try
				{
					lob = new LobFileStatement(sql, this.runner.getBaseDir());
				}
				catch (FileNotFoundException e)
				{
					result.addMessage(e.getMessage());
					result.setFailure();
					return result;
				}
			}

			runner.setSavepoint();
			sql = getSqlToExecute(sql);

			if (lob != null && lob.containsParameter())
			{
				isPrepared = true;
				this.currentStatement = lob.prepareStatement(currentConnection);
			}
			else if (Settings.getInstance().getCheckPreparedStatements() &&
					currentConnection.getPreparedStatementPool().isRegistered(sql))
			{
				this.currentStatement = currentConnection.getPreparedStatementPool().prepareStatement(sql);
				isPrepared = true;
			}
			else
			{
				this.currentStatement = currentConnection.createStatement();
			}

			boolean hasResult = false;
			if (isPrepared)
			{
				hasResult = ((PreparedStatement)this.currentStatement).execute();
			}
			else
			{
				hasResult = this.currentStatement.execute(sql);
			}
			appendSuccessMessage(result);
			result.setSuccess();
			processResults(result, hasResult);
			runner.releaseSavepoint();
		}
		catch (Exception e)
		{
			runner.rollbackSavepoint();
			addErrorInfo(result, sql, e);
			LogMgr.logError("UpdatingCommnad.execute()", sql, e);
		}
		finally
		{
			if (lob != null) lob.done();
			this.done();
		}
		return result;
	}

	@Override
	public String getVerb()
	{
		return verb;
	}

}
