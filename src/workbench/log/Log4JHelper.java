/*
 * Log4JHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import java.lang.reflect.Method;

/**
 * Test if Log4J classes are available on the classpath.
 * <br>
 * If Log4J is available, it will call
 * org.apache.log4j.Log4JLoggerFactory.setLoggerFqcn() passing LogMgr.class
 *
 * All this is done using Reflection, so Log4J does not need to be available.
 *
 * 
 * @author Peter Franken
 * @author Thomas Kellerer
 */
public class Log4JHelper
{
	private static boolean tested = false;
	private static boolean available = false;

	public static boolean isLog4JAvailable()
	{
		if (tested) return available;
		try
		{
			tested = true;
			Class.forName("org.apache.log4j.Logger");
			Class factory = Class.forName("workbench.log.Log4JLoggerFactory");
			Method setLoggerFqcn = factory.getDeclaredMethod("setLoggerFqcn", new Class[] { Class.class });
			setLoggerFqcn.invoke(null, new Object[] { LogMgr.class } );
			available = true;
		}
		catch (Throwable th)
		{
			th.printStackTrace(System.err);
			available = false;
		}
		return available;
	}

}
