/*
 * AppArguments.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import workbench.util.ArgumentParser;

/**
 * @author support@sql-workbench.net
 */
public class AppArguments
	extends ArgumentParser
{
	// Parameters for batch execution used by BatchRunner
	public static final String ARG_SCRIPT = "script";
	public static final String ARG_SCRIPT_ENCODING = "encoding";
	public static final String ARG_ABORT = "abortonerror";
	
	// Connection related parameters
	public static final String ARG_PROFILE = "profile";
	public static final String ARG_PROFILE_GROUP = "profilegroup";
	public static final String ARG_CONN_URL = "url";
	public static final String ARG_CONN_DRIVER = "driver";
	public static final String ARG_CONN_JAR = "driverjar";
	public static final String ARG_CONN_USER = "username";
	public static final String ARG_CONN_PWD = "password";
	public static final String ARG_CONN_AUTOCOMMIT = "autocommit";
	public static final String ARG_CONN_ROLLBACK = "rollbackOnDisconnect";
	public static final String ARG_CONN_TRIM_CHAR = "trimCharData";
	public static final String ARG_IGNORE_DROP = "ignoreDropErrors";
	
	public static final String ARG_DISPLAY_RESULT = "displayResult";
	public static final String ARG_SUCCESS_SCRIPT = "cleanupSuccess";
	public static final String ARG_ERROR_SCRIPT = "cleanupError";
	public static final String ARG_SHOW_TIMING = "showTiming";
	public static final String ARG_FEEDBACK = "feedback";
	public static final String ARG_WORKSPACE = "workspace";
	public static final String ARG_ALT_DELIMITER = "altDelimiter";
	public static final String ARG_DELIMITER = "delimiter";

	// Other parameters
	public static final String ARG_SHOWPROGRESS = "showProgress";
	public static final String ARG_QUIET = "quiet";
	public static final String ARG_PROFILE_STORAGE = "profileStorage";
	public static final String ARG_CONFIGDIR = "configdir";
	public static final String ARG_LIBDIR = "libdir";
	public static final String ARG_LOGFILE = "logfile";
	public static final String ARG_VARDEF = "vardef";
	public static final String ARG_SHOW_PUMPER = "datapumper";
	public static final String ARG_SHOW_DBEXP = "dbexplorer";
	public static final String ARG_LANG = "languaqe";
	public static final String ARG_NOSETTNGS = "nosettings";
	public static final String ARG_NOTEMPLATES = "notemplates";

	public AppArguments()
	{
		addArgument(ARG_PROFILE);
		addArgument(ARG_FEEDBACK);
		addArgument(ARG_PROFILE_GROUP);
		addArgument(ARG_PROFILE_STORAGE);
		addArgument(ARG_CONFIGDIR);
		addArgument(ARG_LIBDIR);
		addArgument(ARG_SCRIPT);
		addArgument(ARG_SCRIPT_ENCODING);
		addArgument(ARG_LOGFILE);
		addArgument(ARG_ABORT);
		addArgument(ARG_SUCCESS_SCRIPT);
		addArgument(ARG_ERROR_SCRIPT);
		addArgument(ARG_VARDEF);
		addArgument(ARG_CONN_URL);
		addArgument(ARG_CONN_DRIVER);
		addArgument(ARG_CONN_JAR);
		addArgument(ARG_CONN_USER);
		addArgument(ARG_CONN_PWD);
		addArgument(ARG_CONN_AUTOCOMMIT);
		addArgument(ARG_CONN_ROLLBACK);
		addArgument(ARG_SHOW_PUMPER);
		addArgument(ARG_IGNORE_DROP);
		addArgument(ARG_DISPLAY_RESULT);
		addArgument(ARG_SHOW_DBEXP);
		addArgument(ARG_SHOW_TIMING);
		addArgument(ARG_SHOWPROGRESS);
		addArgument(ARG_WORKSPACE);
		addArgument(ARG_NOSETTNGS);
		addArgument(ARG_NOTEMPLATES);
		addArgument(ARG_ALT_DELIMITER);
		addArgument(ARG_DELIMITER);
		addArgument(ARG_QUIET);
		addArgument(ARG_CONN_TRIM_CHAR);
		addArgument(ARG_LANG);
	}
}
