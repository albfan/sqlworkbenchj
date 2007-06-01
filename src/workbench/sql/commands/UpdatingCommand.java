/*
 * UpdatingCommand.java
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

import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.LobFileStatement;

/**
 * @author  support@sql-workbench.net
 */
public class UpdatingCommand extends SqlCommand
{
	public static final SqlCommand UPDATE = new UpdatingCommand("UPDATE");
	public static final SqlCommand DELETE = new UpdatingCommand("DELETE");
	public static final SqlCommand INSERT = new UpdatingCommand("INSERT");
	public static final SqlCommand TRUNCATE = new UpdatingCommand("TRUNCATE");

	private String verb;
	private boolean checkLobParameter = false;
	
	public UpdatingCommand(String aVerb)
	{
		this.verb = aVerb;
		this.isUpdatingCommand = true;
		checkLobParameter = aVerb.equals("UPDATE") || aVerb.equals("INSERT");
	}

	public StatementRunnerResult execute(WbConnection aConnection, String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		
		try
		{
			boolean isPrepared = false;
			LobFileStatement lob = null;
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
			
			if (lob != null && lob.containsParameter())
			{
				isPrepared = true;
				this.currentStatement = lob.prepareStatement(aConnection.getSqlConnection());
			}
			else if (Settings.getInstance().getCheckPreparedStatements() &&
					aConnection.getPreparedStatementPool().isRegistered(sql))
			{
				this.currentStatement = aConnection.getPreparedStatementPool().prepareStatement(sql);
				isPrepared = true;
			}
			else
			{
				this.currentStatement = aConnection.createStatement();
			}

			if (isPrepared)
			{
				((PreparedStatement)this.currentStatement).executeUpdate();
			}
			else
			{
				this.currentStatement.executeUpdate(sql);
			}
			appendSuccessMessage(result);
			result.setSuccess();
			processResults(result, false);
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);
			LogMgr.logSqlError("UpdatingCommnad.execute()", sql, e);
		}
		finally
		{
			this.done();
		}
		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}
