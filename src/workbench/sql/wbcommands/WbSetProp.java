/*
 * WbSetProp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.util.Map;
import java.util.TreeMap;

import workbench.console.ConsoleSettings;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbSettings;

import workbench.storage.DataStore;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSetProp
	extends SqlCommand
{
	public static final String VERB = "WbSetProp";
	public static final String ALTERNATE_VERB = "WbSetConfig";
	public static final String ARG_TYPE = "type";
	public static final String ARG_PROP = "property";
	public static final String ARG_VALUE = "value";
	private final Map<String, String> configMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

	public WbSetProp()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_TYPE, CollectionUtil.arrayList("temp","default"));
		cmdLine.addArgument(ARG_PROP);
		cmdLine.addArgument(ARG_VALUE);
		configMap.put("nulldisplay", ConsoleSettings.PROP_NULL_STRING);
		configMap.put("nullstring", ConsoleSettings.PROP_NULL_STRING);
		configMap.put("varsuffix", Settings.PROPERTY_VAR_SUFFIX);
		configMap.put("varprefix", Settings.PROPERTY_VAR_PREFIX);
		configMap.put("debugmeta", "workbench.dbmetadata.debugmetasql");
		configMap.put("date_format", Settings.PROPERTY_DATE_FORMAT);
		configMap.put("ts_format", Settings.PROPERTY_DATETIME_FORMAT);
		configMap.put("time_format", Settings.PROPERTY_TIME_FORMAT);
		configMap.put("digits", Settings.PROPERTY_DECIMAL_DIGITS);
		configMap.put("dec_separator", Settings.PROPERTY_DECIMAL_SEP);
		configMap.put("dec_sep", Settings.PROPERTY_DECIMAL_SEP);
		configMap.put("showscriptfinish", "workbench.gui.sql.script.showtime");
		configMap.put("showendtime", "workbench.gui.sql.script.showtime");
		configMap.put("showfinishtime", "workbench.gui.sql.script.showtime");
		configMap.put("clearonrefresh", ConsoleSettings.PROP_CLEAR_SCREEN);
		configMap.put("logfileviewer", Settings.PROP_LOGFILE_VIEWER);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		String verb = getParsingUtil().getSqlVerb(sql);
		boolean isConfig = verb.equalsIgnoreCase(ALTERNATE_VERB);
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasArguments())
		{
			String type = cmdLine.getValue(ARG_TYPE, "temp");
			String prop = cmdLine.getValue(ARG_PROP);
			String value = null;
			int pos = prop != null ? prop.indexOf(':') : -1;
			if (pos < 0)
			{
				value = cmdLine.getValue(ARG_VALUE);
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
			else if (value == null && cmdLine.isArgNotPresent(ARG_VALUE))
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
				String value = StringUtil.trimQuotes(pair[1]);

				if (isConfig && prop.startsWith("workbench"))
				{
					Settings.getInstance().setProperty(prop, value);
					result.addMessage(prop  + " permanently set to \""  + value + "\"");
					LogMgr.logInfo("WbSetConfig.execute()", "Changed configuration property: " + prop + "=" + value);
					Settings.getInstance().setCreatBackupOnSave(true);
				}
				else
				{
					Settings.getInstance().setTemporaryProperty(prop, value);
					result.addMessage(prop  + " set to \""  + value + "\"");
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
			DataStore ds = WbShowProps.getWbProperties(args);
			if (ds.getRowCount() > 0)
			{
				result.addDataStore(ds);
			}
		}
		result.setSuccess();
		return result;
	}

	private String getPropertyName(String shortName)
	{
		String propName = configMap.get(shortName);
		if (propName == null) propName = shortName;
		if (currentConnection != null)
		{
			return propName.replace(DbSettings.DBID_PLACEHOLDER, this.currentConnection.getDbId());
		}
		return propName;
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

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
