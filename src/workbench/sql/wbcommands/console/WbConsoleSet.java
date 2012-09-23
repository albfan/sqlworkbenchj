/*
 * WbSysProps.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class WbConsoleSet
	extends SqlCommand
{
	public static final String VERB = "WBSET";

	/**
	 * Maps short config names to the corresponding workbench.settings property name
	 */
	private final Map<String, String> configMap = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);

	public WbConsoleSet()
	{
		super();
		cmdLine = new ArgumentParser();
		configMap.put("nulldisplay", "workbench.console.nullstring");
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (StringUtil.isBlank(args))
		{
			result.addMessage("Supported config parameters:");
			for (String key : configMap.keySet())
			{
				result.addMessage(key);
			}
		}
		else if (args.indexOf('=') > -1)
		{
			String[] pair = args.split("=");
			if (pair.length == 2)
			{
				String prop	= getPropertyName(pair[0]);
				String value = pair[1];
				Settings.getInstance().setProperty(prop, value);
				result.addMessage(prop  + " permanently set to "  + value);
				LogMgr.logInfo("WbSetConfig.execute()", "Changed configuration property: " + prop + "=" + value);
				Settings.getInstance().setCreatBackupOnSave(true);
			}
			else if (pair.length == 1)
			{
				String prop	= getPropertyName(pair[0]);
				// no value specified, remove property
				Settings.getInstance().removeProperty(prop);
				result.addMessage(prop  + " removed");
			}
		}
		else
		{
			String prop = getPropertyName(args);
			result.addMessage(prop  + "="  + Settings.getInstance().getProperty(prop, null));
		}
		return result;
	}

	private String getPropertyName(String shortName)
	{
		String longName = configMap.get(shortName);
		if (longName == null) return shortName;
		return longName;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}
}
