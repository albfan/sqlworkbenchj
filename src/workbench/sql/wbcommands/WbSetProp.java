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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSetProp
	extends SqlCommand
{
	public static final String VERB = "WBSETPROP";
	public static final String ALTERNATE_VERB = "WBSETCONFIG";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_PROP = "property";
	public static final String PARAM_VALUE = "value";
	private final Map<String, String> configMap = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);

	public WbSetProp()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_TYPE, CollectionUtil.arrayList("temp","default"));
		cmdLine.addArgument(PARAM_PROP);
		cmdLine.addArgument(PARAM_VALUE);
		configMap.put("nulldisplay", "workbench.console.nullstring");
		configMap.put("varsuffix", Settings.PROPERTY_VAR_SUFFIX);
		configMap.put("varprefix", Settings.PROPERTY_VAR_PREFIX);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String verb = SqlUtil.getSqlVerb(sql);
		boolean isConfig = verb.equalsIgnoreCase(ALTERNATE_VERB);
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasArguments())
		{
			String type = cmdLine.getValue(PARAM_TYPE, "temp");
			String prop = cmdLine.getValue(PARAM_PROP);
			String value = null;
			int pos = prop.indexOf(':');
			if (pos < 0)
			{
				cmdLine.getValue(PARAM_VALUE);
			}
			else
			{
				value = prop.substring(pos + 1);
				prop = prop.substring(0, pos);
			}

			if (prop == null)
			{
				result.setFailure();
				result.addMessage("Property name required!");
			}
			else if (value == null && !cmdLine.isArgPresent(PARAM_VALUE))
			{
				String currentValue = Settings.getInstance().getProperty(prop, "");
				result.addMessage(prop + "=" + currentValue);
			}
			else
			{
				if ("default".equals(type) && prop.startsWith("workbench"))
				{
					Settings.getInstance().setProperty(prop, value);
					result.addMessage(prop  + " permanently set to "  + value);
				}
				else
				{
					Settings.getInstance().setTemporaryProperty(prop, value);
					result.addMessage(prop  + " set to "  + value);
				}
			}
		}
		else if (args.indexOf('=') > -1)
		{
			String[] pair = args.split("=");
			if (pair.length == 2)
			{
				String prop	= getPropertyName(pair[0]);
				String value = pair[1];
				if (isConfig && prop.startsWith("workbench"))
				{
					Settings.getInstance().setProperty(prop, value);
					result.addMessage(prop  + " permanently set to "  + value);
					LogMgr.logInfo("WbSetConfig.execute()", "Changed configuration property: " + prop + "=" + value);
					Settings.getInstance().setCreatBackupOnSave(true);
				}
				else
				{
					Settings.getInstance().setTemporaryProperty(prop, value);
					result.addMessage(prop  + " set to "  + value);
				}
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
			DataStore ds = WbSysProps.getWbProperties(args);
			result.addDataStore(ds);
		}
		result.setSuccess();
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
	public String getAlternateVerb()
	{
		return ALTERNATE_VERB;
	}


	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}


}
