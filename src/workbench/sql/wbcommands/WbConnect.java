/*
 * WbConnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.AppArguments;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.interfaces.ExecutionController;
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
 * Change the active connection for a SQL script or in Console mode.
 * 
 * @author support@sql-workbench.net
 */
public class WbConnect
	extends SqlCommand
{
	private static int connectionId;
	private boolean persistentChange = false;

	public static final String VERB = "WBCONNECT";

	public WbConnect()
	{
		super();
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
		return VERB;
	}

	public void setPersistentChange(boolean flag)
	{
		this.persistentChange = flag;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String aSql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setFailure();

		String args = getCommandLine(aSql);
		cmdLine.parse(args);

		ConnectionProfile profile = null;
		String profName = null;

		// Allow to directly specify a profile name without parameters
		if (cmdLine.getArgumentCount() == 0)
		{
			profName = StringUtil.trimQuotes(args);
		}
		else
		{
			profName = cmdLine.getValue(AppArguments.ARG_PROFILE);
		}

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

		if (!profile.getStorePassword())
		{
			ExecutionController controller = this.runner.getExecutionController();
			if (controller == null)
			{
				result.addMessage(ResourceMgr.getString("ErrConnectNoPwd"));
				result.setFailure();
				return result;
			}
			else
			{
				String pwd = controller.getPassword(ResourceMgr.getString("MsgInputPwd"));
				profile.setInputPassword(pwd);
			}
		}

		WbConnection newConn = null;
		try
		{
			connectionId ++;
			String id = "batch-connect-" + connectionId;

			// persistentChange will be activated by SQLConsole
			// in that case we need to disconnect the current connection
			// as the statement runner will not close the current connection
			if (persistentChange)
			{
				// The statement runner will not close the current connection
				WbConnection current = runner.getConnection();
				if (current != null && current != newConn) current.disconnect();
			}

			newConn = ConnectionMgr.getInstance().getConnection(profile, id);
			LogMgr.logInfo("WbConnect.execute()", "Connected to: " + newConn.getDisplayString());

			if (persistentChange)
			{
				this.runner.setConnection(newConn);
			}
			else
			{
				// The runner will switch back to the original connection automatically once
				// the current script has ended.
				this.runner.changeConnection(newConn);
			}
			result.addMessage(ResourceMgr.getFormattedString("MsgBatchConnectOk", newConn.getDisplayString()));
			String warn = (newConn != null ? newConn.getWarnings() : null);
			if (warn != null)
			{
				result.addMessage(warn);
			}

			result.setSuccess();
		}
		catch (Exception e)
		{
			String err = ExceptionUtil.getDisplay(e);
			result.addMessage(ResourceMgr.getFormattedString("MsgBatchConnectError"));
			result.addMessage(err);
			result.setFailure();
		}

		return result;
	}

}
