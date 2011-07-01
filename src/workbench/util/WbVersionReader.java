/*
 * WbVersionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.swing.Timer;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbVersionReader
	implements ActionListener
{
	private VersionNumber currentDevBuildNumber;
	private String currentDevBuildDate;
	private VersionNumber currentStableBuildNumber;
	private String currentStableBuildDate;
	private final String userAgent;
	private boolean success = false;
	private Timer timeout;
	private ActionListener client;
	private WbThread readThread;

	/**
	 * Constructor only for unit testing
	 */
	WbVersionReader(VersionNumber dev, VersionNumber stable)
	{
		this.currentDevBuildNumber = dev;
		this.currentStableBuildNumber = stable;
		this.userAgent = "VersionTest";
	}

	public WbVersionReader(ActionListener a)
	{
		this("manual", a);
	}

	public WbVersionReader(String type, ActionListener a)
	{
		this.userAgent = "WbUpdateCheck, " +
			ResourceMgr.getBuildNumber().toString() + ", " + type + ", " +
			Settings.getInstance().getLanguage().getLanguage() + ", " +
			System.getProperty("os.name");
		this.client = a;
	}

	public void startCheckThread()
	{
		this.timeout = new Timer(60 * 1000, this);
		this.timeout.start();

		this.readThread = new WbThread("VersionReaderThread")
		{
			@Override
			public void run()
			{
				readBuildInfo();
			}
		};
		readThread.start();
	}

	public boolean success()
	{
		return success;
	}

	private void readBuildInfo()
	{
		long start = System.currentTimeMillis();
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
			success = true;

			long end = System.currentTimeMillis();
			LogMgr.logDebug("WbVersionReader.readBuildInfo()", "Retrieving version information took " + (end - start) + "ms");
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.readBuildInfo()","Could not read version information", e);
			success = false;
		}
		finally
		{
			FileUtil.closeQuietely(in);
			if (timeout != null)
			{
				timeout.stop();
				timeout = null;
			}
			if (client != null)
			{
				ActionEvent e = new ActionEvent(WbVersionReader.this, 1, success ? "versionChecked" : "error");
				client.actionPerformed(e);
			}
		}
	}

	public UpdateVersion getAvailableUpdate()
	{
		return getAvailableUpdate(ResourceMgr.getBuildNumber());
	}

	public UpdateVersion getAvailableUpdate(VersionNumber current)
	{
		if (UpdateCheck.DEBUG) return UpdateVersion.stable;
		if (currentDevBuildNumber != null && currentDevBuildNumber.isNewerThan(current)) return UpdateVersion.devBuild;
		if (currentStableBuildNumber != null && currentStableBuildNumber.isNewerThan(current)) return UpdateVersion.stable;
		return UpdateVersion.none;
	}

	public VersionNumber getDevBuildNumber()
	{
		return this.currentDevBuildNumber;
	}

	public String getDevBuildDate()
	{
		return this.currentDevBuildDate;
	}

	public VersionNumber getStableBuildNumber()
	{
		return this.currentStableBuildNumber;
	}

	public String getStableBuildDate()
	{
		return this.currentStableBuildDate;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.timeout)
		{
			if (this.readThread != null)
			{
				this.readThread.interrupt();
			}
			this.success = false;
		}
	}
}
