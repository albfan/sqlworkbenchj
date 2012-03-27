/*
 * LogLevel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public enum LogLevel
{
	error,
	warning,
	info,
	debug,
	trace;

	public boolean isIncluded(LogLevel target)
	{
		return false;
	}

	public static LogLevel getLevel(String type)
	{
		if (StringUtil.isBlank(type)) return error;
		if (type.equalsIgnoreCase("warn")) return warning;
		if (type.equalsIgnoreCase("warning")) return warning;
		if (type.equalsIgnoreCase("error")) return error;
		if (type.equalsIgnoreCase("info")) return info;
		if (type.equalsIgnoreCase("debug")) return debug;
		if (type.equalsIgnoreCase("trace")) return trace;
		return error;
 	}

	@Override
	public String toString()
	{
		if (this == error) return "ERROR";
		if (this == warning) return "WARN";
		if (this == info) return "INFO";
		if (this == debug) return "DEBUG";
		if (this == trace) return "TRACE";
		return super.toString();
	}

}
