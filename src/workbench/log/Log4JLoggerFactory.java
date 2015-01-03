/*
 * Log4JLoggerFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.log;

import org.apache.log4j.spi.LoggerFactory;

/**
 * This is a specialized LoggerFactory that creates the Workbench specific
 * Log4Jlogger.
 *
 * @author Peter Franken
 */
public class Log4JLoggerFactory
	implements LoggerFactory
{
	private static Class loggerFqcn = Log4JLogger.class;

	public Log4JLoggerFactory()
	{
	}

	@Override
	public Log4JLogger makeNewLoggerInstance(String name)
	{
		return new Log4JLogger(name);
	}

	public static void setLoggerFqcn(Class loggerFqcn)
	{
		Log4JLoggerFactory.loggerFqcn = loggerFqcn;
	}

	public static Class getLoggerFqcn()
	{
		return loggerFqcn;
	}
}
