/*
 * WbVersionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbVersionReader
{

	private String currentDevBuildNumber;
	private String currentDevBuildDate;

	private String currentStableBuildNumber;
	private String currentStableBuildDate;
	private final String userAgent;

	public WbVersionReader()
		throws Exception
	{

		this.userAgent = "SQL Workbench/J Update Check (" + ResourceMgr.getString("TxtBuildNumber") + ")";
		long start, end;
		start = System.currentTimeMillis();
		try
		{
			this.readBuildInfo();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.<init>", "Error when retrieving build version", e);
		}

		end = System.currentTimeMillis();
		LogMgr.logDebug("WbVersionReader.<init>", "Retrieving version information took " + (end - start) + "ms");
	}

	private void readBuildInfo()
		throws Exception
	{
		InputStream in = null;
		try
		{
			URL url = new URL("http://www.sql-workbench.net/release.property");
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", this.userAgent);
			conn.setRequestProperty("Referer", System.getProperty("java.version"));
				
			in = conn.getInputStream();
			
			Properties props = new Properties();
			props.load(in);
			
			this.currentDevBuildNumber = props.getProperty("dev.build.number", null);
			this.currentDevBuildDate = props.getProperty("dev.build.date", null);
			this.currentStableBuildNumber = props.getProperty("release.build.number", null);
			this.currentStableBuildDate = props.getProperty("release.build.date", null);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.readBuildInfo()", "Could not read version information from website: " + ExceptionUtil.getDisplay(e));
			throw e;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}

	public String getDevBuildNumber() { return this.currentDevBuildNumber; }
	public String getDevBuildDate() { return this.currentDevBuildDate; }

	public String getStableBuildNumber() { return this.currentStableBuildNumber; }
	public String getStableBuildDate() { return this.currentStableBuildDate; }
}
