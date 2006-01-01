/*
 * UpdatingCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.LobFileStatement;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class UpdatingCommand extends SqlCommand
{
	public static final SqlCommand UPDATE = new UpdatingCommand("UPDATE");
	public static final SqlCommand DELETE = new UpdatingCommand("DELETE");
	public static final SqlCommand INSERT = new UpdatingCommand("INSERT");

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
		InputStream in = null;
		
		try
		{
			boolean isPrepared = false;
			LobFileStatement lob = null;
			if (checkLobParameter)
			{
				try
				{
					lob = new LobFileStatement(sql);
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

			int updateCount = -1;
			if (isPrepared)
			{
				updateCount = ((PreparedStatement)this.currentStatement).executeUpdate();
			}
			else
			{
				updateCount = this.currentStatement.executeUpdate(sql);
			}
			result.addUpdateCount(updateCount);
			StringBuffer warnings = new StringBuffer();
			boolean hasWarnings = this.appendWarnings(aConnection, this.currentStatement, warnings);
			this.appendSuccessMessage(result);
			result.addMessage(updateCount + " " + ResourceMgr.getString("MsgRowsAffected"));
			if (hasWarnings) result.addMessage(warnings.toString());

			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();
			result.addMessage(ResourceMgr.getString("MsgExecuteError"));
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			LogMgr.logDebug("UpdatingCommnad.execute()", "Error executing statement: " + sql, e);
		}
		finally
		{
			if (in != null)
			{
				try { in.close(); } catch (Throwable th) {}
			}
			this.done();
		}
		return result;
	}

	public String getVerb()
	{
		return verb;
	}

}
