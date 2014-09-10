/*
 * WbIsolationLevel.java
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A SQL Command to set or display the current isolation level in a DBMS independent manner.
 *
 * @author Thomas Kellerer
 */
public class WbIsolationLevel
	extends SqlCommand
{
	public static final String VERB = "WbIsolationLevel";
	private final Map<String, Integer> levelMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

	public WbIsolationLevel()
	{
		levelMap.put("read_committed", Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED));
		levelMap.put("read_uncommitted", Integer.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));
		levelMap.put("serializable", Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE));
		levelMap.put("repeatable_read", Integer.valueOf(Connection.TRANSACTION_REPEATABLE_READ));
		levelMap.put("none", Integer.valueOf(Connection.TRANSACTION_NONE));

		// add support for auto-completion
		cmdLine = new ArgumentParser(false);
		for (String key : levelMap.keySet())
		{
			cmdLine.addArgument(key);
		}
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String parameter = getCommandLine(sql);
		if (StringUtil.isBlank(parameter))
		{
			String level = currentConnection.getIsolationLevelName();
			result.addMessage(ResourceMgr.getFormattedString("MsgLevelCurrent", level));
			result.setSuccess();
			return result;
		}

		int level = stringToLevel(parameter);
		if (level == -1)
		{
			result.addMessage(ResourceMgr.getFormattedString("MsgLevelUnknown", parameter));
			result.setFailure();
		}
		else
		{
			try
			{
				boolean supported = currentConnection.getSqlConnection().getMetaData().supportsTransactionIsolationLevel(level);
				currentConnection.getSqlConnection().setTransactionIsolation(level);
				result.setSuccess();
				result.addMessage(ResourceMgr.getFormattedString("MsgLevelChanged", currentConnection.getIsolationLevelName()));
				if (!supported)
				{
					result.addMessage(ResourceMgr.getFormattedString("MsgLevelNotSupported", SqlUtil.getIsolationLevelName(level)));
					result.setWarning(true);
				}
			}
			catch (SQLException e)
			{
				LogMgr.logError("WbIsolationLevel.execute()", "Could not set isolation level", e);
				result.addMessage(ResourceMgr.getFormattedString("MsgLevelChangeError", e.getMessage()));
				result.setFailure();
			}
		}
		return result;
	}

	protected int stringToLevel(String arg)
	{
		arg = arg.trim();
		arg = arg.replaceAll("\\s+", " ");
		arg = arg.replaceAll(" ", "_");
		Integer level = levelMap.get(arg);
		if (level == null) return -1;
		return level.intValue();
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
