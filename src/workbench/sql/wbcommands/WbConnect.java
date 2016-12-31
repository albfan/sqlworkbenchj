/*
 * WbConnect.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * Change the active connection for a SQL script or in Console mode.
 * <br>
 * When running in GUI mode, this will only change the connection as long
 * as the script is being executed. In console mode, this will change the
 * connection permanently.
 *
 * @author Thomas Kellerer
 */
public class WbConnect
	extends SqlCommand
{
	private static int connectionId;
	private boolean persistentChange = false;

	public static final String VERB = "WbConnect";

	public WbConnect()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(AppArguments.ARG_PROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(AppArguments.ARG_PROFILE_GROUP);
		cmdLine.addArgument(AppArguments.ARG_CONN_URL);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER_CLASS);
		cmdLine.addArgument(AppArguments.ARG_CONN_JAR);
		cmdLine.addArgument(AppArguments.ARG_CONN_USER);
		cmdLine.addArgument(AppArguments.ARG_CONN_PWD);
		cmdLine.addArgument(AppArguments.ARG_CONN_FETCHSIZE);
		cmdLine.addArgument(AppArguments.ARG_CONN_AUTOCOMMIT, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_ROLLBACK, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_TRIM_CHAR, ArgumentType.BoolArgument);
    cmdLine.addArgument(AppArguments.ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_DESCRIPTOR);
		cmdLine.addArgument(AppArguments.ARG_ALT_DELIMITER);
	}

	@Override
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
			profile = BatchRunner.createCmdLineProfile(cmdLine, currentConnection, getBaseDir());
		}
		else
		{
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			profile = ConnectionMgr.getInstance().getProfile(new ProfileKey(profName, group));
		}

		if (profile == null)
		{
			result.addMessageByKey("ErrConnNoArgs");
			return result;
		}

		BatchRunner.loginPrompt(profile, this.runner.getExecutionController());

		WbConnection newConn = null;
		try
		{
			connectionId ++;
			String id = null;
			if (WbManager.getInstance().isConsoleMode())
			{
				id = "Console-" + connectionId;
			}
			else
			{

				id = "batch-connect-" + connectionId;
			}

			WbConnection current = null;

			// persistentChange will be activated by SQLConsole
			// in that case we need to disconnect the current connection
			// as the statement runner will not close the current connection
			if (persistentChange)
			{
				current = runner.getConnection();
			}

			newConn = ConnectionMgr.getInstance().getConnection(profile, id);
			LogMgr.logInfo("WbConnect.execute()", "Connected to: " + newConn.getDisplayString());

			if (persistentChange)
			{
				if (current != null && current != newConn) current.disconnect();
				runner.setConnection(newConn);
			}
			else
			{
				// The runner will switch back to the original connection automatically once
				// the current script has ended.
				runner.changeConnection(newConn);
			}
			result.addMessageByKey("MsgBatchConnectOk", newConn.getDisplayString());
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
			result.addErrorMessage(err);
		}
    finally
    {
      if (!profile.getStorePassword())
      {
        // clear any password that was entered by the user during prompting
        profile.setInputPassword(null);
      }
    }

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
