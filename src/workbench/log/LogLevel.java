/*
 * LogLevel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
