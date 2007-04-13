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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import workbench.util.ExceptionUtil;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.UpdateVersion;
import workbench.util.VersionNumber;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbVersionReader
{
	private VersionNumber currentDevBuildNumber;
	private String currentDevBuildDate;

	private VersionNumber currentStableBuildNumber;
	private String currentStableBuildDate;
	private final String userAgent;
	private boolean success = false;
	
	public WbVersionReader()
	{
		this("");
	}
	
	public WbVersionReader(String type)
	{

		this.userAgent = "SQL Workbench/J " +  type + "update check (" + ResourceMgr.getString("TxtBuildNumber") + ")";
		long start, end;
		start = System.currentTimeMillis();
		try
		{
			this.readBuildInfo();
			success = true;
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.<init>", "Error when retrieving build version", e);
			this.success = false;
		}

		end = System.currentTimeMillis();
		LogMgr.logDebug("WbVersionReader.<init>", "Retrieving version information took " + (end - start) + "ms");
	}

	public boolean success()
	{
		return success;
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
			
			this.currentDevBuildNumber = new VersionNumber(props.getProperty("dev.build.number", null));
			this.currentDevBuildDate = props.getProperty("dev.build.date", null);
			this.currentStableBuildNumber = new VersionNumber(props.getProperty("release.build.number", null));
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

	public UpdateVersion getAvailableUpdate()
	{
		VersionNumber current = ResourceMgr.getBuildNumber();
		//VersionNumber current = new VersionNumber("93");
		if (currentDevBuildNumber != null && currentDevBuildNumber.isNewerThan(current)) return UpdateVersion.devBuild;
		if (currentStableBuildNumber!= null && currentStableBuildNumber.isNewerThan(current)) return UpdateVersion.stable;
		return UpdateVersion.none;
	}
	
	public VersionNumber getDevBuildNumber() { return this.currentDevBuildNumber; }
	public String getDevBuildDate() { return this.currentDevBuildDate; }

	public VersionNumber getStableBuildNumber() { return this.currentStableBuildNumber; }
	public String getStableBuildDate() { return this.currentStableBuildDate; }
	
}
