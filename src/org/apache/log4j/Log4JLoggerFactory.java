/*
 * Log4JLoggerFactory.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */

package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;
import workbench.log.Log4JLogger;

/**
 * This is a specialized LoggerFactory that creates the Workbench specific
 * Log4Jlogger.
 *
 * Due to some strange bug in Log4J, this class needs to reside in the
 * package org.apache.log4j even though it is actually a SQL Workbench class
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

	public Log4JLogger makeNewLoggerInstance(String name)
	{
		return new Log4JLogger(name);
	}

	/**
	 * @param loggerFqcn the loggerFqcn to set
	 */
	public static void setLoggerFqcn(Class loggerFqcn)
	{
		Log4JLoggerFactory.loggerFqcn = loggerFqcn;
	}

	/**
	 * @return the loggerFqcn
	 */
	public static Class getLoggerFqcn()
	{
		return loggerFqcn;
	}
}
