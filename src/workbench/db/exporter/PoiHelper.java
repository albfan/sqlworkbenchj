/*
 * PoiHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.log.LogMgr;
import workbench.util.VersionNumber;

/**
 * Test if POI classes are available on the classpath.
 * 
 * @author support@sql-workbench.net  
 */
public class PoiHelper
{
	private static boolean tested = false;
	private static boolean available = false;

	public static boolean isPoiAvailable()
	{
		if (tested) return available;

		try
		{
			tested = true;
			Class c = Class.forName("org.apache.poi.hssf.usermodel.HSSFWorkbook");
			c.getPackage();

			Package poi = c.getPackage();
			String v = poi.getImplementationVersion();
			int pos = v.indexOf('-');
			if (pos > -1) v = v.substring(0, pos);
			VersionNumber version = new VersionNumber(v);
			VersionNumber needed = new VersionNumber(2, 5);
			available = version.isNewerOrEqual(needed);
			if (!available)
			{
				LogMgr.logError("PoiHelper.isPoiAvailable()", "POI on classpath has wrong version: " + poi.getImplementationVersion() + " but " + needed.toString() + " or later is required", null);
			}
		}
		catch (Throwable th)
		{
			available = false;
		}
		return available;
	}
	
}
