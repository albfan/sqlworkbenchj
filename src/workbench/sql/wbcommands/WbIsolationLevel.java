/*
 * WbIsolationLevel
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
import workbench.util.CaseInsensitiveComparator;
import workbench.util.StringUtil;

/**
 * A SQL Command to set or display the current isolation level in a DBMS independent manner.
 * 
 * @author Thomas Kellerer
 */
public class WbIsolationLevel
	extends SqlCommand
{
	public static final String VERB = "WBISOLATIONLEVEL";
	private final Map<String, Integer> levelMap = new TreeMap<String, Integer>(CaseInsensitiveComparator.INSTANCE);
	
	public WbIsolationLevel()
	{
		levelMap.put("read committed", Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED));
		levelMap.put("read uncommitted", Integer.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));
		levelMap.put("serializable", Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE));
		levelMap.put("repeatable read", Integer.valueOf(Connection.TRANSACTION_REPEATABLE_READ));
		levelMap.put("none", Integer.valueOf(Connection.TRANSACTION_NONE));
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
			String level = currentConnection.getIsolationLevel();
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
				currentConnection.getSqlConnection().setTransactionIsolation(level);
				result.setSuccess();
				result.addMessage(ResourceMgr.getFormattedString("MsgLevelChanged", currentConnection.getIsolationLevel()));
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
		arg = arg.replaceAll("_", " ");
		arg = arg.replaceAll("\\s+", " ");
		arg = arg.trim();
		Integer level = levelMap.get(arg);
		if (level == null) return -1;
		return level.intValue();
	}
}
