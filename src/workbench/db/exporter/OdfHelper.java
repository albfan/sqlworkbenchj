/*
 * PoiHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * Test if ODF Dom and Simple ODF classes are available on the classpath.
 *
 * @author Thomas Kellerer
 */
public class OdfHelper
{
	private static boolean tested;
	private static boolean available;

	public static boolean isSimpleODFAvailable()
	{
		if (tested)
		{
			return available;
		}

		try
		{
			tested = true;
			Class.forName("org.odftoolkit.simple.SpreadsheetDocument");
			available = true;
		}
		catch (Throwable th)
		{
			available = false;
		}
		return available;
	}

}
