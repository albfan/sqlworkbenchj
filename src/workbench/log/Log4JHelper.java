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

/**
 * Test if Log4J classes are available on the classpath.
 * 
 * @author Peter Franken
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
			Class c = Class.forName("org.apache.log4j.Logger");
			available = (c != null);
		}
		catch (Throwable th)
		{
			available = false;
		}
		return available;
	}

}
