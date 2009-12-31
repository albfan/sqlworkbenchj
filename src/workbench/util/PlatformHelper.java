/*
 * PlatformHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 *
 * @author Thomas Kellerer
 */
public class PlatformHelper
{

	public static boolean isWindows()
	{
		return System.getProperty("os.name").indexOf("Windows") > -1;
	}

	public static boolean isMacOS()
	{
		return MacOSHelper.isMacOS();
	}
}
