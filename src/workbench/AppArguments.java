/*
 * AppArguments.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.util.List;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * A class to define and parse the arguments that are available when 
 * the application is started. 
 * 
 * @author support@sql-workbench.net
 */
public class AppArguments
	extends ArgumentParser
{
	// Parameters for batch execution used by BatchRunner
	public static final String ARG_SCRIPT = "script";
	public static final String ARG_COMMAND = "command";
	public static final String ARG_SCRIPT_ENCODING = "encoding";
	public static final String ARG_ABORT = "abortOnError";
	
	// Connection related parameters
	public static final String ARG_PROFILE = "profile";
	public static final String ARG_PROFILE_GROUP = "profilegroup";
	public static final String ARG_CONN_URL = "url";
	public static final String ARG_CONN_DRIVER = "driver";
	public static final String ARG_CONN_DRIVER_CLASS = "driverclass";
	public static final String ARG_CONN_JAR = "driverjar";
	public static final String ARG_CONN_USER = "username";
	public static final String ARG_CONN_PWD = "password";
	public static final String ARG_CONN_AUTOCOMMIT = "autocommit";
	public static final String ARG_CONN_SEPARATE = "separateConnection";
	public static final String ARG_CONN_EMPTYNULL = "emptyStringIsNull";
	public static final String ARG_CONN_ROLLBACK = "rollbackOnDisconnect";
	public static final String ARG_CONN_TRIM_CHAR = "trimCharData";
	public static final String ARG_IGNORE_DROP = "ignoreDropErrors";
	public static final String ARG_READ_ONLY = "readOnly";
	public static final String ARG_CONN_REMOVE_COMMENTS = "removeComments";
	public static final String ARG_HIDE_WARNINGS = "hideWarnings";
	public static final String ARG_INTERACTIVE = "interactive";
	
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
	public static final String ARG_SHOWPROGRESS = "showProgress";
	public static final String ARG_PROFILE_STORAGE = "profileStorage";
	public static final String ARG_CONFIGDIR = "configDir";
	public static final String ARG_LIBDIR = "libdir";
	public static final String ARG_LOGFILE = "logfile";
	public static final String ARG_VARDEF = "varDef";
	public static final String ARG_LANG = "language";
	public static final String ARG_NOSETTNGS = "noSettings";
	public static final String ARG_NOTEMPLATES = "noTemplates";
	public static final String ARG_CONSOLE_OPT_COLS = "optimizeColWidth";
	public static final String ARG_CONSOLE_BUFFER_RESULTS = "bufferResults";

	public AppArguments()
	{
		super();
		addArgument(ARG_PROFILE, ArgumentType.ProfileArgument);
		addArgument(ARG_FEEDBACK);
		addArgument(ARG_PROFILE_GROUP);
		addArgument(ARG_PROFILE_STORAGE);
		addArgument(ARG_CONFIGDIR);
		addArgument(ARG_LIBDIR);
		addArgument(ARG_SCRIPT);
		addArgument(ARG_COMMAND);
		addArgument(ARG_SCRIPT_ENCODING);
		addArgument(ARG_LOGFILE);
		addArgument(ARG_ABORT, ArgumentType.BoolArgument);
		addArgument(ARG_SUCCESS_SCRIPT);
		addArgument(ARG_ERROR_SCRIPT);
		addArgument(ARG_VARDEF);
		addArgument(ARG_CONN_URL);
		addArgument(ARG_CONN_DRIVER);
		addArgument(ARG_CONN_DRIVER_CLASS);
		addArgument(ARG_CONN_JAR);
		addArgument(ARG_CONN_USER);
		addArgument(ARG_CONN_PWD);
		addArgument(ARG_CONN_SEPARATE, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_EMPTYNULL, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_AUTOCOMMIT, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_REMOVE_COMMENTS, ArgumentType.BoolArgument);
		addArgument(ARG_CONN_ROLLBACK, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_PUMPER, ArgumentType.BoolArgument);
		addArgument(ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		addArgument(ARG_DISPLAY_RESULT, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_DBEXP, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_SEARCHER, ArgumentType.BoolArgument);
		addArgument(ARG_SHOW_TIMING, ArgumentType.BoolArgument);
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
	}
	
	public String getHelp()
	{
		StringBuilder msg = new StringBuilder(100);
		List<String> args = getRegisteredArguments();
		msg.append("Available parameters:\n");
		for (String arg : args)
		{
			ArgumentType type = getArgumentType(arg);
			msg.append("-" + arg);
			if (type == ArgumentType.BoolArgument)
			{
				msg.append(" (true/false)");
			}
			msg.append("\n");
		}
		return msg.toString();
	}
}
