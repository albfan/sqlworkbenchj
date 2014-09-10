/*
 * WbFetchSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * A SQL Statement to change the default fetch size for the current connection.
 * <br/>
 * Setting the default fetch size using this command will overwrite the setting
 * in the connection profile, but will not change the connection profile.
 *
 * @author Thomas Kellerer
 * @see workbench.db.WbConnection#setFetchSize(int)
 */
public class WbFetchSize
	extends SqlCommand
{
	public static final String VERB = "WbFetchSize";

	public WbFetchSize()
	{
		super();
		isUpdatingCommand = false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String value = getCommandLine(sql);
		int size = -1;

		try
		{
			size = Integer.parseInt(value);
		}
		catch (Exception e)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getFormattedString("ErrInvalidNumber", value));
			return result;
		}

		currentConnection.setFetchSize(size);
		result.addMessage(ResourceMgr.getFormattedString("MsgFetchSizeChanged", Integer.toString(currentConnection.getFetchSize())));
		result.setSuccess();

		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
