/*
 * UpdatingCommand.java
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
	public static SqlCommand getUpdateCommand()
	{
		return new UpdatingCommand("UPDATE");
	}

	public static SqlCommand getDeleteCommand()
	{
		return new UpdatingCommand("DELETE");
	}

	public static SqlCommand getInsertCommand()
	{
		return new UpdatingCommand("INSERT");
	}

	public static SqlCommand getTruncateCommand()
	{
		return new UpdatingCommand("TRUNCATE");
	}

	private String verb;
	private boolean checkLobParameter;

	private UpdatingCommand(String sqlVerb)
	{
		super();
		this.verb = sqlVerb;
		this.isUpdatingCommand = true;
		checkLobParameter = sqlVerb.equals("UPDATE") || sqlVerb.equals("INSERT");
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
					lob = new LobFileStatement(sql, getBaseDir());
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
			else if (Settings.getInstance().getCheckPreparedStatements() &&	currentConnection.getPreparedStatementPool().isRegistered(sql))
			{
				this.currentStatement = currentConnection.getPreparedStatementPool().prepareStatement(sql);
				isPrepared = true;
			}
			else
			{
				this.currentStatement = currentConnection.createStatement();
			}

			boolean hasResult = false;
			boolean supportsResultSets = currentConnection.getDbSettings().supportsResultSetsWithDML();
			int updateCount = -1;

			if (isPrepared)
			{
				hasResult = ((PreparedStatement)this.currentStatement).execute();
			}
			else if (supportsResultSets)
			{
				hasResult = this.currentStatement.execute(sql);
			}
			else
			{
				updateCount = currentStatement.executeUpdate(sql);
			}

			appendSuccessMessage(result);
			result.setSuccess();

			// adding the result/update count should be done after adding the success message
			// to the StatementRunnerResult object
			if (supportsResultSets || isPrepared)
			{
				processResults(result, hasResult);
			}
			else if (updateCount > -1)
			{
				result.addUpdateCountMsg(updateCount);
			}

			runner.releaseSavepoint();
		}
		catch (Exception e)
		{
			runner.rollbackSavepoint();
			addErrorInfo(result, sql, e);
			LogMgr.logUserSqlError("UpdatingCommnad.execute()", sql, e);
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
