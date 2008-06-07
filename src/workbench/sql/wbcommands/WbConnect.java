/*
 * WbConnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class WbConnect 
	extends SqlCommand
{
	private static int connectionId;
	
	public WbConnect()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(AppArguments.ARG_PROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(AppArguments.ARG_PROFILE_GROUP);
		cmdLine.addArgument(AppArguments.ARG_CONN_URL);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER);
		cmdLine.addArgument(AppArguments.ARG_CONN_JAR);
		cmdLine.addArgument(AppArguments.ARG_CONN_USER);
		cmdLine.addArgument(AppArguments.ARG_CONN_PWD);
		cmdLine.addArgument(AppArguments.ARG_CONN_AUTOCOMMIT, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_ROLLBACK, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_TRIM_CHAR, ArgumentType.BoolArgument);
	}

	public String getVerb()
	{
		return "WBCONNECT";
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setFailure();
		
		if (!WbManager.getInstance().isBatchMode())
		{
			result.addMessage(ResourceMgr.getString("ErrConnNoBatch"));
			return result;
		}
		
		String args = getCommandLine(aSql);
		cmdLine.parse(args);
		
		ConnectionProfile profile = null;
		String profName = cmdLine.getValue(AppArguments.ARG_PROFILE);
		if (StringUtil.isEmptyString(profName))
		{
			profile = BatchRunner.createCmdLineProfile(cmdLine);
		}
		else
		{
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			profile = ConnectionMgr.getInstance().getProfile(new ProfileKey(profName, group));
		}
		
		if (profile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrConnNoArgs"));
			return result;
		}
		
		WbConnection newConn = null;
		try
		{
			String id = null;
			
			if (runner.getConnectionClient() != null)
			{
				id = runner.getConnectionClient().getConnectionId(profile);
				runner.getConnectionClient().connectBegin(profile, null);
			}
			else
			{
				connectionId ++;
				id = "batch-connect-" + connectionId;
			}
			
			newConn = ConnectionMgr.getInstance().getConnection(profile, id);
			if (newConn != null && runner.getConnectionClient() == null)
			{
				// Disconnect the old connection "manually" if no connectionClient 
				// is available, otherwise Connectable.connectBegin() takes care of that
				WbConnection old = this.runner.getConnection();
				if (old != null) 
				{
					LogMgr.logInfo("WbConnect.execute()", "Closing old connection: " + old.getDisplayString());
					old.close();
				}
			}
			LogMgr.logInfo("WbConnect.execute()", "Connected to: " + newConn.getDisplayString());
			this.runner.setConnection(newConn);

			if (runner.getConnectionClient() != null)
			{
				runner.getConnectionClient().connected(newConn);
			}
			
			this.setConnection(newConn);
			result.addMessage(ResourceMgr.getFormattedString("MsgBatchConnectOk", newConn.getDisplayString()));
			result.setSuccess();
		}
		catch (Exception e)
		{
			String err = ExceptionUtil.getDisplay(e);
			if (runner.getConnectionClient() != null)
			{
				runner.getConnectionClient().connectFailed(err);
			}
			result.addMessage(ResourceMgr.getFormattedString("MsgBatchConnectError"));
			result.addMessage(err);
			result.setFailure();
		}
		finally
		{
			if (runner.getConnectionClient() != null)
			{
				runner.getConnectionClient().connectEnded();
			}
		}
			 
		return result;
	}
	
}
