/*
 * PoiHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import workbench.log.LogMgr;
import workbench.util.VersionNumber;

/**
 * Test if POI classes are available on the classpath.
 * 
 * @author Thomas Kellerer  
 */
public class PoiHelper
{
	private static boolean tested;
	private static boolean available;

	private static boolean xlsxTested;
	private static boolean xlsxAvailable;

	public static boolean isPoiAvailable()
	{
		if (tested) return available;

		try
		{
			tested = true;
			Class c = Class.forName("org.apache.poi.ss.usermodel.Workbook");
			c.getPackage();

			Package poi = c.getPackage();
			String v = poi.getImplementationVersion();
			int pos = v.indexOf('-');
			if (pos > -1) v = v.substring(0, pos);
			VersionNumber version = new VersionNumber(v);
			VersionNumber needed = new VersionNumber(3, 5);
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

	public static boolean isXLSXAvailable()
	{
		if (!isPoiAvailable()) return false;

		if (xlsxTested) return xlsxAvailable;

		try
		{
			xlsxTested = true;
			Class c = Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
			xlsxAvailable = (c != null);
		}
		catch (Throwable th)
		{
			xlsxAvailable = false;
		}
		return xlsxAvailable;
	}

	public static void main(String[] args)
	{
		try
		{
			XSSFWorkbook wb = new XSSFWorkbook();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
