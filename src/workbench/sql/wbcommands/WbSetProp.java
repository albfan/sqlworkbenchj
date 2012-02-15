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
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSetProp
	extends SqlCommand
{
	public static final String VERB = "WBSETPROP";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_PROP = "property";
	public static final String PARAM_VALUE = "value";

	public WbSetProp()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_TYPE, CollectionUtil.arrayList("temp","default"));
		cmdLine.addArgument(PARAM_PROP);
		cmdLine.addArgument(PARAM_VALUE);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasArguments())
		{
			String type = cmdLine.getValue(PARAM_TYPE, "temp");
			String prop = cmdLine.getValue(PARAM_PROP);
			String value = cmdLine.getValue(PARAM_VALUE);
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
					System.setProperty(prop, value);
					result.addMessage(prop  + " set to "  + value);
				}
			}
		}
		else if (args.indexOf('=') > -1)
		{
			String[] pair = args.split("=");
			if (pair.length== 2)
			{
				System.setProperty(pair[0], pair[1]);
				result.addMessage(pair[0]  + " set to "  + pair[1]);
			}
		}
		else
		{
			String value = Settings.getInstance().getProperty(args, "");
			result.addMessage(args + "=" + value);
		}
		result.setSuccess();
		return result;
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
