/*
 * WbMode.java
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

import workbench.db.ConnectionProfile;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.ArgumentParser;

/**
 * A SQL Statement to change the read only mode of the current profile.
 *
 * @author Thomas Kellerer
 */
public class WbMode
	extends SqlCommand
{
	public static final String VERB = "WbMode";

	public WbMode()
	{
		super();
		isUpdatingCommand = false;

		// Support auto-completion of parameters
		cmdLine = new ArgumentParser(false);
		cmdLine.addArgument("status");
		cmdLine.addArgument("readonly");
		cmdLine.addArgument("normal");
		cmdLine.addArgument("reset");
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return true;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();

		ConnectionProfile profile = (currentConnection != null ? currentConnection.getProfile() : null);
		if (profile == null)
		{
			result.setFailure();
			result.addMessageByKey("TxtNotConnected"); // should not happen
			return result;
		}

		String command = null;
		try
		{
			SQLLexer l = SQLLexerFactory.createLexer(currentConnection, sql);
			SQLToken t = l.getNextToken(false, false); // ignore the verb
			t = l.getNextToken(false, false);
			if (t != null)
			{
				command = t.getContents();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (command == null)
		{
			result.addMessageByKey("ErrModeWrongArgs");
			result.setFailure();
			return result;
		}

		if (command.equalsIgnoreCase("reset"))
		{
			currentConnection.resetSessionFlags();
		}
		else if (command.equalsIgnoreCase("status"))
		{
			// only trigger display of current state
			result.setSuccess();
		}
		else if (command.equalsIgnoreCase("normal"))
		{
			currentConnection.setSessionConfirmUpdate(false);
			currentConnection.setSessionReadOnly(false);
		}
		else if (command.equalsIgnoreCase("readonly"))
		{
			currentConnection.setSessionReadOnly(true);
		}
		else if (command.equalsIgnoreCase("confirm"))
		{
			currentConnection.setSessionConfirmUpdate(true);
		}
		else
		{
			result.setFailure();
		}

		if (result.isSuccess())
		{
			result.addMessageByKey("MsgModeSession", currentConnection.isSessionReadOnly(), currentConnection.confirmUpdatesInSession());
		}
		else
		{
			result.addMessageByKey("ErrModeWrongArgs");
		}
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
