/*
 * AppArguments.java
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
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * A class to define and parse the arguments that are available when
 * the application is started.
 *
 * @author Thomas Kellerer
 */
public class AppArguments
	extends ArgumentParser
{
	// Parameters for batch execution used by BatchRunner
	public static final String ARG_SCRIPT = "script";
	public static final String ARG_COMMAND = "command";
	public static final String ARG_SCRIPT_ENCODING = "encoding";
	public static final String ARG_ABORT = "abortOnError";
	public static final String ARG_SHOWPROGRESS = "showProgress";

	// Connection related parameters
	public static final String ARG_PROFILE = "profile";
	public static final String ARG_PROFILE_GROUP = "profilegroup";
	public static final String ARG_CONN_URL = "url";
	public static final String ARG_CONN_DRIVER = "driver";
	public static final String ARG_CONN_DRIVER_CLASS = "driverclass";
	public static final String ARG_CONN_JAR = "driverjar";
	public static final String ARG_CONN_USER = "username";
	public static final String ARG_CONN_PWD = "password";
	public static final String ARG_CONN_PROPS = "connectionProperties";
	public static final String ARG_CONN_DESCRIPTOR = "connection";
	public static final String ARG_CONN_AUTOCOMMIT = "autocommit";
	public static final String ARG_CONN_SEPARATE = "separateConnection";
	public static final String ARG_CONN_EMPTYNULL = "emptyStringIsNull";
	public static final String ARG_CONN_ROLLBACK = "rollbackOnDisconnect";
	public static final String ARG_CONN_CHECK_OPEN_TRANS = "checkUncommitted";
	public static final String ARG_CONN_TRIM_CHAR = "trimCharData";
	public static final String ARG_CONN_FETCHSIZE = "fetchSize";
	public static final String ARG_IGNORE_DROP = "ignoreDropErrors";
	public static final String ARG_READ_ONLY = "readOnly";
	public static final String ARG_CONN_REMOVE_COMMENTS = "removeComments";
	public static final String ARG_HIDE_WARNINGS = "hideWarnings";
	public static final String ARG_INTERACTIVE = "interactive";
	public static final String ARG_PROPFILE = "arguments";
	public static final String ARG_LB_CONN = "lbDefaults";
	public static final String ARG_CONN_NAME = "connectionName";

	public static final String ARG_DISPLAY_RESULT = "displayResult";
	public static final String ARG_SUCCESS_SCRIPT = "cleanupSuccess";
	public static final String ARG_ERROR_SCRIPT = "cleanupError";
	public static final String ARG_SHOW_TIMING = "showTiming";
	public static final String ARG_FEEDBACK = "feedback";
	public static final String ARG_WORKSPACE = "workspace";
	public static final String ARG_ALT_DELIMITER = "altDelimiter";
	public static final String ARG_DELIMITER = "delimiter";
	public static final String ARG_CONSOLIDATE_LOG = "consolidateMessages";

	// Initial tool parameters
	public static final String ARG_SHOW_PUMPER = "dataPumper";
	public static final String ARG_SHOW_DBEXP = "dbExplorer";
	public static final String ARG_SHOW_SEARCHER = "objectSearcher";

	// Other parameters
	public static final String ARG_PROFILE_STORAGE = "profileStorage";
	public static final String ARG_MACRO_STORAGE = "macroStorage";
	public static final String ARG_CONFIGDIR = "configDir";
	public static final String ARG_LIBDIR = "libdir";
	public static final String ARG_LOGLEVEL = "logLevel";
	public static final String ARG_LOGFILE = "logfile";
	public static final String ARG_VARDEF = "varDef";
	public static final String ARG_LANG = "language";
	public static final String ARG_NOSETTNGS = "noSettings";
	public static final String ARG_NOTEMPLATES = "noTemplates";
	public static final String ARG_CONSOLE_OPT_COLS = "optimizeColWidth";
	public static final String ARG_CONSOLE_BUFFER_RESULTS = "bufferResults";
	public static final String ARG_PROP = "prop";
	public static final String ARG_LOG_ALL_STMT = "logAllStatements";

	public AppArguments()
	{
		super();
		addArgument(ARG_PROPFILE);
		addArgument(ARG_LB_CONN);
		addArgument(ARG_PROFILE, ArgumentType.ProfileArgument);
		addArgument(ARG_FEEDBACK, ArgumentType.BoolArgument);
		addArgument(ARG_PROFILE_GROUP);
		addArgument(ARG_PROFILE_STORAGE);
		addArgument(ARG_MACRO_STORAGE);
		addArgument(ARG_CONFIGDIR);
		addArgument(ARG_LIBDIR);
		addArgument(ARG_SCRIPT);
		addArgument(ARG_COMMAND);
		addArgument(ARG_SCRIPT_ENCODING);
		addArgument(ARG_LOGLEVEL);
		addArgument(ARG_LOGFILE);
		addArgument(ARG_ABORT, ArgumentType.BoolArgument);
		addArgument(ARG_SUCCESS_SCRIPT);
		addArgument(ARG_ERROR_SCRIPT);
		addArgument(ARG_VARDEF, ArgumentType.RepeatableValue);
		addArgument(ARG_CONN_URL);
		addArgument(ARG_CONN_PROPS, ArgumentType.RepeatableValue);
		addArgument(ARG_CONN_DRIVER);
		addArgument(ARG_CONN_DRIVER_CLASS);
		addArgument(ARG_CONN_JAR);
		addArgument(ARG_CONN_FETCHSIZE);
		addArgument(ARG_CONN_USER);
		addArgument(ARG_CONN_NAME);
		addArgument(ARG_CONN_PWD);
		addArgument(ARG_CONN_SEPARATE, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_EMPTYNULL, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_AUTOCOMMIT, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_REMOVE_COMMENTS, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_CHECK_OPEN_TRANS, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_ROLLBACK, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_PUMPER, ArgumentType.BoolArgument);
		addArgument(ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		addArgument(ARG_DISPLAY_RESULT, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_DBEXP, ArgumentType.BoolSwitch);
		addArgument(ARG_SHOW_SEARCHER, ArgumentType.BoolSwitch);
		addArgument(ARG_SHOW_TIMING, ArgumentType.BoolSwitch);
		addArgument(ARG_SHOWPROGRESS, ArgumentType.BoolArgument);
		addArgument(ARG_CONSOLE_OPT_COLS, ArgumentType.BoolArgument);
		addArgument(ARG_CONSOLE_BUFFER_RESULTS, ArgumentType.BoolArgument);
		addArgument(ARG_WORKSPACE);
		addArgument(ARG_NOSETTNGS, ArgumentType.BoolArgument);
		addArgument(ARG_NOTEMPLATES, ArgumentType.BoolArgument);
		addArgument(ARG_HIDE_WARNINGS, ArgumentType.BoolArgument);
		addArgument(ARG_ALT_DELIMITER);
		addArgument(ARG_DELIMITER);
		addArgument(ARG_READ_ONLY, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_TRIM_CHAR, ArgumentType.BoolArgument);
		addArgument(ARG_LANG);
		addArgument(ARG_CONSOLIDATE_LOG, ArgumentType.BoolArgument);
		addArgument(ARG_INTERACTIVE, ArgumentType.BoolArgument);
		addArgument("help");
		addArgument("version");
		addArgument(ARG_PROP, ArgumentType.Repeatable);
		addArgument(ARG_LOG_ALL_STMT, ArgumentType.BoolSwitch);
		addArgument(ARG_CONN_DESCRIPTOR);
	}

	@Override
	public void parse(String[] args)
	{
		super.parse(args);
		String propfile = getValue(ARG_PROPFILE);
		if (propfile != null)
		{
			try
			{
				File f = new File(propfile);
				parseProperties(f);
			}
			catch (Exception e)
			{
				System.err.println("Could not read properties file: " + propfile);
				e.printStackTrace();
			}
		}
		String lb = getValue(ARG_LB_CONN);
		if (lb != null)
		{
			try
			{
				File f = new File(lb);
				BufferedReader in = EncodingUtil.createBufferedReader(f, null);
				List<String> lines = FileUtil.getLines(in, true);
				List<String> translated = new ArrayList<>(lines.size());
				for (String line : lines)
				{
					if (StringUtil.isBlank(line)) continue;

					String wbLine = line.trim();
					if (wbLine.startsWith("#")) continue;

					if (wbLine.startsWith("classpath:"))
					{
						String filename = wbLine.substring("classpath:".length()).trim();
						File lib = new File(filename);
						if (lib.getParent() == null)
						{
							// If no directory is given, Liquibase assumes the current directory
							// WbDriver on the other hand will search the jar file in the config directory, if no directory
							// is specified, which is most probably not correct.
							filename = "./" + lib.getName();
						}
						wbLine = ARG_CONN_JAR + "=" + filename;
					}
					else
					{
						wbLine = wbLine.replace("driver:", ARG_CONN_DRIVER + "=");
						wbLine = wbLine.replace("url:", ARG_CONN_URL + "=");
						wbLine = wbLine.replace("username:", ARG_CONN_USER + "=");
						wbLine = wbLine.replace("password:", ARG_CONN_PWD + "=");
					}
					translated.add(wbLine);
				}
				parse(translated);
			}
			catch (Exception e)
			{
				System.err.println("Could not read liquibase properties!");
				e.printStackTrace();
			}
		}
	}

	public void setCommandString(String cmd)
	{
		arguments.put(ARG_COMMAND, cmd);
	}

	public String getHelp()
	{
		StringBuilder msg = new StringBuilder(100);
		List<String> args = getRegisteredArguments();
		msg.append("Available parameters:\n");
		for (String arg : args)
		{
			ArgumentType type = getArgumentType(arg);
			msg.append('-');
			msg.append(arg);
			if (type == ArgumentType.BoolArgument)
			{
				msg.append(" (true/false)");
			}
			msg.append("\n");
		}
		return msg.toString();
	}

}
