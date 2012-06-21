/*
 * PlatformHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	public static boolean isWindowsXP()
	{
		if (isWindows())
		{
			VersionNumber current = new VersionNumber(System.getProperty("os.version"));
			VersionNumber vista = new VersionNumber(5,1);
			return current.isNewerOrEqual(vista);
		}
		return false;
	}

	public static boolean isMacOS()
	{
		return MacOSHelper.isMacOS();
	}
}
